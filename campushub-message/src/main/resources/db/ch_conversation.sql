CREATE TABLE IF NOT EXISTS `ch_conversation`
(
    `conversation_id`              BIGINT      NOT NULL COMMENT '会话 ID',
    `conversation_user_a_id`       BIGINT      NOT NULL COMMENT '会话参与者 A：恒取双方用户 ID 的较小值',
    `conversation_user_b_id`       BIGINT      NOT NULL COMMENT '会话参与者 B：恒取双方用户 ID 的较大值',
    `conversation_title`           VARCHAR(64)          COMMENT '会话标题，两人会话可空',
    `conversation_idempotency_key` VARCHAR(64) NOT NULL COMMENT '建会话幂等键，由前端每次提交时生成并传递',
    `create_time`                  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`                  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`conversation_id`),
    -- 一对用户之间至多一个会话：双列已规范化（小 ID 在前、大 ID 在后），
    -- 任何方向、任何幂等键的重复创建都会被同一个联合唯一键拦死
    UNIQUE KEY `uk_conversation_users` (`conversation_user_a_id`, `conversation_user_b_id`),
    -- 幂等键兜底：同一请求重放时冲突，回查原会话直接返回
    UNIQUE KEY `uk_conversation_idempotency_key` (`conversation_idempotency_key`),
    -- 查询"我参与的会话"按 A 或 B 过滤：A 侧走联合唯一键前缀，B 侧需要本索引（避免 OR 条件全表扫）
    INDEX `idx_conversation_user_b` (`conversation_user_b_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '会话表';
