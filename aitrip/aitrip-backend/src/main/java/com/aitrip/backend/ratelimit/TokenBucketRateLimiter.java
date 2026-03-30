package com.aitrip.backend.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 基于 Redis Lua 的分布式令牌桶限流器
 * 支持 VIP 优先级（VIP 消耗令牌减半）
 */
@Slf4j
@Component
public class TokenBucketRateLimiter {

    private final RedissonClient redissonClient;
    private final String luaScript;

    public TokenBucketRateLimiter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.luaScript = loadScript("lua/token_bucket.lua");
    }

    /**
     * 尝试获取令牌
     *
     * @param key      限流维度 Key（如优惠券ID、接口路径）
     * @param capacity 桶容量
     * @param rate     每秒补充令牌数
     * @param cost     本次消耗令牌数
     * @param isVip    是否 VIP（VIP 消耗减半）
     * @return true=放行，false=限流
     */
    public boolean tryAcquire(String key, int capacity, int rate, int cost, boolean isVip) {
        long now = System.currentTimeMillis();
        try {
            Long result = redissonClient.getScript(LongCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    luaScript,
                    RScript.ReturnType.INTEGER,
                    Collections.singletonList(key),
                    String.valueOf(capacity),
                    String.valueOf(rate),
                    String.valueOf(cost),
                    String.valueOf(now),
                    isVip ? "1" : "0"
            );
            return result != null && result == 1L;
        } catch (Exception e) {
            log.error("[RateLimit] Lua 执行失败，降级放行 key={}", key, e);
            return true; // 降级：Redis 不可用时放行，避免影响主流程
        }
    }

    /**
     * 便捷方法：使用默认参数（capacity=100, rate=50, cost=1）
     */
    public boolean tryAcquire(String key, boolean isVip) {
        return tryAcquire(key, 100, 50, 1, isVip);
    }

    private String loadScript(String path) {
        try (var is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Lua script not found: " + path);
            return new String(is.readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load lua script: " + path, e);
        }
    }
}
