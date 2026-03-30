-- seckill.lua
-- 秒杀原子脚本：检查库存 + 防重购 + 扣减
-- KEYS[1] = seckill:stock:{voucherId}
-- KEYS[2] = seckill:qualify:{voucherId}
-- ARGV[1] = userId
-- 返回值：0=成功, 1=库存不足, 2=重复购买

local stockKey   = KEYS[1]
local qualifyKey = KEYS[2]
local userId     = ARGV[1]

-- 1. 检查是否已购买（防重购）
if redis.call('SISMEMBER', qualifyKey, userId) == 1 then
    return 2
end

-- 2. 检查库存
local stock = tonumber(redis.call('GET', stockKey))
if stock == nil or stock <= 0 then
    return 1
end

-- 3. 原子扣减库存 + 记录已购用户
redis.call('DECRBY', stockKey, 1)
redis.call('SADD', qualifyKey, userId)

return 0
