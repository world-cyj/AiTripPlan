package com.aitrip.backend.service;

public interface OrderService {
    /** 查询订单详情 */
    String getOrderDetail(Long orderId, Long userId);
    /** 查询用户订单列表 */
    String listUserOrders(Long userId, Integer page, Integer size);
    /** 取消订单（库存回补 + 触发订阅通知） */
    String cancelOrder(Long orderId, Long userId);
}
