package com.aitrip.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_outbox")
public class TbOutbox {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 全局唯一消息ID（UUID，用于幂等） */
    private String messageId;

    private String topic;
    private String payload;

    /** 0=待发送 1=已发送 2=失败 */
    private Integer status;
    private Integer retryCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private LocalDateTime sendTime;
    private LocalDateTime nextRetryTime;
    private String lastError;
}
