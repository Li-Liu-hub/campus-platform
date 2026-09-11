-- ============================================================================
-- ch_outbox 升级脚本：新增 next_retry_time 与配套索引（2026-09-10）
-- ============================================================================
-- 背景：轮询中继原先固定按 id 升序取批次，一条永远 confirm 不了的消息会永久停在
--       status=0 并占据批次头部，积压超过批大小后后续消息永远发不出去（队头阻塞）。
--       新增 next_retry_time 后，中继改按「已到期」筛选 + 按到期时刻排序，
--       失败消息在退避窗口内不进入候选集，不再阻塞后面的消息。
--
-- 幂等性说明：MySQL 8.0 的 ADD COLUMN / ADD INDEX / DROP INDEX 均不支持 IF NOT EXISTS，
--       重复执行会报 1060（列已存在）/ 1091（索引不存在）/ 1061（索引已存在），
--       属预期结果，忽略即可。全新环境直接执行 ch_outbox.sql 即可，无需本脚本。
-- ============================================================================

ALTER TABLE `ch_outbox`
    ADD COLUMN `next_retry_time` DATETIME NULL
        COMMENT '下次允许重发时刻，按重试次数指数退避；NULL 表示立即可发'
        AFTER `retry_count`;

ALTER TABLE `ch_outbox`
    DROP INDEX `idx_status_create`;

ALTER TABLE `ch_outbox`
    ADD INDEX `idx_status_next_retry` (`status`, `next_retry_time`);

ALTER TABLE `ch_outbox`
    ADD INDEX `idx_status_confirm` (`status`, `confirm_time`);
