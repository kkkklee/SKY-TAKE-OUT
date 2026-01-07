package com.sky.service.impl;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.*;
import com.sky.entity.User;


import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.PageResult;
import com.sky.service.AddressBookService;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private AddressBookService addressBookService;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper ordersDetailMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //1.异常订单检测，购物车空，地址簿空
        AddressBook addressBook = addressBookService.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //2.查询用户购物车
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                                    .userId(userId)
                                    .build();
        List<ShoppingCart> shoppingCartlist = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartlist == null || shoppingCartlist.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //检查用户收货地址是否超出范围
        checkOutOfRange(addressBook.getCityName() +addressBook.getDistrictName()+addressBook.getDetail());
        //3.插入订单表
            Orders orders = new Orders();
            BeanUtils.copyProperties(ordersSubmitDTO,orders);
            orders.setNumber(String.valueOf(System.currentTimeMillis()));
            orders.setStatus(Orders.PENDING_PAYMENT);
            orders.setUserId(userId);
            orders.setOrderTime(LocalDateTime.now());
            orders.setPayStatus(Orders.UN_PAID);
            orders.setPhone(addressBook.getPhone());
            orders.setAddress(addressBook.getDetail());//这里没有省市？
            orders.setConsignee(addressBook.getConsignee());
            orderMapper.insert(orders);
        //4.插入订单明细表
        List<OrderDetail> orderdetails = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartlist) {
            OrderDetail orderdetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderdetail);
            orderdetail.setOrderId(orders.getId());//这里用了orderid，所以要拿回主键
            orderdetails.add(orderdetail);
        }
        ordersDetailMapper.insertBatch(orderdetails);
        //5.清空购物车
        shoppingCartMapper.clean(userId);
        //6.返回订单VO
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }
    /**
     * 历史订单分页查询
     * @param ordersPageQuerytDTO
     * @return
     */
    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQuerytDTO) {
        ordersPageQuerytDTO.setUserId(BaseContext.getCurrentId());
        PageHelper.startPage(ordersPageQuerytDTO.getPage(), ordersPageQuerytDTO.getPageSize());
        Page<Orders> list = orderMapper.pageQuery(ordersPageQuerytDTO);
        List<OrderVO> orderVOList = new ArrayList<>();

        if (list!= null && list.size()>0) {
            for (Orders orders : list) {
                OrderVO orderVO = new OrderVO();
                Long orderID = orders.getId();
                List<OrderDetail> orderDetaillist = orderDetailMapper.listByOrderId(orderID);
                orderVO.setOrderDetailList(orderDetaillist);
                BeanUtils.copyProperties(orders,orderVO);
                orderVOList.add(orderVO);
            }
        }
        return new PageResult(list.getTotal(),orderVOList);
    }
    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO getOrderDetail(Long id) {
        Orders order  = orderMapper.getById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        List<OrderDetail> orderDetaillist = orderDetailMapper.listByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order,orderVO);
        orderVO.setOrderDetailList(orderDetaillist);
        return orderVO;
    }
    /**
     * 用户取消订单
     * @param id
     */
    @Override
    public void cancel(Long id) {
        Orders orders = orderMapper.getById(id);
        Integer status = orders.getStatus();
        if (status == Orders.COMPLETED || status == Orders.CANCELLED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //待接单或待支付，则直接取消
        if(status == Orders.PENDING_PAYMENT){
            orders.setStatus(Orders.CANCELLED);
        }
        //待接单状态下取消吗，商家需要给用户退款
        else if (status == Orders.TO_BE_CONFIRMED) {
            orders.setStatus(Orders.CANCELLED);
            orders.setPayStatus(Orders.REFUND);
        }
        //已接单和派送中取消需要和商家电话联系
        else {
            System.out.println("请和商家联系取消订单");
        }
        orderMapper.update(orders);
    }
    /**
     * 支付订单
     * @param id 订单id
     * @return
     */
    @Override
    public void repitition(Long id) {
        //查询订单明细表
        List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(id);
        Long UserId = BaseContext.getCurrentId();
        //以前的订单明细转化到现在的购物车
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x ->{
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(x,shoppingCart,"id");
            shoppingCart.setUserId(UserId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());
        //插入到购物车表
        shoppingCartMapper.insertBatch(shoppingCartList);
    }
    /**
     * 管理端订单条件查询
     * @param ordersPageQuerytDTO
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQuerytDTO) {
        PageHelper.startPage(ordersPageQuerytDTO.getPage(), ordersPageQuerytDTO.getPageSize());
        Page<Orders> page = orderMapper.conditionSearch(ordersPageQuerytDTO);
        //将Orders转为OrderVO，后面订单详情需要
        List<OrderVO> orderVOList = getOrderVOList(page);
        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        List<OrderVO> orderVOList = new ArrayList<>();
        List<Orders> ordersList = page.getResult();
        if (ordersList!=null && ordersList.size()>0) {
            for(Orders order : ordersList){
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order,orderVO);
                //拿到该订单的订单细节表
                List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(order.getId());
                //拿到该订单的菜品名称
                String orderDishesStr =  getOrderDishesStr(orderDetailList);
                orderVO.setOrderDishes(orderDishesStr);
                orderVO.setOrderDetailList(orderDetailList);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    private String getOrderDishesStr(List<OrderDetail> orderDetailList) {
        String orderDishsStr = orderDetailList.stream().map(x -> {
            String dishName = x.getName()+"*"+x.getNumber()+";";
            return dishName;
        }).collect(Collectors.toList()).toString();
        return String.join("", orderDishsStr);
    }
    /**
     * 订单统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(orderMapper.countStatus(Orders.TO_BE_CONFIRMED));
        orderStatisticsVO.setConfirmed(orderMapper.countStatus(Orders.CONFIRMED));
        orderStatisticsVO.setDeliveryInProgress(orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS));
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders order = new Orders().builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(order);
    }
    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        //查询订单
        Orders order = orderMapper.getById(ordersRejectionDTO.getId());
        //只有在待接单下可以拒单
        if(order == null || order.getStatus() != Orders.TO_BE_CONFIRMED ){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //如果用户已支付，则自动退款
        if(order.getPayStatus() == Orders.PAID){
            String refund = weChatPayUtil.refund(
                    order.getNumber(),
                    order.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
        }
        //拒绝订单需要退款，更改订单状态，取消原因，拒单时间
        Orders order2 = new Orders();
        order2.setId(ordersRejectionDTO.getId());
        order2.setStatus(Orders.CANCELLED);
        order2.setCancelReason(ordersRejectionDTO.getRejectionReason());
        order2.setCancelTime(LocalDateTime.now());
        orderMapper.update(order2);
    }
    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     * @return
     */
    @Override
    public void delivery(long id) {
        log.info("派送订单:{}",id);
        Orders orderBD = orderMapper.getById(id);
        if (orderBD ==null || orderBD.getStatus() != Orders.CONFIRMED){
            throw new DeletionNotAllowedException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders order = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .deliveryTime(LocalDateTime.now())
                .build();

        orderMapper.update(order);
    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(long id) {
        Orders ordersDB = orderMapper.getById(id);
        if (ordersDB==null || ordersDB.getStatus() != Orders.DELIVERY_IN_PROGRESS){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    @Override
    public void reminder(long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //基于WebSocket实现催单
        Map map = new HashMap();
        map.put("type", 2);//2代表用户催单
        map.put("orderId", id);
        map.put("content", "订单号：" + orders.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }


}
