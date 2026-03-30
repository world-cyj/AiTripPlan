package com.aitrip.backend.controller;

import com.aitrip.backend.service.SeckillService;
import com.aitrip.common.exception.BusinessException;
import com.aitrip.common.response.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 秒杀接口
 * 供前端直接调用，或通过 MCP Tool 间接调用
 */
@Slf4j
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 执行秒杀
     * POST /api/seckill/execute
     */
    @PostMapping("/execute")
    public Result<Map<String, Object>> execute(@RequestBody Map<String, Long> req) {
        Long voucherId = req.get("voucherId");
        Long userId    = req.get("userId");
        Long orderId   = seckillService.seckillVoucher(voucherId, userId);
        return Result.ok(Map.of(
                "orderId",  orderId,
                "message", "抢票成功！订单正在处理中"
        ));
    }

    /**
     * 预校验秒杀资格（不扣库存）
     * GET /api/seckill/pre-check?voucherId=1&userId=1
     */
    @GetMapping("/pre-check")
    public Result<String> preCheck(@RequestParam Long voucherId,
                                   @RequestParam Long userId) {
        String result = seckillService.preCheckEligibility(voucherId, userId);
        return Result.ok(result);
    }

    /**
     * 初始化秒杀库存到 Redis（活动发布时调用）
     * POST /api/seckill/init-stock
     */
    @PostMapping("/init-stock")
    public Result<Void> initStock(@RequestBody Map<String, Object> req) {
        Long    voucherId = Long.parseLong(req.get("voucherId").toString());
        Integer stock     = Integer.parseInt(req.get("stock").toString());
        seckillService.initSeckillStock(voucherId, stock);
        return Result.ok();
    }
}
