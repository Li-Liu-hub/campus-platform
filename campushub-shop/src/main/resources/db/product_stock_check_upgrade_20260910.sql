-- ============================================================================
-- 商品库存非负 CHECK 约束升级脚本（2026-09-10）
-- ============================================================================
-- 背景：应用层已经用条件更新（WHERE product_stock >= n / product_stock + delta >= 0）
--       保证库存不会被扣成负数，但这是「约定」，不是「约束」。后续任何人新写一条
--       忘记带 WHERE 条件的 UPDATE，库存就会变成负数，且不会有任何报错。
--       这里在数据库层补一道兜底：CHECK (product_stock >= 0)，越界直接报错回滚。
--
-- 注意：MySQL 8.0.16 起才真正强制执行 CHECK 约束（8.0.15 及以下会被解析但忽略）。
--       执行前可用 SELECT VERSION(); 确认版本。
--
-- 幂等性说明：MySQL 不支持 ADD CONSTRAINT ... IF NOT EXISTS，重复执行会报
--       3822（Duplicate check constraint name），属预期结果，忽略即可。
--       全新环境直接执行 ch_product.sql 即可，无需本脚本。
--
-- 存量数据：若表内已存在负库存的脏数据，加约束会失败（报 3819）。执行前先自查：
--       SELECT product_id, product_stock FROM ch_product WHERE product_stock < 0;
-- ============================================================================

ALTER TABLE `ch_product`
    ADD CONSTRAINT `ck_product_stock_not_negative` CHECK (`product_stock` >= 0);
