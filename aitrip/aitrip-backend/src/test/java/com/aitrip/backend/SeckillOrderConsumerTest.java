package com.aitrip.backend;

import com.aitrip.backend.consumer.SeckillOrderConsumer;
import com.aitrip.backend.mapper.IdempotentMapper;
import com.aitrip.backend.mapper.VoucherOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 秒杀订单消费者 - 幂等测试（需要 Redis + MySQL 运行）
 * 运行命令：mvn test -pl aitrip-backend -Dtest=SeckillOrderConsumerTest
 */
@SpringBootTest(classes = AiTripBackendApplication.class)
@ActiveProfiles("test")
@DisplayName("秒杀订单消费者 - 幂等测试")
class SeckillOrderConsumerTest {

    @Autowired
    private SeckillOrderConsumer consumer;

    @Autowired
    private IdempotentMapper idempotentMapper;

    @Autowired
    private VoucherOrderMapper voucherOrderMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("正常消息 - 应创建订单并标记幂等成功")
    void testNormalMessage_createOrder() throws Exception {
        String messageId = "test-msg-" + System.currentTimeMillis();
        Long orderId   = 100000L + System.currentTimeMillis() % 10000;
        Long voucherId = 1L;
        Long userId    = 20001L;

        String payload = objectMapper.writeValueAsString(Map.of(
                "orderId",   orderId,
                "voucherId", voucherId,
                "userId",    userId
        ));

        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("aitrip.ticket.seckill", 0, 0L, messageId, payload);
        Acknowledgment ack = mock(Acknowledgment.class);

        consumer.consumeSeckillOrder(record, ack);

        int count = idempotentMapper.existsSuccessRecord(messageId, "aitrip.ticket.seckill");
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("重复消息 - 应直接忽略不重复创建订单")
    void testDuplicateMessage_shouldBeIgnored() throws Exception {
        String messageId = "dup-msg-" + System.currentTimeMillis();
        Long orderId   = 200000L + System.currentTimeMillis() % 10000;
        Long voucherId = 1L;
        Long userId    = 20002L;

        String payload = objectMapper.writeValueAsString(Map.of(
                "orderId",   orderId,
                "voucherId", voucherId,
                "userId",    userId
        ));

        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("aitrip.ticket.seckill", 0, 0L, messageId, payload);
        Acknowledgment ack = mock(Acknowledgment.class);

        consumer.consumeSeckillOrder(record, ack);
        consumer.consumeSeckillOrder(record, ack);

        long orderCount = voucherOrderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.aitrip.backend.entity.TbVoucherOrder>()
                        .eq("id", orderId));
        assertThat(orderCount).isEqualTo(1);
    }
}
