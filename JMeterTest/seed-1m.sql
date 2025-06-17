-- 百万行压测数据灌入脚本：追加约 99 万行，使 ch_post 总量达到 100 万
-- 幂等键 bulk- 前缀，post_id 从 10 亿起自增（与雪花 ID 区间不冲突），create_time 按秒铺开约 11.5 天
SET SESSION cte_max_recursion_depth = 2000;

INSERT INTO ch_post (post_id, post_user_id, post_title, post_type, post_text,
                     post_idempotency_key, post_view_number, post_is_delete, create_time)
WITH RECURSIVE a(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM a WHERE n < 1000),
                b(n) AS (SELECT 1 UNION ALL SELECT n + 1 FROM b WHERE n < 1000)
SELECT
    1000000000 + (a.n - 1) * 1000 + b.n,
    1,
    CONCAT('压测数据-', (a.n - 1) * 1000 + b.n),
    ELT(1 + ((a.n - 1) * 1000 + b.n) MOD 7, '打听求助', '校园吐槽', '二手闲置', '失物招领', '恋爱交友', '游戏开黑', '其他内容'),
    '批量灌入的压测数据，用于验证数据量增长后的访问路径差异。',
    CONCAT('bulk-', 1000000000 + (a.n - 1) * 1000 + b.n),
    FLOOR(RAND() * 1000),
    0,
    NOW() - INTERVAL ((a.n - 1) * 1000 + b.n) SECOND
FROM a CROSS JOIN b;
