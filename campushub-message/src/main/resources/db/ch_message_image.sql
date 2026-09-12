CREATE TABLE IF NOT EXISTS `ch_message_image`
(
    `message_image_id`         BIGINT       NOT NULL COMMENT '消息图片 ID',
    `message_image_message_id` BIGINT       NOT NULL COMMENT '图片属于哪个消息',
    `message_image_url`        VARCHAR(512) NOT NULL COMMENT '图片地址',
    `message_image_sort`       INT          NOT NULL DEFAULT 0 COMMENT '同消息多图展示顺序，从 0 递增',
    `create_time`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`message_image_id`),
    -- 刻意不设独立幂等键：图片是消息的子资源，重复提交由"消息唯一键先拦、冲突时不插图片"的实现顺序兜住，
    -- 加第二个幂等键反而引入"消息冲突但图片幂等键不同"的错位组合
    INDEX `idx_message_image_message` (`message_image_message_id`, `message_image_sort`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '消息图片表';
