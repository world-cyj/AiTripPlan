package com.aitrip.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优惠券订单实体（分库分表，按 userId % 2 路由）
 */
@Data
@TableName("tb_voucher_order")
public class TbVoucherOrder {

    /** 订单ID（雪花ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 下单用户ID */
    private Long userId;

    /** 购买的优惠券ID */
    private Long voucherId;

    /**
     * 订单状态
     * 1: 待付款  2: 已付款  3: 已核销  4: 已取消  5: 退款中  6: 已退款
     */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
