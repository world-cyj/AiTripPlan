package com.aitrip.backend.service;

/**
 * 秒杀服务接口
 */
public interface SeckillService {

    /**
     * 执行秒杀（令牌桶限流 + Lua 原子扣减 + Outbox 写入）
     * @return orderId
     */
    Long seckillVoucher(Long voucherId, Long userId);

    /**
     * 预校验秒杀资格（不扣库存）
     * @return JSON：{eligible, stock, alreadyBought, reason}
     */
    String preCheckEligibility(Long voucherId, Long userId);

    /**
     * 初始化秒杀库存到 Redis
     */
    void initSeckillStock(Long voucherId, Integer stock);

    /**
     * 订阅到票通知（ZSet）
     */
    void subscribeNotify(Long voucherId, Long userId);
}
