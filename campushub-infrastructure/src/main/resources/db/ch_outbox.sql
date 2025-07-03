CREATE TABLE IF NOT EXISTS `ch_outbox`
(
    `id`           BIGINT       NOT NULL COMMENT '主键，雪花算法生成',
    `exchange`     VARCHAR(64)  NOT NULL COMMENT '目标交换机',
    `routing_key`  VARCHAR(128) NOT NULL COMMENT '路由键',
    `biz_id`       VARCHAR(128)          COMMENT '业务标识，confirm 失败或人工排查时定位业务事件',
    `payload`      TEXT         NOT NULL COMMENT '消息体 JSON 文本',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '发布状态：0 待确认（未确认即轮询重发），1 broker 已确认',
    `retry_count`  INT          NOT NULL DEFAULT 0 COMMENT '轮询中继累计尝试发送次数',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，与业务数据同事务写入保证原子性',
    `confirm_time` DATETIME              COMMENT 'broker 确认时间，清理任务的判断依据',
    PRIMARY KEY (`id`),
    KEY `idx_status_create` (`status`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'MQ 发件箱：高价值消息在业务事务内写入，轮询中继发送，confirm 后标记，保留期清理';
