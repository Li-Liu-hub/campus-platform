CREATE TABLE IF NOT EXISTS `ch_collect`
(
    `collect_id`      BIGINT   NOT NULL COMMENT '收藏 ID',
    `collect_post_id` BIGINT   NOT NULL COMMENT '收藏帖子 ID',
    `collect_user_id` BIGINT   NOT NULL COMMENT '收藏用户 ID',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`collect_id`),
    UNIQUE KEY `uk_collect_post_user` (`collect_post_id`, `collect_user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '帖子收藏表';
