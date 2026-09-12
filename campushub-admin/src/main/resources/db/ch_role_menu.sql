CREATE TABLE IF NOT EXISTS `ch_role_menu`
(
    `role_menu_id`      BIGINT   NOT NULL COMMENT '角色菜单关联 ID',
    `role_menu_role_id` BIGINT   NOT NULL COMMENT '角色 ID',
    `role_menu_menu_id` BIGINT   NOT NULL COMMENT '菜单 ID',
    `create_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`role_menu_id`),
    -- 授权幂等：同一角色重复分配同一菜单撞唯一键，由服务层吸收为"已授权"
    UNIQUE KEY `uk_role_menu` (`role_menu_role_id`, `role_menu_menu_id`),
    -- 反查「哪些角色拥有该菜单」：唯一键前缀只覆盖角色维度，菜单维度需要本索引
    INDEX `idx_role_menu_menu` (`role_menu_menu_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '角色菜单关联表：角色与菜单的授权关系，前端按角色查菜单树的数据出口';
