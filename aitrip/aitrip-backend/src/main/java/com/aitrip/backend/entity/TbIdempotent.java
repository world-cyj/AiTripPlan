package com.aitrip.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消费端幂等记录表实体
 */
@Data
@TableName("tb_idempotent")
public class TbIdempotent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Kafka 消息ID（幂等键） */
    private String messageId;

    private String topic;

    /** 0=处理中 1=成功 2=失败 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 幂等记录过期时间（默认7天） */
    private LocalDateTime expireTime;
}
