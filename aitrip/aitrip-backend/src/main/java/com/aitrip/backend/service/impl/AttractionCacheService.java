package com.aitrip.backend.service.impl;

import com.aitrip.backend.cache.MultiLevelCacheManager;
import com.aitrip.backend.constant.RedisKeyConstants;
import com.aitrip.backend.entity.TbAttraction;
import com.aitrip.backend.mapper.AttractionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 景区缓存服务
 * 封装多层缓存查询，供 Controller 和 MCP Tool 调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttractionCacheService {

    private final MultiLevelCacheManager cacheManager;
    private final AttractionMapper       attractionMapper;

    /**
     * 查询景区详情（走多层缓存）
     */
    public TbAttraction getAttractionById(Long id) {
        String cacheKey = RedisKeyConstants.CACHE_ATTRACTION + id;
        String bloomKey = String.valueOf(id);
        String lockKey  = RedisKeyConstants.LOCK_CACHE_REBUILD + id;

        return cacheManager.getWithBloomAndRebuild(
                cacheKey,
                bloomKey,
                lockKey,
                v -> attractionMapper.selectById(id),
                TbAttraction.class
        );
    }

    /**
     * 批量预热景区缓存（启动时或定时任务调用）
     */
    public void warmUp(List<Long> ids) {
        ids.forEach(id -> {
            try {
                getAttractionById(id);
            } catch (Exception e) {
                log.warn("[WarmUp] 景区 {} 缓存预热失败: {}", id, e.getMessage());
            }
        });
        log.info("[WarmUp] 景区缓存预热完成，共 {} 条", ids.size());
    }

    /**
     * 主动刷新某景区缓存（数据更新时调用）
     */
    public void refreshCache(Long id) {
        String cacheKey = RedisKeyConstants.CACHE_ATTRACTION + id;
        cacheManager.invalidateAll(cacheKey);
        log.info("[Cache] 景区 {} 缓存已清除，等待下次访问时重建", id);
    }
}
