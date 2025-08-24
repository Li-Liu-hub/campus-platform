-- ============================================================================
-- 店铺模块幂等键升级脚本：为三张表补充幂等键字段与唯一索引（2026-09-10）
-- ============================================================================
-- 背景：店铺、商品、下单三个创建接口原先没有防重复提交手段，前端重复点击或网络重试会
--       落下重复数据，下单场景更会重复扣减库存。本次按 ch_post 的既有约定补齐幂等键：
--       前端每次提交生成一个键随请求体传入，应用层直接插入并捕获唯一键冲突，
--       冲突时按「归属者 + 幂等键」回查原记录返回，保证重复提交的响应语义一致。
--
-- 涉及字段：
--       ch_shop.shop_idempotency_key                 唯一索引 uk_shop_idempotency_key
--       ch_product.product_idempotency_key           唯一索引 uk_product_idempotency_key
--       ch_user_product.user_product_idempotency_key 唯一索引 uk_user_product_idempotency_key
--
-- 幂等性说明：MySQL 8.0 的 ADD COLUMN / ADD INDEX 均不支持 IF NOT EXISTS，重复执行会报
--       1060（列已存在）/ 1061（索引已存在），属预期结果，忽略即可。
--       全新环境直接执行 ch_shop.sql / ch_product.sql / ch_user_product.sql 即可，无需本脚本。
--
-- 兼容存量数据：先按可空加列并回填 legacy-<主键> 占位值，再收紧为 NOT NULL 并建唯一索引，
--       旧行不会被唯一索引挡住，也不会因为非空约束而升级失败。
-- ============================================================================

ALTER TABLE `ch_shop`
    ADD COLUMN `shop_idempotency_key` VARCHAR(64) NULL
        COMMENT '店铺幂等键，由前端每次提交时生成并传递，防止重复创建店铺'
        AFTER `shop_description`;

UPDATE `ch_shop`
SET `shop_idempotency_key` = CONCAT('legacy-shop-', `shop_id`)
WHERE `shop_idempotency_key` IS NULL;

ALTER TABLE `ch_shop`
    MODIFY COLUMN `shop_idempotency_key` VARCHAR(64) NOT NULL
        COMMENT '店铺幂等键，由前端每次提交时生成并传递，防止重复创建店铺';

ALTER TABLE `ch_shop`
    ADD UNIQUE KEY `uk_shop_idempotency_key` (`shop_idempotency_key`);

ALTER TABLE `ch_product`
    ADD COLUMN `product_idempotency_key` VARCHAR(64) NULL
        COMMENT '商品幂等键，由前端每次提交时生成并传递，防止重复创建商品'
        AFTER `product_status`;

UPDATE `ch_product`
SET `product_idempotency_key` = CONCAT('legacy-product-', `product_id`)
WHERE `product_idempotency_key` IS NULL;

ALTER TABLE `ch_product`
    MODIFY COLUMN `product_idempotency_key` VARCHAR(64) NOT NULL
        COMMENT '商品幂等键，由前端每次提交时生成并传递，防止重复创建商品';

ALTER TABLE `ch_product`
    ADD UNIQUE KEY `uk_product_idempotency_key` (`product_idempotency_key`);

ALTER TABLE `ch_user_product`
    ADD COLUMN `user_product_idempotency_key` VARCHAR(64) NULL
        COMMENT '下单幂等键，由前端每次提交时生成并传递，防止重复下单导致重复扣库存'
        AFTER `order_status`;

UPDATE `ch_user_product`
SET `user_product_idempotency_key` = CONCAT('legacy-order-', `user_product_id`)
WHERE `user_product_idempotency_key` IS NULL;

ALTER TABLE `ch_user_product`
    MODIFY COLUMN `user_product_idempotency_key` VARCHAR(64) NOT NULL
        COMMENT '下单幂等键，由前端每次提交时生成并传递，防止重复下单导致重复扣库存';

ALTER TABLE `ch_user_product`
    ADD UNIQUE KEY `uk_user_product_idempotency_key` (`user_product_idempotency_key`);
