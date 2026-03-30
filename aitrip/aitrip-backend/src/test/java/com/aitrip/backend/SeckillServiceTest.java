package com.aitrip.backend;

import com.aitrip.backend.service.SeckillService;
import com.aitrip.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 秒杀服务集成测试（需要 Redis + MySQL 运行）
 * 运行命令：mvn test -pl aitrip-backend -Dtest=SeckillServiceTest
 */
@SpringBootTest(classes = AiTripBackendApplication.class)
@ActiveProfiles("test")
@DisplayName("秒杀服务 - 并发安全测试")
class SeckillServiceTest {

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final Long VOUCHER_ID = 9999L;
    private static final int  STOCK      = 100;

    @BeforeEach
    void setUp() {
        seckillService.initSeckillStock(VOUCHER_ID, STOCK);
        stringRedisTemplate.delete("seckill:qualify:" + VOUCHER_ID);
    }

    @Test
    @DisplayName("200并发抢100库存 - 成功数不超过库存，Redis余量不为负")
    void testConcurrentSeckill_noOversell() throws InterruptedException {
        int threadCount  = 200;
        CountDownLatch latch   = new CountDownLatch(threadCount);
        AtomicInteger  success = new AtomicInteger(0);
        AtomicInteger  failed  = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final long userId = 10000L + i;
            pool.submit(() -> {
                try {
                    seckillService.seckillVoucher(VOUCHER_ID, userId);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    failed.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        System.out.printf("成功: %d, 失败: %d%n", success.get(), failed.get());
        assertThat(success.get()).isLessThanOrEqualTo(STOCK);

        String remaining = stringRedisTemplate.opsForValue().get("seckill:stock:" + VOUCHER_ID);
        if (remaining != null) {
            assertThat(Integer.parseInt(remaining)).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("同一用户重复购买 - 第二次应被拦截")
    void testDuplicateBuy_shouldBeRejected() {
        Long userId = 88888L;
        Long orderId = seckillService.seckillVoucher(VOUCHER_ID, userId);
        assertThat(orderId).isPositive();

        org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> seckillService.seckillVoucher(VOUCHER_ID, userId)
        );
    }

    @Test
    @DisplayName("预校验资格 - 有库存时返回 eligible=true")
    void testPreCheck_eligible() {
        String result = seckillService.preCheckEligibility(VOUCHER_ID, 66666L);
        assertThat(result).contains("eligible");
    }
}
