package com.aitrip.backend.cache;

import com.aitrip.backend.mapper.AttractionMapper;
import com.aitrip.backend.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 布隆过滤器初始化
 * 应用启动后自动从 DB 加载所有合法景区 ID 和优惠券 ID
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterInitializer implements ApplicationRunner {

    private final RedissonClient   redissonClient;
    private final AttractionMapper attractionMapper;
    private final VoucherMapper    voucherMapper;

    /** 初始容量 10 万，误判率 0.01 */
    private static final long   EXPECTED_INSERTIONS = 100_000L;
    private static final double FALSE_PROBABILITY    = 0.01;

    /** Spring 注入给 MultiLevelCacheManager 使用 */
    private RBloomFilter<String> attractionBloomFilter;

    @Override
    public void run(ApplicationArguments args) {
        try {
            initAttractionBloom();
        } catch (Exception e) {
            log.warn("[BloomFilter] 景区布隆过滤器初始化失败（不影响启动）: {}", e.getMessage());
        }
        try {
            initVoucherBloom();
        } catch (Exception e) {
            log.warn("[BloomFilter] 优惠券布隆过滤器初始化失败（不影响启动）: {}", e.getMessage());
        }
    }

    private void initAttractionBloom() {
        attractionBloomFilter = redissonClient.getBloomFilter("bloom:attraction");
        attractionBloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);

        List<Long> ids = attractionMapper.selectAllValidIds();
        ids.forEach(id -> attractionBloomFilter.add(String.valueOf(id)));
        log.info("[BloomFilter] 景区布隆过滤器初始化完成，加载 {} 个合法 ID", ids.size());
    }

    private void initVoucherBloom() {
        RBloomFilter<String> voucherBloom = redissonClient.getBloomFilter("bloom:voucher");
        voucherBloom.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);

        voucherMapper.selectList(null)
                .forEach(v -> voucherBloom.add(String.valueOf(v.getId())));
        log.info("[BloomFilter] 优惠券布隆过滤器初始化完成");
    }

    /**
     * 动态添加新 ID（新增景区时调用）
     */
    public void addAttractionId(Long id) {
        if (attractionBloomFilter != null) {
            attractionBloomFilter.add(String.valueOf(id));
        }
    }
}
