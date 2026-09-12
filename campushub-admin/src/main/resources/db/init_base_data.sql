-- RBAC 基础数据初始化脚本：角色、菜单与授权关系（幂等：先删后插，可重复执行）
-- 执行时机：在 ch_role.sql / ch_menu.sql / ch_role_menu.sql 建表之后执行。
-- 语义：重放即把这三张表的基础数据重置为脚本定义；ID 使用固定小值，不占用雪花 ID 段，
--       用户自行扩展的菜单（非本脚本 ID）不受影响。
-- 作用：菜单数据是前端动态路由的数据源，角色授权决定各角色可见的路由，
--       缺少基础数据时 /api/v1/menus/my 返回空、前端无路由可注册。

DELETE FROM `ch_role_menu` WHERE `role_menu_id` IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
DELETE FROM `ch_menu` WHERE `menu_id` IN (1, 2, 10, 11, 12, 20, 21, 22, 23);
DELETE FROM `ch_role` WHERE `role_id` IN (1, 2);

-- 角色：编码与 ch_user.user_role、RoleCodes 常量三方对齐
INSERT INTO `ch_role` (`role_id`, `role_code`, `role_name`, `role_description`)
VALUES (1, 'USER', '普通用户', '平台注册用户，默认角色'),
       (2, 'ADMIN', '管理员', '平台管理员，可查看与审核全平台数据');

-- 菜单：两级树（用户端 / 管理后台），path 与 component 为前端路由契约
INSERT INTO `ch_menu` (`menu_id`, `menu_parent_id`, `menu_name`, `menu_path`, `menu_component`, `menu_icon`,
                       `menu_sort`, `menu_visible`, `menu_permission`)
VALUES (1, 0, '用户端', '/', NULL, 'HomeFilled', 1, 1, NULL),
       (10, 1, '帖子广场', '/posts', 'post/list', 'Document', 1, 1, 'post:list'),
       (11, 1, '我的店铺', '/shops', 'shop/my', 'Shop', 2, 1, 'shop:my'),
       (12, 1, '消息中心', '/messages', 'message/list', 'ChatDotRound', 3, 1, 'message:list'),
       (2, 0, '管理后台', '/admin', NULL, 'Setting', 2, 1, NULL),
       (20, 2, '用户管理', '/admin/users', 'admin/user/list', 'User', 1, 1, 'admin:user:list'),
       (21, 2, '帖子管理', '/admin/posts', 'admin/post/list', 'Document', 2, 1, 'admin:post:list'),
       (22, 2, '店铺管理', '/admin/shops', 'admin/shop/list', 'Shop', 3, 1, 'admin:shop:list'),
       (23, 2, '商品管理', '/admin/products', 'admin/product/list', 'Goods', 4, 1, 'admin:product:list');

-- 授权：USER 仅用户端子树；ADMIN 含用户端与管理后台全部菜单
INSERT INTO `ch_role_menu` (`role_menu_id`, `role_menu_role_id`, `role_menu_menu_id`)
VALUES (1, 1, 1), (2, 1, 10), (3, 1, 11), (4, 1, 12),
       (5, 2, 1), (6, 2, 2), (7, 2, 10), (8, 2, 11), (9, 2, 12),
       (10, 2, 20), (11, 2, 21), (12, 2, 22), (13, 2, 23);
