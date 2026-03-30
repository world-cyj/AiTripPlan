-- 分片库建表脚本  userId%2==0->hmdp_0  奇数->hmdp_1

CREATE DATABASE IF NOT EXISTS hmdp_0 DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS hmdp_1 DEFAULT CHARACTER SET utf8mb4;

USE hmdp_0;

CREATE TABLE IF NOT EXISTS `tb_voucher_order` (
  `id`          BIGINT   NOT NULL,
  `user_id`     BIGINT   NOT NULL,
  `voucher_id`  BIGINT   NOT NULL,
  `pay_type`    TINYINT  DEFAULT 1,
  `status`      TINYINT  DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `pay_time`    DATETIME,
  `use_time`    DATETIME,
  `refund_time` DATETIME,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tb_order_route` (
  `id`       BIGINT  NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT  NOT NULL,
  `user_id`  BIGINT  NOT NULL,
  `shard`    TINYINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE hmdp_1;

CREATE TABLE IF NOT EXISTS `tb_voucher_order` (
  `id`          BIGINT   NOT NULL,
  `user_id`     BIGINT   NOT NULL,
  `voucher_id`  BIGINT   NOT NULL,
  `pay_type`    TINYINT  DEFAULT 1,
  `status`      TINYINT  DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `pay_time`    DATETIME,
  `use_time`    DATETIME,
  `refund_time` DATETIME,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tb_order_route` (
  `id`       BIGINT  NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT  NOT NULL,
  `user_id`  BIGINT  NOT NULL,
  `shard`    TINYINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
