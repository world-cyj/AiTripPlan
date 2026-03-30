package com.aitrip.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 景区实体（与 tb_attraction 表字段一一对应）
 */
@Data
@TableName("tb_attraction")
public class TbAttraction {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 景区名称 */
    private String name;

    /** 所在城市 */
    private String city;

    /** 景区类型 */
    private Integer typeId;

    /** 封面图片 */
    private String images;

    /** 景区描述 */
    private String description;

    /** 门票参考价格 */
    private BigDecimal price;

    /** 评分（0-10） */
    private BigDecimal score;

    /** 售出总量 */
    private Integer sold;

    /** 评论数 */
    private Integer comments;

    /** 详细地址 */
    private String address;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 开放时间 */
    private String openHours;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
