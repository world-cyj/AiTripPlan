package com.aitrip.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 订单路由表实体（主库，用于跨分片查询定位订单所在分片）
 */
@Data
@TableName("tb_order_route")
public class TbOrderRoute {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    /** 用户ID（路由依据） */
    private Long userId;

    /** 所在分片：0 或 1 */
    private Integer shard;
}
