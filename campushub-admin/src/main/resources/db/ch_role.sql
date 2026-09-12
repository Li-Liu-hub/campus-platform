CREATE TABLE IF NOT EXISTS `ch_role`
(
    `role_id`          BIGINT       NOT NULL COMMENT '角色 ID',
    `role_code`        VARCHAR(32)  NOT NULL COMMENT '角色编码，与 ch_user.user_role 取值对齐（USER/ADMIN），是角色与用户的关联键',
    `role_name`        VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `role_description` VARCHAR(255)          COMMENT '角色描述',
    `role_is_delete`   TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标识：0 未删除，1 已删除',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`role_id`),
    -- 角色编码是用户与角色之间的唯一关联键（ch_user.user_role 存编码而非 ID），必须唯一
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '角色表';
