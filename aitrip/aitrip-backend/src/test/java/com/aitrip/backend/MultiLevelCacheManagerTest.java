package com.aitrip.backend;

import com.aitrip.backend.cache.MultiLevelCacheManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多层缓存集成测试（需要 Redis 运行）
 * 运行命令：mvn test -pl aitrip-backend -Dtest=MultiLevelCacheManagerTest
 */
@SpringBootTest(classes = AiTripBackendApplication.class)
@ActiveProfiles("test")
@DisplayName("多层缓存管理器测试")
class MultiLevelCacheManagerTest {

    @Autowired
    private MultiLevelCacheManager cacheManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("布隆过滤器拦截不存在的景区ID")
    void testBloomFilter_rejectsInvalidId() {
        Long invalidId = -999999L;
        Object result = cacheManager.getWithBloomAndRebuild(
                "cache:attraction:" + invalidId,
                String.valueOf(invalidId),
                "lock:rebuild:attraction:" + invalidId,
                v -> null,
                Object.class
        );
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("空值缓存防穿透 - DB 只被查询一次")
    void testNullValueCaching_preventsPenetration() {
        String cacheKey = "cache:attraction:test:null:888";
        String bloomKey = "888";
        String lockKey  = "lock:rebuild:test:888";
        AtomicInteger dbCallCount = new AtomicInteger(0);

        cacheManager.getWithBloomAndRebuild(cacheKey, bloomKey, lockKey,
                v -> { dbCallCount.incrementAndGet(); return null; }, Object.class);

        cacheManager.getWithBloomAndRebuild(cacheKey, bloomKey, lockKey,
                v -> { dbCallCount.incrementAndGet(); return null; }, Object.class);

        // DB 最多被调用一次
        assertThat(dbCallCount.get()).isLessThanOrEqualTo(1);
        cacheManager.invalidateAll(cacheKey);
    }

    @Test
    @DisplayName("并发击穿防护 - 50线程请求同一Key，DB只被查询一次")
    void testConcurrentRebuild_onlyOneDbCall() throws InterruptedException {
        String cacheKey = "cache:attraction:test:concurrent:1001";
        String bloomKey = "1001";
        String lockKey  = "lock:rebuild:test:1001";
        cacheManager.invalidateAll(cacheKey);

        AtomicInteger dbCallCount = new AtomicInteger(0);
        int threadCount = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    cacheManager.getWithBloomAndRebuild(cacheKey, bloomKey, lockKey,
                            v -> { dbCallCount.incrementAndGet(); return "value"; },
                            String.class);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();

        System.out.printf("DB 调用次数: %d（期望: 1）%n", dbCallCount.get());
        assertThat(dbCallCount.get()).isEqualTo(1);
        cacheManager.invalidateAll(cacheKey);
    }
}
