package com.aitrip.backend.constant;

public final class RedisKeyConstants {

    private RedisKeyConstants() {}

    // 用户 Session
    public static final String SESSION_USER = "session:user:";

    // 景区缓存
    public static final String CACHE_ATTRACTION = "cache:attraction:";
    public static final String CACHE_ATTRACTION_LIST = "cache:attraction:list:";

    // 秒杀
    public static final String SECKILL_STOCK = "seckill:stock:";
    public static final String SECKILL_TOKEN_BUCKET = "seckill:token:bucket:";
    public static final String SECKILL_QUALIFY = "seckill:qualify:";

    // 分布式锁
    public static final String LOCK_SECKILL = "lock:seckill:";
    public static final String LOCK_ORDER = "lock:order:";
    public static final String LOCK_CACHE_REBUILD = "lock:rebuild:attraction:";

    // 地理位置
    public static final String GEO_ATTRACTION = "geo:attraction:";

    // 布隆过滤器
    public static final String BLOOM_ATTRACTION = "bloom:attraction";
    public static final String BLOOM_VOUCHER = "bloom:voucher";

    // 订阅通知（ZSet score=订阅时间 member=userId）
    public static final String NOTIFY_SUBSCRIBE = "notify:subscribe:";

    // 排行榜
    public static final String RANK_BUYER_DAILY = "rank:buyer:daily:";
    public static final String RANK_BLOG_LIKED = "rank:blog:liked:";

    // UV 统计（HyperLogLog）
    public static final String UV_ATTRACTION = "uv:attraction:";

    // 签到（BitMap）
    public static final String SIGN_USER = "sign:user:";

    // 关注（Set）
    public static final String FOLLOW_USER = "follow:user:";

    // JWT Token 黑名单
    public static final String TOKEN_BLACKLIST = "token:blacklist:";

    // 用户偏好（Hash）
    public static final String USER_PREFERENCE = "user:preference:";

    // 景区热榜（ZSet score=综合热度）
    public static final String RANK_ATTRACTION_HOT = "rank:attraction:hot";
    public static final String RANK_ATTRACTION_CITY = "rank:attraction:city:";

    // UV 统计
    public static final String UV_DAILY = "uv:daily:";

    // 订单查询缓存
    public static final String CACHE_ORDER = "cache:order:";
    public static final String CACHE_ORDER_LIST = "cache:order:list:";
}
