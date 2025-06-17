CREATE TABLE IF NOT EXISTS `ch_log`
(
    `log_id`       BIGINT      NOT NULL COMMENT '日志 ID',
    `log_user_id`  BIGINT               COMMENT '此次操作的用户 ID',
    `log_user_ip`  VARCHAR(45) NOT NULL COMMENT '用户请求地址',
    `log_type`     VARCHAR(32) NOT NULL COMMENT '操作类型',
    `log_table_id` BIGINT               COMMENT '创建或修改的表主键',
    `log_status`   TINYINT     NOT NULL DEFAULT 1 COMMENT '操作状态：1 成功，0 失败',
    `log_text`     TEXT                 COMMENT '失败原因或日志说明',
    `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`log_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '日志表';
