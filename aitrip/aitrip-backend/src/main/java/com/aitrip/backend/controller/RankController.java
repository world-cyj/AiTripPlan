package com.aitrip.backend.controller;

import com.aitrip.backend.service.impl.AttractionRankService;
import com.aitrip.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 热榜接口
 * GET /api/rank/hot?topN=10        — 全站热榜
 * GET /api/rank/city/{city}?topN=5 — 城市热榜
 * POST /api/rank/visit             — 记录访问（供前端埋点）
 */
@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
public class RankController {

    private final AttractionRankService rankService;

    @GetMapping("/hot")
    public Result<String> hotRank(
            @RequestParam(defaultValue = "10") int topN) {
        return Result.ok(rankService.getHotRank(topN));
    }

    @GetMapping("/city/{city}")
    public Result<String> cityRank(
            @PathVariable String city,
            @RequestParam(defaultValue = "5") int topN) {
        return Result.ok(rankService.getCityRank(city, topN));
    }

    @PostMapping("/visit")
    public Result<Void> recordVisit(
            @RequestParam Long attractionId,
            @RequestParam(defaultValue = "anonymous") String userId) {
        rankService.recordVisit(attractionId, userId);
        return Result.ok();
    }
}
