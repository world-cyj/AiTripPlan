package com.aitrip.backend.service.impl;

import com.aitrip.backend.constant.RedisKeyConstants;
import com.aitrip.backend.entity.TbAttraction;
import com.aitrip.backend.mapper.AttractionMapper;
import com.aitrip.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 景区热榜服务
 * 使用 Redis ZSet 维护实时热度排行（score = 加权热度）
 * 热度公式：score = sold*3 + comments*2 + score_rating*10 + uv*1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttractionRankService {

    private final StringRedisTemplate redisTemplate;
    private final AttractionMapper attractionMapper;

    /**
     * 记录景区访问（UV + 热度 +1）
     */
    public void recordVisit(Long attractionId, String userId) {
        // HyperLogLog UV 统计
        String uvKey = RedisKeyConstants.UV_ATTRACTION + attractionId;
        redisTemplate.opsForHyperLogLog().add(uvKey, userId);

        // 热榜全局 ZSet +1
        redisTemplate.opsForZSet().incrementScore(
                RedisKeyConstants.RANK_ATTRACTION_HOT,
                String.valueOf(attractionId), 1.0);
    }

    /**
     * 购票成功后热度 +3
     */
    public void recordPurchase(Long attractionId) {
        redisTemplate.opsForZSet().incrementScore(
                RedisKeyConstants.RANK_ATTRACTION_HOT,
                String.valueOf(attractionId), 3.0);
    }

    /**
     * 查询全局热榜 Top N
     */
    public String getHotRank(int topN) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(
                        RedisKeyConstants.RANK_ATTRACTION_HOT, 0, topN - 1);

        if (tuples == null || tuples.isEmpty()) return "[]";

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            Long id = Long.parseLong(Objects.requireNonNull(t.getValue()));
            TbAttraction attraction = attractionMapper.selectById(id);
            if (attraction == null) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank++);
            item.put("attractionId", id);
            item.put("name", attraction.getName());
            item.put("city", attraction.getCity());
            item.put("score", attraction.getScore());
            item.put("price", attraction.getPrice());
            item.put("hotScore", t.getScore());
            // UV 统计
            Long uv = redisTemplate.opsForHyperLogLog()
                    .size(RedisKeyConstants.UV_ATTRACTION + id);
            item.put("uv", uv);
            result.add(item);
        }
        return JsonUtil.toJson(result);
    }

    /**
     * 查询城市热榜
     */
    public String getCityRank(String city, int topN) {
        List<TbAttraction> list = attractionMapper.searchAttractions(city, 0, topN);
        // 按热度分排序（结合 ZSet）
        List<Map<String, Object>> result = list.stream().map(a -> {
            Double hotScore = redisTemplate.opsForZSet().score(
                    RedisKeyConstants.RANK_ATTRACTION_HOT, String.valueOf(a.getId()));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("attractionId", a.getId());
            item.put("name", a.getName());
            item.put("city", a.getCity());
            item.put("score", a.getScore());
            item.put("price", a.getPrice());
            item.put("address", a.getAddress());
            item.put("hotScore", hotScore != null ? hotScore : 0.0);
            return item;
        }).sorted(Comparator.comparingDouble(
                m -> -((Number) m.get("hotScore")).doubleValue())
        ).collect(Collectors.toList());
        return JsonUtil.toJson(result);
    }

    /**
     * 每天凌晨2点衰减热度（防止旧数据霸榜）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void decayHotScore() {
        Set<String> members = redisTemplate.opsForZSet().range(
                RedisKeyConstants.RANK_ATTRACTION_HOT, 0, -1);
        if (members == null) return;
        members.forEach(m -> redisTemplate.opsForZSet().incrementScore(
                RedisKeyConstants.RANK_ATTRACTION_HOT, m, -0.1));
        log.info("[Rank] 热榜热度衰减完成，影响 {} 个景区", members.size());
    }
}
