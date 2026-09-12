CREATE TABLE IF NOT EXISTS `ch_product`
(
    `product_id`        BIGINT         NOT NULL COMMENT '商品 ID',
    `product_shop_id`   BIGINT         NOT NULL COMMENT '所属店铺 ID',
    `product_name`      VARCHAR(128)   NOT NULL COMMENT '商品名称',
    `product_price`     DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
    `product_stock`     BIGINT         NOT NULL DEFAULT 0 COMMENT '可卖库存，下单冻结时减少、取消或超时后回补、付款后不再回补',
    `product_stock_locked` BIGINT      NOT NULL DEFAULT 0 COMMENT '冻结库存，已下单未付款占用中，付款后消耗',
    `product_status`    TINYINT        NOT NULL DEFAULT 1 COMMENT '上架状态：1 上架，0 下架',
    `product_idempotency_key` VARCHAR(64) NOT NULL COMMENT '商品幂等键，由前端每次提交时生成并传递，防止重复创建商品',
    `product_is_delete` TINYINT        NOT NULL DEFAULT 0 COMMENT '软删除标识：0 未删除，1 已删除',
    `create_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`product_id`),
    UNIQUE KEY `uk_product_idempotency_key` (`product_idempotency_key`),
    INDEX `idx_shop_status` (`product_shop_id`, `product_status`),
    INDEX `idx_delete_time` (`product_is_delete`, `create_time`),
    -- 库存非负兜底：应用层已用条件更新保证不为负，这里再加一道数据库级防线，
    -- 防止后续新增代码漏写 WHERE 条件时把库存扣成负数；两列都不可为负
    CONSTRAINT `ck_product_stock_not_negative`
        CHECK (`product_stock` >= 0 AND `product_stock_locked` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '商品表';
