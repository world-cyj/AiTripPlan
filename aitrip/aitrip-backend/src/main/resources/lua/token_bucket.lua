-- token_bucket.lua
-- 分布式令牌桶限流（支持 VIP 优先级）
-- KEYS[1] = 令牌桶 Redis Key，如 seckill:token:bucket:{voucherId}
-- ARGV[1] = capacity   （桶容量）
-- ARGV[2] = rate       （每秒补充令牌数）
-- ARGV[3] = cost       （本次消耗令牌数，通常为1）
-- ARGV[4] = now        （当前时间戳，毫秒）
-- ARGV[5] = is_vip     （1=VIP，令牌消耗减半）
-- 返回值：1=放行，0=限流

local key      = KEYS[1]
local capacity = tonumber(ARGV[1])
local rate     = tonumber(ARGV[2])
local cost     = tonumber(ARGV[3])
local now      = tonumber(ARGV[4])
local is_vip   = tonumber(ARGV[5])

-- VIP 用户消耗令牌减半
if is_vip == 1 then
    cost = math.max(1, math.floor(cost / 2))
end

-- 读取当前桶状态
local bucket    = redis.call('HMGET', key, 'tokens', 'last_time')
local tokens    = tonumber(bucket[1])
local last_time = tonumber(bucket[2])

if tokens == nil then
    -- 首次初始化：满桶
    tokens    = capacity
    last_time = now
end

-- 按时间差补充令牌
local elapsed = (now - last_time) / 1000.0
local refill  = elapsed * rate
tokens = math.min(capacity, tokens + refill)

-- 判断令牌是否足够
if tokens >= cost then
    tokens = tokens - cost
    redis.call('HMSET', key, 'tokens', tokens, 'last_time', now)
    redis.call('EXPIRE', key, 3600)
    return 1
else
    -- 令牌不足，更新状态但不扣减
    redis.call('HMSET', key, 'tokens', tokens, 'last_time', now)
    redis.call('EXPIRE', key, 3600)
    return 0
end
