package com.aitrip.backend.consumer;

import com.aitrip.backend.entity.TbIdempotent;
import com.aitrip.backend.entity.TbVoucherOrder;
import com.aitrip.backend.mapper.IdempotentMapper;
import com.aitrip.backend.mapper.VoucherOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 秒杀订单消费者
 * 幂等消费 + 指数退避重试 + 死信队列
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final IdempotentMapper  idempotentMapper;
    private final VoucherOrderMapper orderMapper;
    private final ObjectMapper      objectMapper;

    /**
     * 主消费方法：幂等 + RetryableTopic（1s->2s->4s 指数退避，最多4次）
     * 超过重试次数后路由到 .dlq 死信 Topic
     */
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltTopicSuffix = ".dlq",
            autoCreateTopics = "false"
    )
    @KafkaListener(topics = "aitrip.ticket.seckill", groupId = "aitrip-consumer-group")
    @Transactional(rollbackFor = Exception.class)
    public void consumeSeckillOrder(ConsumerRecord<String, String> record,
                                    Acknowledgment ack) {
        String messageId = record.key();
        String topic     = record.topic();

        log.info("[Consumer] 收到秒杀消息 messageId={} topic={}", messageId, topic);

        // 幂等检查
        if (idempotentMapper.existsSuccessRecord(messageId, topic) > 0) {
            log.warn("[Consumer] 重复消息，忽略 messageId={}", messageId);
            ack.acknowledge();
            return;
        }

        // 插入幂等记录（处理中）
        TbIdempotent idempotent = new TbIdempotent();
        idempotent.setMessageId(messageId);
        idempotent.setTopic(topic);
        idempotent.setStatus(0);
        idempotent.setExpireTime(LocalDateTime.now().plusDays(7));
        idempotentMapper.insertOrIgnore(idempotent);

        try {
            Map<?, ?> payload  = objectMapper.readValue(record.value(), Map.class);
            Long orderId   = Long.parseLong(payload.get("orderId").toString());
            Long voucherId = Long.parseLong(payload.get("voucherId").toString());
            Long userId    = Long.parseLong(payload.get("userId").toString());

            // 创建订单
            TbVoucherOrder order = new TbVoucherOrder();
            order.setId(orderId);
            order.setUserId(userId);
            order.setVoucherId(voucherId);
            order.setStatus(1);
            orderMapper.insert(order);

            // 标记幂等成功
            idempotentMapper.updateStatus(messageId, topic, 1);
            ack.acknowledge();
            log.info("[Consumer] 订单创建成功 orderId={}", orderId);

        } catch (Exception e) {
            idempotentMapper.updateStatus(messageId, topic, 2);
            log.error("[Consumer] 消息处理失败 messageId={}", messageId, e);
            throw new RuntimeException(e); // 触发重试
        }
    }

    /**
     * 死信队列消费：记录日志，等待人工处理
     */
    @KafkaListener(topics = "aitrip.ticket.seckill.dlq", groupId = "aitrip-dlq-group")
    public void consumeDLQ(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.error("[DLQ] 消息进入死信队列 topic={} key={} value={}",
                record.topic(), record.key(), record.value());
        // TODO: 接入告警（钉钉/邮件）
        ack.acknowledge();
    }
}
