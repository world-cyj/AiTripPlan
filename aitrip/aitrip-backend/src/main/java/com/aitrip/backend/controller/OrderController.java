package com.aitrip.backend.controller;

import com.aitrip.backend.service.OrderService;
import com.aitrip.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单接口
 * GET  /api/orders/{orderId}?userId=  — 订单详情
 * GET  /api/orders/user/{userId}      — 用户订单列表
 * POST /api/orders/{orderId}/cancel   — 取消订单
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public Result<String> detail(@PathVariable Long orderId,
                                 @RequestParam Long userId) {
        return Result.ok(orderService.getOrderDetail(orderId, userId));
    }

    @GetMapping("/user/{userId}")
    public Result<String> listByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1")  Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(orderService.listUserOrders(userId, page, size));
    }

    @PostMapping("/{orderId}/cancel")
    public Result<String> cancel(@PathVariable Long orderId,
                                 @RequestBody Map<String, Long> req) {
        return Result.ok(orderService.cancelOrder(orderId, req.get("userId")));
    }
}
