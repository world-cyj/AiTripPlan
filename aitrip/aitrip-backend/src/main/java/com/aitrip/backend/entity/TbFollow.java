package com.aitrip.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注关系表实体
 */
@Data
@TableName("tb_follow")
public class TbFollow {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关注者ID */
    private Long userId;

    /** 被关注者ID */
    private Long followId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
