package com.aitrip.backend.controller;

import com.aitrip.backend.entity.TbAttraction;
import com.aitrip.backend.service.AttractionService;
import com.aitrip.backend.service.impl.AttractionCacheService;
import com.aitrip.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 景区接口
 * 供前端 + MCP Tool 调用
 */
@RestController
@RequestMapping("/api/attractions")
@RequiredArgsConstructor
public class AttractionController {

    private final AttractionService      attractionService;
    private final AttractionCacheService cacheService;

    /**
     * 按城市搜索景区
     * GET /api/attractions/search?city=西安&keyword=兵马俑
     */
    @GetMapping("/search")
    public Result<List<TbAttraction>> search(
            @RequestParam(required = false, defaultValue = "") String city,
            @RequestParam(required = false, defaultValue = "") String keyword) {
        return Result.ok(attractionService.searchByCity(city, keyword));
    }

    /**
     * 查询景区详情（走多层缓存）
     * GET /api/attractions/{id}
     */
    @GetMapping("/{id}")
    public Result<TbAttraction> detail(@PathVariable Long id) {
        return Result.ok(cacheService.getAttractionById(id));
    }

    /**
     * 查询景区门票库存
     * GET /api/attractions/{id}/stock
     */
    @GetMapping("/{id}/stock")
    public Result<Map<String, Object>> stock(@PathVariable Long id) {
        int stock = attractionService.queryStock(id);
        return Result.ok(Map.of(
                "attractionId", id,
                "stock",        stock,
                "available",    stock > 0
        ));
    }
}
