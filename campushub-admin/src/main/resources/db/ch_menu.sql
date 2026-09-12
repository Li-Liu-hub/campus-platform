CREATE TABLE IF NOT EXISTS `ch_menu`
(
    `menu_id`         BIGINT       NOT NULL COMMENT '菜单 ID',
    `menu_parent_id`  BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单 ID，0 表示根节点，自关联成树',
    `menu_name`       VARCHAR(64)  NOT NULL COMMENT '菜单名称，前端路由显示标题',
    `menu_path`       VARCHAR(128) NOT NULL COMMENT '路由路径，如 /admin/users',
    `menu_component`  VARCHAR(128)          COMMENT '前端组件路径，如 admin/user/index',
    `menu_icon`       VARCHAR(64)           COMMENT '菜单图标',
    `menu_sort`       INT          NOT NULL DEFAULT 0 COMMENT '同级排序，升序',
    `menu_visible`    TINYINT      NOT NULL DEFAULT 1 COMMENT '是否在菜单中显示：1 显示，0 隐藏（隐藏节点仍参与路由注册）',
    `menu_permission` VARCHAR(64)           COMMENT '权限标识，如 admin:user:list，为按钮级权限校验预留',
    `menu_is_delete`  TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标识：0 未删除，1 已删除',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`menu_id`),
    -- 菜单树按「父 + 同级排序」一次取出，建树时不再排序
    INDEX `idx_parent_sort` (`menu_parent_id`, `menu_sort`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '菜单表：前端动态路由的数据源，按角色经 ch_role_menu 授权';
