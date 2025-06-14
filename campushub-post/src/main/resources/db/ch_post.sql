CREATE TABLE IF NOT EXISTS `ch_post`
(
    `post_id`              BIGINT       NOT NULL COMMENT '帖子 ID',
    `post_user_id`         BIGINT       NOT NULL COMMENT '用户 ID',
    `post_title`           VARCHAR(128) NOT NULL COMMENT '帖子标题',
    `post_type`            VARCHAR(32)  NOT NULL COMMENT '帖子类型',
    `post_text`            TEXT         NOT NULL COMMENT '帖子内容',
    `post_idempotency_key` VARCHAR(64)  NOT NULL COMMENT '帖子幂等键',
    `post_view_number`     BIGINT       NOT NULL DEFAULT 0 COMMENT '帖子浏览量',
    `post_is_delete`       TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标识：0 未删除，1 已删除',
    `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`post_id`),
    UNIQUE KEY `uk_post_idempotency_key` (`post_idempotency_key`),
    INDEX `idx_delete_time` (`post_is_delete`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '帖子表';
