CREATE TABLE IF NOT EXISTS `ch_order`
(
    `order_id`              BIGINT        NOT NULL COMMENT '订单 ID',
    `order_sent_user_id`    BIGINT        NOT NULL COMMENT '发布订单用户 ID',
    `order_receive_user_id` BIGINT        NULL COMMENT '接单用户 ID，抢单成功后回填',
    `order_idempotency_key` VARCHAR(64)   NOT NULL COMMENT '发布订单幂等键',
    `order_amount`          DECIMAL(10,2) NOT NULL COMMENT '订单金额，保留两位小数',
    `order_status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '订单状态：0 待接单，1 已接单，2 已完成，3 已取消',
    `order_timeout`         DATETIME      NOT NULL COMMENT '订单最晚支付时间',
    `order_type`            VARCHAR(32)   NOT NULL COMMENT '订单类型',
    `order_view_number`     BIGINT        NOT NULL DEFAULT 0 COMMENT '订单浏览量',
    `order_is_delete`       TINYINT       NOT NULL DEFAULT 0 COMMENT '软删除标识：0 未删除，1 已删除',
    `create_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`order_id`),
    UNIQUE KEY `uk_order_idempotency_key` (`order_sent_user_id`, `order_idempotency_key`),
    INDEX `idx_sent_delete_time` (`order_sent_user_id`, `order_is_delete`, `create_time`),
    INDEX `idx_receive_delete_time` (`order_receive_user_id`, `order_is_delete`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '订单表';
