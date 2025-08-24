CREATE TABLE IF NOT EXISTS `ch_shop`
(
    `shop_id`          BIGINT       NOT NULL COMMENT '店铺 ID',
    `shop_user_id`     BIGINT       NOT NULL COMMENT '店主用户 ID',
    `shop_name`        VARCHAR(64)  NOT NULL COMMENT '店铺名称',
    `shop_description` VARCHAR(255) COMMENT '店铺简介',
    `shop_idempotency_key` VARCHAR(64) NOT NULL COMMENT '店铺幂等键，由前端每次提交时生成并传递，防止重复创建店铺',
    `shop_is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标识：0 未删除，1 已删除',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`shop_id`),
    UNIQUE KEY `uk_shop_idempotency_key` (`shop_idempotency_key`),
    INDEX `idx_shop_user_id` (`shop_user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '店铺表';
