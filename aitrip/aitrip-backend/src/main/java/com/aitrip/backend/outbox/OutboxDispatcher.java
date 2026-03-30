package com.aitrip.backend.outbox;

import com.aitrip.backend.entity.TbOutbox;
import com.aitrip.backend.mapper.OutboxMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbox 消息分发器
 * 定时扫描 tb_outbox 待发送消息，投递到 Kafka
 * 投递失败按指数退避重试：30s → 60s → 120s → 300s → 600s
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDispatcher {

    private static final int   BATCH_SIZE = 100;
    private static final int[] RETRY_DELAYS = {30, 60, 120, 300, 600}; // 秒
    private static final int   MAX_RETRY = RETRY_DELAYS.length;

    private final OutboxMapper                outboxMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 每 2 秒扫描一次待发送消息
     */
    @Scheduled(fixedDelay = 2000)
    public void dispatch() {
        List<TbOutbox> pending = outboxMapper.selectPendingMessages(BATCH_SIZE);
        if (pending.isEmpty()) return;

        log.debug("[Outbox] 扫描到 {} 条待发送消息", pending.size());

        for (TbOutbox msg : pending) {
            try {
                kafkaTemplate.send(msg.getTopic(), msg.getMessageId(), msg.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                outboxMapper.markDelivered(msg.getId());
                                log.debug("[Outbox] 消息投递成功 messageId={}", msg.getMessageId());
                            } else {
                                handleRetry(msg, ex.getMessage());
                            }
                        });
            } catch (Exception e) {
                handleRetry(msg, e.getMessage());
            }
        }
    }

    private void handleRetry(TbOutbox msg, String errorMsg) {
        int retryCount = msg.getRetryCount() == null ? 0 : msg.getRetryCount();
        retryCount++;

        if (retryCount > MAX_RETRY) {
            outboxMapper.markFailed(msg.getId(), "超过最大重试次数: " + errorMsg);
            log.error("[Outbox] 消息超过最大重试次数，标记失败 messageId={}", msg.getMessageId());
            return;
        }

        int delaySec = RETRY_DELAYS[Math.min(retryCount - 1, RETRY_DELAYS.length - 1)];
        outboxMapper.updateRetry(msg.getId(), delaySec, errorMsg);
        log.warn("[Outbox] 消息投递失败，{}秒后重试 messageId={} retry={}",
                delaySec, msg.getMessageId(), retryCount);
    }
}
