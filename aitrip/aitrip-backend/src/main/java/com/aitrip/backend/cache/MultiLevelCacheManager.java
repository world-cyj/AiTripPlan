package com.aitrip.backend.cache;

import com.aitrip.common.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 多层缓存管理器
 * 链路：Caffeine L1 → 布隆过滤器 → Redis L2 → 加锁重建（双重检查）→ DB
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultiLevelCacheManager {

    private static final String NULL_VALUE     = "__NULL__";
    private static final Duration NULL_TTL     = Duration.ofSeconds(30);
    private static final Duration ENTITY_TTL   = Duration.ofMinutes(30);
    private static final long LOCK_WAIT_SEC    = 3L;
    private static final long LOCK_LEASE_SEC   = 30L;

    private final Cache<String, String>       localCache;
    private final StringRedisTemplate         redisTemplate;
    private final RedissonClient              redissonClient;
    private final RBloomFilter<String>        attractionBloomFilter;
    private final ObjectMapper                objectMapper;

    /**
     * 通用缓存查询（防穿透 + 防击穿）
     *
     * @param cacheKey Redis / L1 缓存 Key
     * @param bloomKey 布隆过滤器检查 Key（通常为 ID 字符串）
     * @param lockKey  缓存重建分布式锁 Key
     * @param dbLoader DB 查询函数（cache miss 时调用）
     * @param type     反序列化目标类型
     */
    public <T> T getWithBloomAndRebuild(
            String cacheKey,
            String bloomKey,
            String lockKey,
            Function<Void, T> dbLoader,
            Class<T> type) {

        // ---- L1: 本地缓存 Caffeine ----
        String localVal = localCache.getIfPresent(cacheKey);
        if (localVal != null) {
            log.debug("[Cache L1 HIT] key={}", cacheKey);
            return NULL_VALUE.equals(localVal) ? null : deserialize(localVal, type);
        }

        // ---- L2: 布隆过滤器拦截非法 ID ----
        if (!attractionBloomFilter.contains(bloomKey)) {
            log.debug("[Bloom REJECT] bloomKey={}", bloomKey);
            return null;
        }

        // ---- L3: Redis 缓存 ----
        String redisVal = redisTemplate.opsForValue().get(cacheKey);
        if (redisVal != null) {
            log.debug("[Cache L2 HIT] key={}", cacheKey);
            if (NULL_VALUE.equals(redisVal)) {
                localCache.put(cacheKey, NULL_VALUE);
                return null;
            }
            T result = deserialize(redisVal, type);
            localCache.put(cacheKey, redisVal);
            return result;
        }

        // ---- L4: 获取重建锁（防击穿）----
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(LOCK_WAIT_SEC, LOCK_LEASE_SEC, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("[Cache REBUILD] 获取锁超时，降级返回 null, key={}", cacheKey);
                return null;
            }

            // ---- L5: 双重检查（锁内再查 Redis）----
            redisVal = redisTemplate.opsForValue().get(cacheKey);
            if (redisVal != null) {
                log.debug("[Cache L2 HIT after lock] key={}", cacheKey);
                return NULL_VALUE.equals(redisVal) ? null : deserialize(redisVal, type);
            }

            // ---- L6: 查询 DB ----
            log.info("[Cache REBUILD] DB 查询 key={}", cacheKey);
            T dbResult = dbLoader.apply(null);

            // ---- L7: 回填缓存 ----
            if (dbResult == null) {
                redisTemplate.opsForValue().set(cacheKey, NULL_VALUE, NULL_TTL);
                localCache.put(cacheKey, NULL_VALUE);
            } else {
                String serialized = serialize(dbResult);
                redisTemplate.opsForValue().set(cacheKey, serialized, ENTITY_TTL);
                localCache.put(cacheKey, serialized);
            }
            return dbResult;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Cache REBUILD] 锁等待被中断 key={}", cacheKey);
            return null;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 主动使某个 Key 的 L1 缓存失效（更新场景使用）
     */
    public void invalidateLocal(String cacheKey) {
        localCache.invalidate(cacheKey);
    }

    /**
     * 主动使某个 Key 的 L1 + L2 缓存全部失效
     */
    public void invalidateAll(String cacheKey) {
        localCache.invalidate(cacheKey);
        redisTemplate.delete(cacheKey);
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.error("[Cache] 反序列化失败 type={} json={}", type.getSimpleName(), json, e);
            return null;
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("[Cache] 序列化失败 obj={}", obj, e);
            return "{}";
        }
    }
}
