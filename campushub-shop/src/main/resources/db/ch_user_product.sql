CREATE TABLE IF NOT EXISTS `ch_user_product`
(
    `user_product_id` BIGINT   NOT NULL COMMENT '用户购买商品记录 ID',
    `user_id`         BIGINT   NOT NULL COMMENT '购买用户 ID',
    `product_id`      BIGINT   NOT NULL COMMENT '商品 ID',
    `product_number`  BIGINT   NOT NULL COMMENT '购买数量',
    `purchase_time`   DATETIME NOT NULL COMMENT '下单时间',
    `payment_time`    DATETIME COMMENT '付款时间，付款成功时回填',
    `order_status`    TINYINT  NOT NULL DEFAULT 0 COMMENT '购买记录状态：0 待付款，1 已付款，2 已取消',
    `user_product_idempotency_key` VARCHAR(64) NOT NULL COMMENT '下单幂等键，由前端每次提交时生成并传递，防止重复下单导致重复冻结库存',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_product_id`),
    UNIQUE KEY `uk_user_product_idempotency_key` (`user_product_idempotency_key`),
    INDEX `idx_user_id` (`user_id`, `create_time`),
    INDEX `idx_product_id` (`product_id`),
    -- 超时关闭任务扫描必需：现有 idx_user_id、idx_product_id 都无法支撑
    -- WHERE order_status = 0 AND purchase_time < ? ，缺该索引任务一上线就是全表扫。
    -- 用 purchase_time 而非 create_time：前者由应用时钟写入，与任务算出的 deadline 同源，
    -- 不会因数据库容器时区与 JVM 不一致而把新订单误判为超时
    INDEX `idx_status_purchase` (`order_status`, `purchase_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户购买商品表';
