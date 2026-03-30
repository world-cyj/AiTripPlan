package com.aitrip.backend.service.impl;

import com.aitrip.backend.constant.RedisKeyConstants;
import com.aitrip.backend.service.SeckillService;
import com.aitrip.common.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 订阅通知服务（强化版）
 * 使用 Redis ZSet 存储订阅关系，Redisson 延迟队列推送预通知
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketNotifyService {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient      redissonClient;

    /**
     * 订阅到票提醒
     * ZSet: key=notify:subscribe:{voucherId}, score=订阅时间戳, member=userId
     */
    public String subscribe(Long voucherId, Long userId) {
        String key = RedisKeyConstants.NOTIFY_SUBSCRIBE + voucherId;
        double score = System.currentTimeMillis();

        // 检查是否已订阅
        Double existing = redisTemplate.opsForZSet().score(key, String.valueOf(userId));
        if (existing != null) {
            return JsonUtil.toJson(Map.of(
                    "success", true,
                    "message", "您已订阅该票的到票提醒，无需重复订阅"));
        }

        redisTemplate.opsForZSet().add(key, String.valueOf(userId), score);
        redisTemplate.expire(key, 30, TimeUnit.DAYS);

        log.info("[Notify] 用户订阅到票提醒: userId={}, voucherId={}", userId, voucherId);
        return JsonUtil.toJson(Map.of(
                "success", true,
                "message", "订阅成功！有余票时我们将第一时间通知您，您排队位置：" +
                        redisTemplate.opsForZSet().rank(key, String.valueOf(userId))));
    }

    /**
     * 取消订阅
     */
    public String unsubscribe(Long voucherId, Long userId) {
        String key = RedisKeyConstants.NOTIFY_SUBSCRIBE + voucherId;
        redisTemplate.opsForZSet().remove(key, String.valueOf(userId));
        return JsonUtil.toJson(Map.of("success", true, "message", "已取消订阅"));
    }

    /**
     * 查询订阅排队位置
     */
    public String getQueuePosition(Long voucherId, Long userId) {
        String key = RedisKeyConstants.NOTIFY_SUBSCRIBE + voucherId;
        Long rank = redisTemplate.opsForZSet().rank(key, String.valueOf(userId));
        Long total = redisTemplate.opsForZSet().size(key);
        if (rank == null) {
            return JsonUtil.toJson(Map.of("subscribed", false, "message", "您尚未订阅该票"));
        }
        return JsonUtil.toJson(Map.of(
                "subscribed", true,
                "position", rank + 1,
                "total", total,
                "message", "您当前排队位置：" + (rank + 1) + "，共" + total + "人等待"));
    }

    /**
     * 有票回流时触发通知（订单取消/退款时调用）
     * 使用 Redisson 延迟队列，2分钟后推送给前 N 个订阅用户
     */
    public void triggerNotify(Long voucherId, int restoredStock) {
        String key = RedisKeyConstants.NOTIFY_SUBSCRIBE + voucherId;
        Set<String> userIds = redisTemplate.opsForZSet()
                .range(key, 0, restoredStock - 1);
        if (userIds == null || userIds.isEmpty()) return;

        // 使用 Redisson 延迟队列推送（2分钟后执行）
        RQueue<String> queue = redissonClient.getQueue("notify:pending");
        RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(queue);

        for (String userId : userIds) {
            String msg = JsonUtil.toJson(Map.of(
                    "userId", userId,
                    "voucherId", voucherId,
                    "content", "您订阅的门票有余票了！请尽快前往抢购",
                    "type", "TICKET_AVAILABLE"
            ));
            delayedQueue.offer(msg, 2, TimeUnit.MINUTES);
        }

        // 移除已通知用户
        redisTemplate.opsForZSet().removeRange(key, 0, restoredStock - 1);
        log.info("[Notify] 触发到票通知: voucherId={}, 通知人数={}", voucherId, userIds.size());
    }

    /**
     * 定时消费延迟通知队列（每5秒扫描一次）
     */
    @Scheduled(fixedDelay = 5000)
    public void consumeNotifyQueue() {
        RQueue<String> queue = redissonClient.getQueue("notify:pending");
        String msg;
        int count = 0;
        while ((msg = queue.poll()) != null && count++ < 100) {
            try {
                log.info("[Notify] 发送通知: {}", msg);
                // 实际生产：调用短信/推送 API
                // smsService.send(userId, content);
                // pushService.push(userId, content);
            } catch (Exception e) {
                log.error("[Notify] 通知发送失败: {}", msg, e);
            }
        }
    }
}
