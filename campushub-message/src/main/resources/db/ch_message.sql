CREATE TABLE IF NOT EXISTS `ch_message`
(
    `message_id`              BIGINT      NOT NULL COMMENT '消息 ID',
    `message_conversation_id` BIGINT      NOT NULL COMMENT '该消息来自哪个会话',
    `message_send_user_id`    BIGINT      NOT NULL COMMENT '发送者用户 ID',
    `message_receive_user_id` BIGINT      NOT NULL COMMENT '接收者用户 ID',
    `message_text`            TEXT                 COMMENT '消息内容，纯图片消息允许为空',
    `message_is_read`         TINYINT     NOT NULL DEFAULT 0 COMMENT '是否已读：0 未读，1 已读',
    `message_idempotency_key` VARCHAR(64) NOT NULL COMMENT '发送消息幂等键，由前端每次提交时生成并传递，防止重发产生重复消息',
    `create_time`             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`message_id`),
    -- 幂等的硬保障：重复发送撞唯一键，回查原消息返回（与帖子/商品/购买记录同一模式）
    UNIQUE KEY `uk_message_idempotency_key` (`message_idempotency_key`),
    -- 会话内消息游标翻页：按 (会话, 消息 ID) 倒序走索引，随消息量增长成本恒定
    INDEX `idx_conversation_message` (`message_conversation_id`, `message_id`),
    -- 未读拉取与标记已读共用：前两列等值服务"我的未读全集"，
    -- 第三列服务"打开某会话把其中我的未读置已读"（UPDATE 条件三列齐用）
    INDEX `idx_receiver_unread` (`message_receive_user_id`, `message_is_read`, `message_conversation_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '消息表';
