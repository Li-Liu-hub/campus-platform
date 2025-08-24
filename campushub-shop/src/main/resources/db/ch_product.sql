CREATE TABLE IF NOT EXISTS `ch_product`
(
    `product_id`        BIGINT         NOT NULL COMMENT '商品 ID',
    `product_shop_id`   BIGINT         NOT NULL COMMENT '所属店铺 ID',
    `product_name`      VARCHAR(128)   NOT NULL COMMENT '商品名称',
    `product_price`     DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
    `product_stock`     BIGINT         NOT NULL DEFAULT 0 COMMENT '商品库存',
    `product_status`    TINYINT        NOT NULL DEFAULT 1 COMMENT '上架状态：1 上架，0 下架',
    `product_idempotency_key` VARCHAR(64) NOT NULL COMMENT '商品幂等键，由前端每次提交时生成并传递，防止重复创建商品',
    `product_is_delete` TINYINT        NOT NULL DEFAULT 0 COMMENT '软删除标识：0 未删除，1 已删除',
    `create_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`product_id`),
    UNIQUE KEY `uk_product_idempotency_key` (`product_idempotency_key`),
    INDEX `idx_shop_status` (`product_shop_id`, `product_status`),
    INDEX `idx_delete_time` (`product_is_delete`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '商品表';
