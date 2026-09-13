CREATE TABLE IF NOT EXISTS `ch_notification`
(
    `notification_id`      BIGINT       NOT NULL COMMENT '通知 ID',
    `notification_user_id` BIGINT       NOT NULL COMMENT '接收通知的用户 ID',
    `notification_type`    VARCHAR(32)  NOT NULL COMMENT '通知类型：FOLLOW / LIKE / COLLECT 等',
    `notification_text`    VARCHAR(255) NOT NULL COMMENT '通知内容',
    `notification_is_read` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0 未读，1 已读',
    `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`notification_id`),
    -- 列表翻页（用户 + 通知 ID 倒序）与未读统计（用户 + 未读过滤）共用一条索引
    INDEX `idx_notification_user_read` (`notification_user_id`, `notification_is_read`, `notification_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '通知表';
