package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import io.swagger.models.auth.In;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        //先把每天的日期加入列表
        List<LocalDate> dates = new ArrayList<>();
        dates.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dates.add(begin);
        }
        //把每天的营业额加入列表
        List<Double> turnovers = new ArrayList<>();
        for (LocalDate date : dates) {
            //获取营业额
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.getTurnoverByDate(map);
            turnovers.add(turnover);
        }
        String dateList = dates.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(","));

        String turnoverList = turnovers.stream()
                .map(turnover -> String.format("%.2f", turnover))
                .collect(Collectors.joining(","));

        return TurnoverReportVO.builder()
                .dateList(dateList)
                .turnoverList(turnoverList)
                .build();


    }

    /**
     * 用户统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        dates.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dates.add(begin);
        }
        List<Long> totalUsers = new ArrayList<>();
        List<Long> newUsers = new ArrayList<>();

        for (LocalDate date : dates) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //统计每天新增用户数
            Long newUser = userMapper.countByDate(beginTime, endTime);
            Long sum = userMapper.countByDate(null,endTime);
            newUsers.add(newUser);
            totalUsers.add(sum);
        }

        String dateList = dates.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String totalUserList = totalUsers.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String newUserList = newUsers.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return new UserReportVO(dateList, totalUserList, newUserList);
    }

    /**
     * 订单统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dates = new ArrayList();
        dates.add(begin);
        List<Integer> orders = new ArrayList();  //订单数列表
        List<Integer> validOrders = new ArrayList();  //有效订单列表
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dates.add(begin);
        }
        Integer totalOderCount = 0;
        Integer validOrderCount = 0;
        for (LocalDate date : dates) {
            //获得今天的订单数和有效订单数
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer order = (Integer) getOderCount(beginTime, endTime ,null);
            Integer validOrder = (Integer)getOderCount(beginTime, endTime, 5);
            //订单总数
            totalOderCount += order;
            validOrderCount += validOrder;
            orders.add(order);
            validOrders.add(validOrder);
        }
        //订单完成率
        Double orderCompletionRate = null;
        if (totalOderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOderCount;
        }
        String dateList = dates.stream().map(String::valueOf).collect(Collectors.joining(","));
        String orderCountList = orders.stream().map(String::valueOf).collect(Collectors.joining(","));
        String validOrderCountList = validOrders.stream().map(String::valueOf).collect(Collectors.joining(","));
        OrderReportVO orderReportVO = OrderReportVO.builder()
                .dateList(dateList)
                .orderCountList(orderCountList)
                .validOrderCountList(validOrderCountList)
                .totalOrderCount(totalOderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
        return orderReportVO;

    }

    private Integer getOderCount(LocalDateTime begin, LocalDateTime end , Integer status) {
        Map map = new HashMap();
        map.put("beginTime", begin);
        map.put("endTime", end);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }

    /**
     * 销量统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<LocalDate> dates = new ArrayList<>();
        dates.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dates.add(begin);
        }
        //查询指定时间区间内的销量排名top10的名称和数量
        List<GoodsSalesDTO> list = orderMapper.getSalesTop10(beginTime,endTime);
        String namelist =list.stream().map(GoodsSalesDTO::getName).collect(Collectors.joining(","));
        String numberlist =list.stream().map(GoodsSalesDTO::getNumber).map(Object::toString).collect(Collectors.joining(","));
        return new SalesTop10ReportVO(namelist,numberlist);
    }
    /**导出近30天的运营数据报表
     * @param response
     **/
    public void exportBusinessData(HttpServletResponse response) {
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        //查询概览运营数据，提供给Excel模板文件
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin,LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            //基于提供好的模板文件创建一个新的Excel表格对象
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);
            //获得Excel文件中的一个Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            sheet.getRow(1).getCell(1).setCellValue(begin + "至" + end);
            //获得第4行
            XSSFRow row = sheet.getRow(3);
            //获取单元格
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                //准备明细数据
                businessData = workspaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }
            //通过输出流将文件下载到客户端浏览器中
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            //关闭资源
            out.flush();
            out.close();
            excel.close();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

}
