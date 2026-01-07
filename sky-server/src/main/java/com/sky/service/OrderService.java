package com.sky.service;


import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    /**
     * 用户下单
     * @param ordersSubmitDTO
     */
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 用户历史订单查询
     * @param ordersPaymentDTO
     * @return
     */
    PageResult pageQuery(OrdersPageQueryDTO ordersPaymentDTO);
    /**
     * 查询订单详情
     * @param id
     * @return
     */
    OrderVO getOrderDetail(Long id);
    /**
     * 用户取消订单
     * @param id
     */
    void cancel(Long id);
    /**
     * 支付订单
     * @param id
     * @return
     */
    void repitition(Long id);

    /**
     * 条件搜索订单
     * @param ordersPageQuerytDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQuerytDTO);
    /**
     * 订单统计
     * @return
     */
    OrderStatisticsVO statistics();
    /**
     * 接单
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);
    /**
     * 拒单
     * @param ordersRejectionDTO
     */

    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;
    /**
     * 商家取消订单
     *
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;
    /**
     * 派送订单
     * @param id
     * @return
     */
    void delivery(long id);
    /**
     * 完成订单
     * @param id
     * @return
     */
    void complete(long id);
    /**
     * 催单
     * @param id
     * @return
     */
    void reminder(long id);
}
