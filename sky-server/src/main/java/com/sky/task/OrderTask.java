package com.sky.task;

import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@Slf4j

public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    /**
     * 定时检查订单状态，如果订单状态处于待支付状态，则取消该订单
     */
   /* @Scheduled(cron = " 0 * * * * ? ")*/
    public void processTimeoutOrders(){
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> ordersList = orderMapper.getStatusAndOrderTime(Orders.PENDING_PAYMENT,time);

    //对每个超时未支付订单，需要修改订单状态为6已取消，修改订单取消原因为超时未支付，取消时间为当前时间
        if(ordersList != null && ordersList.size() > 0) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，未支付");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }
  /*  @Scheduled(cron = " 0 0 1 * * ? ")*/
    public void processDeliveryOrders() {
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> ordersList = orderMapper.getStatusAndOrderTime(Orders.PENDING_PAYMENT, time);

        //凌晨一点对前一天的状态为派送中的订单修改为5已完成，订单完成时间设置为当前时间，
        if (ordersList != null && ordersList.size() > 0) {
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orders.setDeliveryTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }
}
