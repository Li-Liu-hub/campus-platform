CREATE TABLE IF NOT EXISTS `ch_user_product`
(
    `user_product_id` BIGINT   NOT NULL COMMENT '用户购买商品记录 ID',
    `user_id`         BIGINT   NOT NULL COMMENT '购买用户 ID',
    `product_id`      BIGINT   NOT NULL COMMENT '商品 ID',
    `product_number`  BIGINT   NOT NULL COMMENT '购买数量',
    `purchase_time`   DATETIME NOT NULL COMMENT '下单时间',
    `payment_time`    DATETIME COMMENT '预约付款时间',
    `order_status`    TINYINT  NOT NULL DEFAULT 0 COMMENT '订单状态：0 待付款，1 已付款，2 已取消',
    `user_product_idempotency_key` VARCHAR(64) NOT NULL COMMENT '下单幂等键，由前端每次提交时生成并传递，防止重复下单导致重复扣库存',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_product_id`),
    UNIQUE KEY `uk_user_product_idempotency_key` (`user_product_idempotency_key`),
    INDEX `idx_user_id` (`user_id`, `create_time`),
    INDEX `idx_product_id` (`product_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户购买商品表';
