package com.aitrip.backend.service.impl;

import com.aitrip.backend.constant.RedisKeyConstants;
import com.aitrip.backend.entity.TbVoucherOrder;
import com.aitrip.backend.mapper.VoucherOrderMapper;
import com.aitrip.backend.service.OrderService;
import com.aitrip.common.exception.BusinessException;
import com.aitrip.common.exception.ErrorCode;
import com.aitrip.common.util.JsonUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final VoucherOrderMapper orderMapper;
    private final StringRedisTemplate redisTemplate;

    private static final Map<Integer, String> STATUS_MAP = Map.of(
            1, "待付款", 2, "已付款", 3, "已核销", 4, "已取消", 5, "退款中", 6, "已退款"
    );

    @Override
    public String getOrderDetail(Long orderId, Long userId) {
        // 先查缓存
        String cacheKey = RedisKeyConstants.CACHE_ORDER + orderId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;

        TbVoucherOrder order = orderMapper.selectOne(
                new QueryWrapper<TbVoucherOrder>()
                        .eq("id", orderId)
                        .eq("user_id", userId));
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单不存在或无权限查看");
        }

        String result = JsonUtil.toJson(Map.of(
                "orderId",    order.getId(),
                "voucherId",  order.getVoucherId(),
                "userId",     order.getUserId(),
                "status",     order.getStatus(),
                "statusDesc", STATUS_MAP.getOrDefault(order.getStatus(), "未知"),
                "createTime", order.getCreateTime().toString()
        ));
        // 缓存5分钟
        redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public String listUserOrders(Long userId, Integer page, Integer size) {
        page = page == null || page < 1 ? 1 : page;
        size = size == null || size < 1 ? 10 : Math.min(size, 50);

        // 查缓存
        String cacheKey = RedisKeyConstants.CACHE_ORDER_LIST + userId + ":" + page;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;

        IPage<TbVoucherOrder> pageResult = orderMapper.selectPage(
                new Page<>(page, size),
                new QueryWrapper<TbVoucherOrder>()
                        .eq("user_id", userId)
                        .orderByDesc("create_time"));

        List<Map<String, Object>> list = pageResult.getRecords().stream().map(o -> Map.<String, Object>of(
                "orderId",    o.getId(),
                "voucherId",  o.getVoucherId(),
                "status",     o.getStatus(),
                "statusDesc", STATUS_MAP.getOrDefault(o.getStatus(), "未知"),
                "createTime", o.getCreateTime().toString()
        )).toList();

        String result = JsonUtil.toJson(Map.of(
                "total",   pageResult.getTotal(),
                "pages",   pageResult.getPages(),
                "current", pageResult.getCurrent(),
                "records", list
        ));
        redisTemplate.opsForValue().set(cacheKey, result, 2, TimeUnit.MINUTES);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String cancelOrder(Long orderId, Long userId) {
        TbVoucherOrder order = orderMapper.selectOne(
                new QueryWrapper<TbVoucherOrder>()
                        .eq("id", orderId).eq("user_id", userId));
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单不存在");
        }
        if (order.getStatus() != 1 && order.getStatus() != 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前订单状态不支持取消");
        }

        // 更新订单状态为已取消
        order.setStatus(4);
        orderMapper.updateById(order);

        // 库存回补（Redis INCR）
        String stockKey = RedisKeyConstants.SECKILL_STOCK + order.getVoucherId();
        redisTemplate.opsForValue().increment(stockKey);

        // 清除购买资格（允许重新购买）
        String qualifyKey = RedisKeyConstants.SECKILL_QUALIFY + order.getVoucherId() + ":" + userId;
        redisTemplate.delete(qualifyKey);

        // 清缓存
        redisTemplate.delete(RedisKeyConstants.CACHE_ORDER + orderId);
        redisTemplate.delete(RedisKeyConstants.CACHE_ORDER_LIST + userId + ":1");

        log.info("[Order] 订单取消成功: orderId={}, userId={}, voucherId={}",
                orderId, userId, order.getVoucherId());

        return JsonUtil.toJson(Map.of(
                "success", true,
                "orderId", orderId,
                "message", "订单已取消，库存已回补"
        ));
    }
}
 