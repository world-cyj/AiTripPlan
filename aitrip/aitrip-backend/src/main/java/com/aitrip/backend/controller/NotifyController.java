package com.aitrip.backend.controller;

import com.aitrip.backend.service.impl.TicketNotifyService;
import com.aitrip.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订阅通知接口
 * POST   /api/notify/subscribe  — 订阅到票提醒
 * DELETE /api/notify/subscribe  — 取消订阅
 * GET    /api/notify/position   — 查询排队位置
 */
@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotifyController {

    private final TicketNotifyService notifyService;

    @PostMapping("/subscribe")
    public Result<String> subscribe(
            @RequestParam Long voucherId,
            @RequestParam Long userId) {
        return Result.ok(notifyService.subscribe(voucherId, userId));
    }

    @DeleteMapping("/subscribe")
    public Result<String> unsubscribe(
            @RequestParam Long voucherId,
            @RequestParam Long userId) {
        return Result.ok(notifyService.unsubscribe(voucherId, userId));
    }

    @GetMapping("/position")
    public Result<String> position(
            @RequestParam Long voucherId,
            @RequestParam Long userId) {
        return Result.ok(notifyService.getQueuePosition(voucherId, userId));
    }
}
