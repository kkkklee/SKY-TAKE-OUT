package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    //插入订单表
    void insert(Orders orders);
    //历史订单分页查询
    //TODO 这里和答案写的不一样，答案写的是xml的条件查询
    @Select("select * from orders where user_id = #{userId} order by order_time desc")
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPaymentDTO);

    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);


    void update(Orders orders);
    //订单条件搜索
    Page<Orders> conditionSearch(OrdersPageQueryDTO ordersPageQuerytDTO);

    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer confirmed);

    @Select("select * from orders where status = #{status} and order_time < #{time} and status = 5")
    List<Orders> getStatusAndOrderTime(Integer status, LocalDateTime time);

    @Select("select sum(amount) from orders where order_time >= #{beginTime} and order_time <= #{endTime}")
    Double getTurnoverByDate(Map map);

    Integer countByMap(Map map);

    List<GoodsSalesDTO> getSalesTop10(LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 根据动态条件统计营业额
     * @param map
     */
    Double sumByMap(Map map);
}
