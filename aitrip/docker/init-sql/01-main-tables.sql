-- ============================================================
-- 01-main-tables.sql  主库建表脚本（aitrip 数据库）
-- ============================================================

CREATE DATABASE IF NOT EXISTS aitrip
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE aitrip;

-- 用户表
CREATE TABLE IF NOT EXISTS `tb_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID（雪花算法）',
  `phone`       VARCHAR(11)  NOT NULL COMMENT '手机号',
  `password`    VARCHAR(128) DEFAULT '' COMMENT '密码（BCrypt）',
  `nick_name`   VARCHAR(32)  DEFAULT '用户' COMMENT '昵称',
  `icon`        VARCHAR(255) DEFAULT '' COMMENT '头像URL',
  `level`       TINYINT      DEFAULT 1 COMMENT '1=普通 2=VIP 3=SVIP',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 景区信息表
CREATE TABLE IF NOT EXISTS `tb_attraction` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '景区ID',
  `name`        VARCHAR(128)  NOT NULL COMMENT '景区名称',
  `city`        VARCHAR(64)   NOT NULL COMMENT '所在城市',
  `type_id`     INT           NOT NULL DEFAULT 0 COMMENT '景区类型',
  `images`      VARCHAR(1024) DEFAULT '' COMMENT '封面图片',
  `description` TEXT          COMMENT '景区描述',
  `price`       DECIMAL(10,2) DEFAULT 0 COMMENT '门票参考价格',
  `score`       DECIMAL(3,1)  DEFAULT 0 COMMENT '评分（0-10）',
  `sold`        INT           DEFAULT 0 COMMENT '售出总量',
  `comments`    INT           DEFAULT 0 COMMENT '评论数',
  `address`     VARCHAR(255)  DEFAULT '' COMMENT '详细地址',
  `longitude`   DECIMAL(10,7) COMMENT '经度',
  `latitude`    DECIMAL(10,7) COMMENT '纬度',
  `open_hours`  VARCHAR(64)   DEFAULT '' COMMENT '开放时间',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_city_type` (`city`, `type_id`),
  FULLTEXT KEY `ft_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='景区信息表';

-- 优惠券/门票母表
CREATE TABLE IF NOT EXISTS `tb_voucher` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
  `attraction_id` BIGINT        NOT NULL COMMENT '关联景区ID',
  `title`         VARCHAR(255)  NOT NULL COMMENT '优惠券标题',
  `sub_title`     VARCHAR(255)  DEFAULT '' COMMENT '副标题',
  `rules`         VARCHAR(1024) DEFAULT '' COMMENT '使用规则',
  `pay_value`     DECIMAL(10,2) NOT NULL COMMENT '支付价格',
  `actual_value`  DECIMAL(10,2) NOT NULL COMMENT '原价',
  `type`          TINYINT       NOT NULL DEFAULT 0 COMMENT '0=普通券 1=秒杀券',
  `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '1=未发布 2=已发布 3=已过期',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_attraction_type` (`attraction_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券/门票母表';

-- 秒杀券扩展表
CREATE TABLE IF NOT EXISTS `tb_seckill_voucher` (
  `voucher_id`  BIGINT   NOT NULL COMMENT '关联 tb_voucher.id',
  `stock`       INT      NOT NULL COMMENT '库存数量',
  `begin_time`  DATETIME NOT NULL COMMENT '秒杀开始时间',
  `end_time`    DATETIME NOT NULL COMMENT '秒杀结束时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀券库存表';

-- Outbox 本地消息表
CREATE TABLE IF NOT EXISTS `tb_outbox` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `message_id`      VARCHAR(64)  NOT NULL COMMENT '全局唯一消息ID（UUID）',
  `topic`           VARCHAR(128) NOT NULL COMMENT 'Kafka Topic',
  `payload`         TEXT         NOT NULL COMMENT 'JSON 消息体',
  `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待发送 1=已发送 2=失败',
  `retry_count`     INT          DEFAULT 0 COMMENT '重试次数',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `send_time`       DATETIME     COMMENT '实际发送时间',
  `next_retry_time` DATETIME     COMMENT '下次重试时间（指数退避）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_id` (`message_id`),
  KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Outbox 本地消息表';

-- 消费端幂等表
CREATE TABLE IF NOT EXISTS `tb_idempotent` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `message_id`  VARCHAR(64)  NOT NULL COMMENT 'Kafka 消息ID',
  `topic`       VARCHAR(128) NOT NULL COMMENT 'Topic',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0=处理中 1=成功 2=失败',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expire_time` DATETIME     NOT NULL COMMENT '幂等记录过期时间（默认7天）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_msg_topic` (`message_id`, `topic`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费端幂等记录表';

-- 关注关系表
CREATE TABLE IF NOT EXISTS `tb_follow` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT   NOT NULL COMMENT '关注者ID',
  `follow_id`   BIGINT   NOT NULL COMMENT '被关注者ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow` (`user_id`, `follow_id`),
  KEY `idx_follow_id` (`follow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注关系表';

-- 游记/笔记表
CREATE TABLE IF NOT EXISTS `tb_blog` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `attraction_id` BIGINT        DEFAULT 0 COMMENT '关联景区（可选）',
  `user_id`       BIGINT        NOT NULL COMMENT '作者ID',
  `title`         VARCHAR(255)  NOT NULL COMMENT '标题',
  `images`        VARCHAR(2048) NOT NULL COMMENT '图片URL列表（逗号分隔）',
  `content`       TEXT          NOT NULL COMMENT '正文',
  `liked`         INT           DEFAULT 0 COMMENT '点赞数',
  `comments`      INT           DEFAULT 0 COMMENT '评论数',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_attraction_id` (`attraction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游记/笔记表';
