CREATE TABLE IF NOT EXISTS `ch_post_comment`
(
    `comment_id`              BIGINT        NOT NULL COMMENT '评论 ID',
    `comment_post_id`         BIGINT        NOT NULL COMMENT '所属帖子 ID',
    `comment_sent_user_id`    BIGINT        NOT NULL COMMENT '评论者用户 ID',
    `comment_father_id`       BIGINT        NOT NULL DEFAULT 0 COMMENT '父评论 ID：0 表示一级评论，否则为被回复的一级评论 ID（二级回复平铺挂在一级下）',
    `comment_receive_user_id` BIGINT        NOT NULL COMMENT '被回复的用户 ID：一级评论为帖子作者，二级回复为被回复评论的作者',
    `comment_text`            VARCHAR(1000) NOT NULL COMMENT '评论内容',
    `comment_idempotency_key` VARCHAR(64)   NOT NULL COMMENT '评论幂等键，由前端每次提交时生成并传递',
    `comment_is_delete`       TINYINT       NOT NULL DEFAULT 0 COMMENT '软删除标识：0 未删除，1 已删除',
    `create_time`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`comment_id`),
    -- 幂等的硬保障：重复提交撞唯一键，回查原评论返回（与帖子/订单同一模式）
    UNIQUE KEY `uk_comment_idempotency_key` (`comment_idempotency_key`),
    -- 帖子维度取一级评论分页：WHERE post_id = ? AND father_id = 0 ORDER BY (create_time, id)
    INDEX `idx_comment_post_father` (`comment_post_id`, `comment_father_id`, `comment_id`),
    -- 二级回复批量取：WHERE father_id IN (...)（联合索引前缀不含 father 单列，需独立索引）
    INDEX `idx_comment_father` (`comment_father_id`, `comment_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '帖子评论表：一级评论与二级回复共存，father_id 区分层级';
