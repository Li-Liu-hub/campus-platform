CREATE TABLE IF NOT EXISTS `ch_log`
(
    `log_id`         BIGINT      NOT NULL COMMENT '日志 ID，雪花算法生成',
    `log_user_id`    BIGINT               COMMENT '此次操作的用户 ID',
    `log_user_ip`    VARCHAR(45) NOT NULL COMMENT '用户请求地址',
    `log_type`       VARCHAR(32) NOT NULL COMMENT '操作类型',
    `log_target_id`  BIGINT               COMMENT '操作目标数据主键',
    `log_status`     TINYINT     NOT NULL DEFAULT 1 COMMENT '操作状态：1 成功，0 失败',
    `log_text`       TEXT                 COMMENT '失败原因或日志说明',
    `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，消费端路径写入事件携带的操作发生时刻',
    `log_cost_time`  INT                  COMMENT '日志耗时（毫秒）',
    PRIMARY KEY (`log_id`),
    -- 消费幂等：重复投递的事件撞唯一键被当作已消费吸收。
    -- 注意 MySQL 唯一索引不约束 NULL，无目标操作（targetId 为 null）的重复事件仍会重复落库，为已知边界
    UNIQUE KEY `uk_log_event` (`log_type`, `log_target_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '日志表';
