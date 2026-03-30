package com.aitrip.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 游记/笔记表实体
 */
@Data
@TableName("tb_blog")
public class TbBlog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long attractionId;

    private Long userId;

    private String title;

    /** 图片URL列表（逗号分隔） */
    private String images;

    private String content;

    private Integer liked;

    private Integer comments;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 非DB字段：作者信息 */
    @TableField(exist = false)
    private String authorName;

    @TableField(exist = false)
    private String authorIcon;

    /** 非DB字段：当前用户是否已点赞 */
    @TableField(exist = false)
    private Boolean isLiked;
}
