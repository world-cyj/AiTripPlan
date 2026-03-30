package com.aitrip.backend.service.impl;

import com.aitrip.backend.constant.RedisKeyConstants;
import com.aitrip.backend.entity.TbAttraction;
import com.aitrip.backend.mapper.AttractionMapper;
import com.aitrip.backend.service.AttractionService;
import com.aitrip.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttractionServiceImpl implements AttractionService {

    private final AttractionMapper      attractionMapper;
    private final StringRedisTemplate   stringRedisTemplate;

    @Override
    public List<TbAttraction> searchByCity(String city, String keyword) {
        return attractionMapper.searchByCity(city, keyword == null ? "" : keyword);
    }

    @Override
    public int queryStock(Long attractionId) {
        // 查询该景区关联的秒杀优惠券库存（取第一个）
        String key = RedisKeyConstants.SECKILL_STOCK + attractionId;
        String val = stringRedisTemplate.opsForValue().get(key);
        return val != null ? Integer.parseInt(val) : 0;
    }

    @Override
    public String searchAttractions(String city, Integer typeId, Integer pageSize) {
        try {
            int size = (pageSize == null || pageSize <= 0) ? 10 : Math.min(pageSize, 20);
            List<TbAttraction> list = attractionMapper.searchAttractions(
                    city, typeId == null ? 0 : typeId, size);
            return JsonUtil.toJson(list);
        } catch (Exception e) {
            log.error("searchAttractions 失败", e);
            return "[]";
        }
    }

    @Override
    public String getAttractionDetail(Long attractionId) {
        try {
            TbAttraction attraction = attractionMapper.selectById(attractionId);
            if (attraction == null) {
                return JsonUtil.toJson(Map.of("error", true, "message", "景区不存在"));
            }
            return JsonUtil.toJson(attraction);
        } catch (Exception e) {
            log.error("getAttractionDetail 失败", e);
            return JsonUtil.toJson(Map.of("error", true, "message", e.getMessage()));
        }
    }

    @Override
    public String getNearbyAttractions(Double longitude, Double latitude, Double radiusKm) {
        try {
            // 简化实现：按城市查询（GEO 精确实现需 Redis GEO 预热数据）
            // 实际生产中可用 Redis GEORADIUS 命令
            List<TbAttraction> list = attractionMapper.searchAttractions(null, 0, 10);
            return JsonUtil.toJson(list);
        } catch (Exception e) {
            log.error("getNearbyAttractions 失败", e);
            return "[]";
        }
    }
}
