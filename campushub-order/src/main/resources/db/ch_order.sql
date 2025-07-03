CREATE TABLE IF NOT EXISTS `ch_order`
(
    `order_id`        BIGINT        NOT NULL COMMENT '订单 ID',
    `order_user_id`   BIGINT        NOT NULL COMMENT '下单用户 ID',
    `order_title`     VARCHAR(128)  NOT NULL COMMENT '订单标题',
    `order_amount`    DECIMAL(10,2) NOT NULL COMMENT '订单金额，保留两位小数',
    `order_status`    TINYINT       NOT NULL DEFAULT 0 COMMENT '订单状态：0 待支付，1 已支付，2 已完成，3 已取消',
    `order_is_delete` TINYINT       NOT NULL DEFAULT 0 COMMENT '软删除标识：0 未删除，1 已删除',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`order_id`),
    INDEX `idx_delete_time` (`order_is_delete`, `create_time`),
    INDEX `idx_status_time` (`order_is_delete`, `order_status`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '订单表';
