CREATE TABLE IF NOT EXISTS `ch_post_view`
(
    `view_id`      BIGINT   NOT NULL COMMENT '浏览明细 ID，雪花算法生成',
    `post_id`      BIGINT   NOT NULL COMMENT '帖子 ID',
    `view_user_id` BIGINT   NULL COMMENT '浏览用户 ID，当前需登录浏览，预留匿名浏览为空',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    PRIMARY KEY (`view_id`),
    INDEX `idx_post_time` (`post_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '帖子浏览明细表：每次浏览一行，聚合统计仍在 ch_post.post_view_number';
