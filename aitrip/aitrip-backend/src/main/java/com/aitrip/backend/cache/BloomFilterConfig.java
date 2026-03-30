package com.aitrip.backend.cache;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器 Bean 配置
 * 供 MultiLevelCacheManager 注入使用
 */
@Configuration
public class BloomFilterConfig {

    @Bean
    public RBloomFilter<String> attractionBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("bloom:attraction");
        // tryInit：如果已初始化则不重复初始化
        bloomFilter.tryInit(100_000L, 0.01);
        return bloomFilter;
    }
}
