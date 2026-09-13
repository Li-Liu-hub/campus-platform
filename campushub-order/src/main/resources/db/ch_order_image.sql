CREATE TABLE IF NOT EXISTS `ch_order_image`
(
    `order_image_id`      BIGINT       NOT NULL COMMENT '订单图片 ID',
    `order_image_order_id` BIGINT      NOT NULL COMMENT '图片所属订单 ID',
    `order_image_url`     VARCHAR(512) NOT NULL COMMENT '图片地址',
    `order_image_sort`    INT          NOT NULL DEFAULT 0 COMMENT '同单多图展示顺序，从 0 递增',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`order_image_id`),
    -- 按 (订单, 顺序) 取图，订单详情组装时无需二次排序
    INDEX `idx_order_image_order` (`order_image_order_id`, `order_image_sort`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '订单图片表';
