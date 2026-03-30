package com.aitrip.backend.mapper;

import com.aitrip.backend.entity.TbOrderRoute;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 订单路由 Mapper
 */
@Mapper
public interface OrderRouteMapper extends BaseMapper<TbOrderRoute> {

    @Select("SELECT shard FROM tb_order_route WHERE order_id = #{orderId} LIMIT 1")
    Integer selectShardByOrderId(Long orderId);
}
