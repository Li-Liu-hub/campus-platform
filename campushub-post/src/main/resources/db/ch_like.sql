CREATE TABLE IF NOT EXISTS `ch_like`
(
    `like_id`      BIGINT   NOT NULL COMMENT '点赞 ID',
    `like_post_id` BIGINT   NOT NULL COMMENT '点赞帖子 ID',
    `like_user_id` BIGINT   NOT NULL COMMENT '点赞用户 ID',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`like_id`),
    UNIQUE KEY `uk_like_post_user` (`like_post_id`, `like_user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '帖子点赞表';
