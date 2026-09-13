CREATE TABLE IF NOT EXISTS `ch_follow`
(
    `follow_id`              BIGINT   NOT NULL COMMENT '关注 ID',
    `follow_sent_user_id`    BIGINT   NOT NULL COMMENT '发出关注的用户 ID',
    `follow_receive_user_id` BIGINT   NOT NULL COMMENT '被关注的用户 ID',
    `create_time`            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`follow_id`),
    -- 重复关注幂等：同一用户对同一目标至多一条关注关系，撞键即“已关注”
    UNIQUE KEY `uk_follow_sent_receive` (`follow_sent_user_id`, `follow_receive_user_id`),
    -- 粉丝列表按被关注维度查询：唯一键前缀只覆盖关注者维度，被关注维度需要本索引
    INDEX `idx_follow_receive` (`follow_receive_user_id`, `follow_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '用户关注表';
