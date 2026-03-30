package com.aitrip.backend.service.impl;

import com.aitrip.backend.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 到票通知调度器
 * 定时检查是否有余票回流，并通知最早订阅的用户
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyScheduler {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 每30秒扫描一次所有订阅 Key，检查对应库存是否回流
     */
    @Scheduled(fixedDelay = 30000)
    public void checkAndNotify() {
        // 扫描所有订阅 Key
        Set<String> keys = stringRedisTemplate.keys(
                RedisKeyConstants.NOTIFY_SUBSCRIBE + "*");
        if (keys == null || keys.isEmpty()) return;

        for (String subscribeKey : keys) {
            String voucherIdStr = subscribeKey
                    .replace(RedisKeyConstants.NOTIFY_SUBSCRIBE, "");
            try {
                Long voucherId = Long.parseLong(voucherIdStr);
                checkVoucher(subscribeKey, voucherId);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void checkVoucher(String subscribeKey, Long voucherId) {
        String stockKey = RedisKeyConstants.SECKILL_STOCK + voucherId;
        String stockStr = stringRedisTemplate.opsForValue().get(stockKey);
        if (stockStr == null) return;

        int stock = Integer.parseInt(stockStr);
        if (stock <= 0) return;

        // 有余票：取最早订阅（score最小）的用户推送通知
        Set<String> userIds = stringRedisTemplate.opsForZSet()
                .range(subscribeKey, 0, stock - 1L);
        if (userIds == null || userIds.isEmpty()) return;

        for (String userId : userIds) {
            sendNotification(userId, voucherId, stock);
        }

        // 移除已通知用户
        stringRedisTemplate.opsForZSet()
                .removeRange(subscribeKey, 0, stock - 1L);

        log.info("[Notify] 余票回流通知 voucherId={} 通知用户数={} 剩余库存={}",
                voucherId, userIds.size(), stock);
    }

    private void sendNotification(String userId, Long voucherId, int stock) {
        // TODO: 接入钉钉/邮件/WebSocket 推送
        log.info("[Notify] 推送通知 userId={} voucherId={} 余票={}",
                userId, voucherId, stock);
    }
}
