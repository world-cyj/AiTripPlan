package com.aitrip.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券/门票母表实体
 */
@Data
@TableName("tb_voucher")
public class TbVoucher {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long attractionId;

    private String title;

    private String subTitle;

    private String rules;

    private BigDecimal payValue;

    private BigDecimal actualValue;

    /** 0=普通券 1=秒杀券 */
    private Integer type;

    /** 1=未发布 2=已发布 3=已过期 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 秒杀扩展信息（非DB字段，查询时关联填充） */
    @TableField(exist = false)
    private TbSeckillVoucher seckillVoucher;
}
