CREATE TABLE IF NOT EXISTS `ch_post_image`
(
    `post_image_id`      BIGINT       NOT NULL COMMENT '帖子图片 ID',
    `post_image_post_id` BIGINT       NOT NULL COMMENT '图片所属帖子 ID',
    `post_image_url`     VARCHAR(512) NOT NULL COMMENT '图片地址',
    `post_image_sort`    INT          NOT NULL DEFAULT 0 COMMENT '同帖多图展示顺序，从 0 递增',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`post_image_id`),
    -- 按 (帖子, 顺序) 取图，建帖详情组装时无需二次排序
    INDEX `idx_post_image_post` (`post_image_post_id`, `post_image_sort`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '帖子图片表';
