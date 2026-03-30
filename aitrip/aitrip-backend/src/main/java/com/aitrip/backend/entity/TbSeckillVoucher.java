package com.aitrip.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀券实体
 */
@Data
@TableName("tb_seckill_voucher")
public class TbSeckillVoucher {

    /** 优惠券ID（与 tb_voucher 同 ID） */
    @TableId(type = IdType.INPUT)
    private Long voucherId;

    /** 初始库存 */
    private Integer stock;

    /** 已售数量（冗余，辅助统计） */
    private Integer sold;

    /** 秒杀开始时间 */
    private LocalDateTime beginTime;

    /** 秒杀结束时间 */
    private LocalDateTime endTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
