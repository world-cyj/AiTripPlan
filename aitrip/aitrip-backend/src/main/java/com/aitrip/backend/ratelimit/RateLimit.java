package com.aitrip.backend.ratelimit;

import java.lang.annotation.*;

/**
 * 分布式令牌桶限流注解
 * 用法：@RateLimit(key = "#voucherId", capacity = 100, rate = 50)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流维度 Key（支持 SpEL 表达式，如 "#voucherId"）
     */
    String key();

    /**
     * 令牌桶容量（最大并发突发量）
     */
    int capacity() default 100;

    /**
     * 每秒补充令牌数
     */
    int rate() default 50;

    /**
     * 每次消耗令牌数
     */
    int cost() default 1;

    /**
     * 限流提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
