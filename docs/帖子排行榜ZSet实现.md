# 帖子排行榜 ZSet 实现设计

> 定位：帖子模块第四个 Redis 结构。榜单是业务行为的**下游衍生品**——业务接口不感知它的存在，
> 由浏览 flush 任务与互动 MQ 消费者在落库成功后统一驱动，加法模型累加，永不重算总分。

## 1. 与既有结构的分工

| 键 | 类型 | 职责 | TTL |
|---|---|---|---|
| `campushub:post:info:{id}` | String | 详情缓存（含空值双态） | 300s+random(0~120)s / 空值 60s |
| `campushub:post:view:delta` | Hash | 浏览增量记账 | 禁止（数据本体） |
| `campushub:post:{like\|collect}:members:{postId}` | Set | 点赞/收藏判重标记（member=userId） | 7 天（DB 唯一键兜底的副本） |
| `campushub:rank:{yyyyMMdd}` | ZSet | 当日热度榜（member=postId, score=热度分） | 7 天（可再生的展示数据） |
| `campushub:rank:week` | ZSet | 周榜（ZUNIONSTORE 合成） | 短 TTL（见 §6） |

TTL 判据贯穿：**副本设 TTL 兜底、原始增量禁 TTL、可再生展示数据随意**。
日榜键丢了不疼（交互还在发生，新榜自然重建），所以敢给 7 天 TTL。

## 2. 为什么是 ZSet

member 唯一 + 按 score 有序，排行榜三个产品需求各对应一条原生命令：

```text
ZINCRBY   key 3 "103"        加分（member 不存在自动创建，原子累加）
ZREVRANGE key 0 9 WITHSCORES  Top10（按分倒序）
ZREVRANK  key "103"          某帖当前第几名
```

全部 O(logN)，插入/加分/查排名无锁竞争。"先查分数再写回"这类多步操作一律禁止——
凡"查了再改"的地方都该有一条原子原语，这里就是 ZINCRBY。

## 3. 分数模型：加权增量（加法）

```text
score 增量 = 浏览 +1 × delta + 点赞 +3 × delta + 收藏 +5 × delta
权重为常量，集中定义，可配置。
```

两条铁律：

1. **只加增量，永不重算总分**。禁止"读 DB 三个总量 → 加权 → ZADD 覆盖"：
   既多读 DB，又有并发覆盖丢失，且 DB 总量本身滞后（增量还在 Redis 里）。
   ZINCRBY 是加法，各维度独立累加，最终和天然正确——一致性问题被模型消解。
2. **三个维度不需要对齐 flush 时间**。浏览 5 分钟一刷、点赞收藏 10 分钟一刷都行，
   加法模型下先后与间隔不影响结果。榜单永远处于"最终正确"状态。

## 4. 写路径：按事件形态分流（接口零侵入）

浏览/点赞/收藏接口**不碰排行榜**，榜单更新统一发生在落库成功之后。
两条写路径按事件形态分流：

```text
浏览（高频、可折叠）→ 时间驱动批量聚合：
  ① HSCAN 枚举 delta Hash 字段，HGETDEL 原子取走增量
  ② UPDATE ch_post SET post_view_number += delta   批量落库
     失败 → HINCRBY 加回去，下轮重试（补偿逻辑）
  ③ ZINCRBY campushub:rank:{今日} {1×delta} {postId}
     失败 → 仅记 error 日志，不补偿（榜单 at-most-once）

点赞/收藏（低频、单行事件）→ 事件驱动 MQ 异步落库：
  接口：SADD/ SREM 判重标记 → 发布 PostInteractionEvent → 立即返回
  消费者：① INSERT/DELETE ch_like / ch_collect（唯一键 uk(post,user) 幂等兜底，
          重复消息冲突即丢弃）
          ② 落库成功 → ZINCRBY ±权重分，失败不补偿
```

要点：

- **顺序必须是 DB 先、榜单后**：DB 失败会补偿重试，若榜单先加分，重试轮会重复加分；
  榜单后加分且失败不补，最坏少记几分，无感。
- **时间驱动 vs 事件驱动的分工**：浏览量是可折叠的纯计数（5 分钟聚合一次无感），
  用定时任务批量刷；点赞/收藏是独立业务行（ch_like/ch_collect 各一行），
  用 MQ 逐事件落库，及时性好且天然削峰。
- **判重是 Set 标记 + DB 唯一键双层**：Redis Set 挡住绝大多数重复请求，
  标记丢失时唯一键冲突分支幂等丢弃，正确性不依赖 Redis。
- **单一写者**：榜单只由 flush 任务与互动消费者更新，出问题一眼定位，请求路径零耦合。
- **新鲜度**：榜单延迟 ≈ flush 周期（浏览分钟级）/ MQ 消费延迟（点赞秒级），
  对"现在什么最火"的展示场景足够。

## 5. 日榜 / 周榜：分键是结构，TTL 是保洁

- **日榜**：所有 ZINCRBY 落进"执行时刻的当天键"。跨零点漂移（23:59 的交互在 00:01 flush
  落入新键）仅差几条交互，接受，不做事件时间路由。
- **周榜**：`ZUNIONSTORE campushub:rank:week 7` 个日键合成，权重可调（近期权重更高）。
  合成结果写临时键并给短 TTL（如 10 分钟）——首个请求触发合成，窗口内后续请求直接读，
  避免每次看榜都做 O(N) 聚合。这本身就是一次迷你 Cache-Aside。

## 6. 读路径：榜单与详情缓存握手

```text
Top10：
  ZREVRANGE campushub:rank:{今日} 0 9 WITHSCORES     → postId + 分数
  pipeline 批量 GET campushub:post:info:{id}          → 一次网络往返取内容
  miss 的走既有 Cache-Aside 回源
  → 组装 VO。榜单接口的 DB 压力趋近于零。

单帖排名：ZREVRANK campushub:rank:{今日} {postId}
```

## 7. 边界与取舍（有意为之，不是疏漏）

| 事项 | 决策 | 理由 |
|---|---|---|
| 同分排序 | ZSet 按 member 字典序，不加 tiebreaker | 展示无感，不值得加复杂度 |
| member 规模 | 当日被交互过的帖子全量入榜，不裁剪 | 校园规模（万级）远够；真需要时 ZREMRANGEBYRANK 砍万名外 |
| 与 DB 对账 | 不做 | 榜单与 DB 计数天然有分钟级偏差，展示型数据无感 |
| 榜单丢失 | 接受 | 日键 TTL 7 天，且新交互持续重建；数据可再生 |
| 僵尸霸榜 | 分键轮换天然解决 | 每天新键从零开始，旧榜自动沉底，无需衰减公式 |
| 互动消息丢失 | 接受（中等可靠） | durable + 持久化 + 重试 + DLQ 兜底；Redis 标记已写而消息丢失时"已点赞但无记录"，标记 7 天过期后可重新操作自愈 |
| 互动消息乱序 | 接受 | 重试中的 LIKE 落到 UNLIKE 之后会重新插入记录，语义为"用户又点了一次赞"，校园场景无感 |
| 软删除帖子的浏览增量 | 照常累计 | 刷库 UPDATE 不带软删除条件，避免取走的增量无处落；被删帖子的计数本就不再展示 |

## 8. 实现清单

1. `RankConstants`：榜单键前缀、周榜键、权重常量（浏览 1 / 点赞 3 / 收藏 5）。
2. `ViewCountFlushTask`：浏览增量定时刷库（HSCAN 枚举 + HGETDEL 原子取走 →
   批量落库 → ZINCRBY），间隔 5 分钟可配。
3. 点赞/收藏 MQ 链路：`campushub.post` 交换机 + `campushub.post.interaction.queue`
   （durable + TTL + DLX），`PostInteractionConsumer` 落库 ch_like/ch_collect 后加分；
   表 `ch_like` / `ch_collect` 均带 uk(post,user) 唯一键幂等兜底。
4. 榜单查询接口：Top10（含 pipeline 详情组装）、单帖排名、周榜（临时键合成）。
5. 压测实验（按 AGENTS.md 性能实验规范）：榜单接口 QPS/p99、
   判据含"MySQL Questions 差分 ≈ 0、ZREVRANGE 命中日榜键"。
