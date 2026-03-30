package com.aitrip.backend.service.impl;

import com.aitrip.backend.constant.RedisKeyConstants;
import com.aitrip.backend.entity.TbOutbox;
import com.aitrip.backend.mapper.OutboxMapper;
import com.aitrip.backend.mapper.SeckillVoucherMapper;
import com.aitrip.backend.mapper.VoucherOrderMapper;
import com.aitrip.backend.ratelimit.RateLimit;
import com.aitrip.backend.ratelimit.TokenBucketRateLimiter;
import com.aitrip.backend.service.SeckillService;
import com.aitrip.common.exception.BusinessException;
import com.aitrip.common.exception.ErrorCode;
import com.aitrip.common.util.JsonUtil;
import com.aitrip.common.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 秒杀服务实现
 * 流程：令牌桶限流 → Lua 原子扣减+防重购 → 写 Outbox → Kafka 异步下单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final StringRedisTemplate   stringRedisTemplate;
    private final TokenBucketRateLimiter rateLimiter;
    private final OutboxMapper           outboxMapper;
    private final SeckillVoucherMapper   seckillVoucherMapper;
    private final VoucherOrderMapper     voucherOrderMapper;
    private final SnowflakeIdGenerator   snowflakeIdGenerator;

    private DefaultRedisScript<Long> seckillScript;

    @PostConstruct
    public void init() {
        seckillScript = new DefaultRedisScript<>();
        seckillScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/seckill.lua")));
        seckillScript.setResultType(Long.class);
    }

    @Override
    @RateLimit(key = "#voucherId", capacity = 100, rate = 50,
               message = "当前抢购人数过多，请稍后重试")
    @Transactional(rollbackFor = Exception.class)
    public Long seckillVoucher(Long voucherId, Long userId) {
        String stockKey   = RedisKeyConstants.SECKILL_STOCK + voucherId;
        String qualifyKey = RedisKeyConstants.SECKILL_QUALIFY + voucherId;

        // Lua 原子扣减（0=成功, 1=库存不足, 2=重复购买）
        Long result = stringRedisTemplate.execute(
                seckillScript,
                List.of(stockKey, qualifyKey),
                String.valueOf(userId));

        if (result == null || result == 1L) {
            throw new BusinessException(ErrorCode.STOCK_EMPTY);
        }
        if (result == 2L) {
            throw new BusinessException(ErrorCode.REPEAT_BUY);
        }

        long orderId = snowflakeIdGenerator.nextId();

        // 写 Outbox（同一事务）
        TbOutbox outbox = new TbOutbox();
        outbox.setTopic("aitrip.ticket.seckill");
        outbox.setMessageId(UUID.randomUUID().toString());
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId",   orderId);
        payload.put("voucherId", voucherId);
        payload.put("userId",    userId);
        payload.put("createTime", System.currentTimeMillis());
        outbox.setPayload(JsonUtil.toJson(payload));
        outbox.setStatus(0);
        outbox.setRetryCount(0);
        outbox.setNextRetryTime(LocalDateTime.now());
        outbox.setLastError(null);
        outboxMapper.insert(outbox);

        log.info("秒杀成功 voucherId={} userId={} orderId={}", voucherId, userId, orderId);
        return orderId;
    }

    @Override
    public String preCheckEligibility(Long voucherId, Long userId) {
        String stockKey   = RedisKeyConstants.SECKILL_STOCK + voucherId;
        String qualifyKey = RedisKeyConstants.SECKILL_QUALIFY + voucherId;

        String stockStr = stringRedisTemplate.opsForValue().get(stockKey);
        int stock = stockStr != null ? Integer.parseInt(stockStr) : 0;
        boolean alreadyBought = Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet().isMember(qualifyKey, String.valueOf(userId)));

        Map<String, Object> res = new HashMap<>();
        res.put("eligible",      !alreadyBought && stock > 0);
        res.put("stock",         stock);
        res.put("alreadyBought", alreadyBought);
        res.put("voucherId",     voucherId);
        if (alreadyBought) {
            res.put("reason", "您已购买过此门票");
        } else if (stock <= 0) {
            res.put("reason", "门票已售完，可订阅到票提醒");
        } else {
            res.put("reason", "可以参与抢购");
        }
        return JsonUtil.toJson(res);
    }

    @Override
    public void initSeckillStock(Long voucherId, Integer stock) {
        String stockKey = RedisKeyConstants.SECKILL_STOCK + voucherId;
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
        log.info("初始化秒杀库存 voucherId={} stock={}", voucherId, stock);
    }

    @Override
    public void subscribeNotify(Long voucherId, Long userId) {
        String key = RedisKeyConstants.NOTIFY_SUBSCRIBE + voucherId;
        stringRedisTemplate.opsForZSet().add(key,
                String.valueOf(userId), System.currentTimeMillis());
        log.info("用户订阅到票通知 voucherId={} userId={}", voucherId, userId);
    }
}
