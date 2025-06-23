# 热门帖子 Redis 缓存实现（定稿）

本文是最终实现规格。三个组件：详情缓存、空值缓存（并入详情键，单键双态）、浏览量聚合（大 Hash + 定时 flush）。

## 0. 方案总览与职责边界

| 组件 | 键 | 类型 | 一句话职责 |
|---|---|---|---|
| 详情缓存 | `campushub:post:info:{postId}` | String | 详情读加速，Cache-Aside |
| 空值缓存 | `campushub:post:info:{postId}`（同一键，值 = 空串） | String | 防穿透，不存在的帖子不再打 DB |
| 浏览量聚合 | `campushub:post:view:delta` | Hash | 浏览增量在 Redis 累计，定时任务折叠回 DB |

边界约定：

- MySQL 是唯一真相源；详情缓存是**可再生的副本**（TTL 合法），聚合 Hash 是**仅此一份的原始增量**（TTL 禁设）；
- 详情缓存只由"内容写操作"失效，浏览量 flush **不碰**详情缓存（避免热帖周期性集体回源）；
- 两套键互不感知，各自的写路径各自闭环。

## 1. 详情缓存

### 1.1 键值规格

```text
键：    campushub:post:info:{postId}
类型：  String
值：    PostVO 序列化 JSON（与接口契约同构，暂不单独建缓存 DTO）
```

字段语义：

| 字段 | 语义 |
|---|---|
| postId / postUserId / postIdempotencyKey | 精确值，永不变化 |
| postTitle / postType / postText | 可能脏；由 DEL 失效保证新鲜，TTL 兜底 |
| postViewNumber | 快照值，**允许滞后**（实时增量在聚合 Hash 里） |
| createTime / updateTime | LocalDateTime → ISO 字符串 |

### 1.2 序列化规则（两条军规）

1. **必须使用 Spring 容器注入的 ObjectMapper**，禁止 `new ObjectMapper()`：容器实例自动注册 JavaTimeModule（LocalDateTime 正确序列化为字符串），且默认忽略未知字段（字段演进兼容）。自己 new 的会把 LocalDateTime 序列化成数组、遇到旧结构多出的字段直接抛异常。
2. **序列化/反序列化异常一律降级为 miss**：catch 后 DEL 该键（清毒 key）、回源 DB 重写。缓存解析失败绝不能让详情接口 500。

### 1.3 读流程（单键三分支）

```text
GET campushub:post:info:{postId}
  ├─ 返回 JSON   → 命中，反序列化直接返回
  ├─ 返回 ""     → 已知不存在，直接 404，不回源（空值缓存生效）
  └─ 返回 null   → 真 miss，回源 DB：
        ├─ 命中   → SET key JSON EX (300 + random(0~120))，返回
        └─ 不存在 → SET key "" EX 60，返回 404
```

一次 GET 完成三种判断，无额外往返。

### 1.4 TTL 配置

| 项 | 值 | 说明 |
|---|---|---|
| 正常值 | 300s + random(0~120)s | 语义：失效逻辑出 bug 时脏数据最大存活时间；抖动防雪崩 |
| 空值 | 60s 固定，不加抖动 | 空值是"此刻不存在"的断言，必须短；数量少且零散创建，无雪崩条件 |
| 续期 | **无**——命中读不续期 | 滑动续期会让竞争窗口写回的脏数据被人气"续命"永生；固定 TTL 才是自愈机制 |
| 设置方式 | TTL 随 SET 命令一并下发（`SET key value EX n`） | 禁止 SET 后补 EXPIRE：两步之间进程挂掉会造出永生键 |

### 1.5 写失效

```text
update 成功 → DEL post:info:{postId}
delete 成功 → DEL post:info:{postId}
create 成功 → DEL post:info:{postId}   ← 清掉可能残留的空值态，新帖立即可查
```

统一先 DB 后 DEL；并发竞争窗口概率极低，由 TTL 自愈，不做延迟双删。

## 2. 空值缓存（已并入详情键，单键双态）

设计要点全部体现在 1.3 / 1.4：

- 双键方案（`info:{id}` + `info:empty:{id}`）作废：miss 路径多一次往返，TTL 双份维护；
- 单键双态：值的三种形态（JSON / 空串 / null）即三种状态，业务层按返回值分支；
- 空值断言的时效上限 60s，误判兜底靠 create 路径的 DEL。

## 3. 浏览量聚合（大 Hash）

### 3.1 键值规格

```text
键：     campushub:post:view:delta          全局唯一一个键
类型：   Hash
field：  字符串化 postId，如 "103"
value：  自上次 flush 以来的浏览增量
键 TTL： 不设。可选保险丝：flush 任务每次成功后 EXPIRE 7d（仅当无任务健康监控时启用；续期者必须是 flush 任务，绝不能是浏览路径）
```

field 生命周期闭环（无 TTL 参与）：HINCRBY（不存在自动置 1）→ 累计 → flush 原子取走 → field 消失 → 再浏览才重建。冷帖不浏览即无 field，无垃圾滞留。

### 3.2 浏览路径

```text
HINCRBY campushub:post:view:delta {postId} 1
```

单条命令，不查不改不碰 TTL。

### 3.3 flush 定时任务（每 5 分钟）

```text
1. HSCAN campushub:post:view:delta cursor COUNT 500   ← 游标分批，禁用 HGETALL
2. 对本批每个 field 原子取删：
     Redis ≥ 7.4：HGETDEL key field
     Redis < 7.4：微型 Lua（HGET + HDEL 原子执行），脚本只做这一步，禁止放批量逻辑
   取回 null 说明已被处理，跳过
3. 批量落库：UPDATE ch_post SET post_view_number = post_view_number + delta
   （DB 侧增量累加，禁止"查出值算好再 SET 回去"）
4. DB 失败补偿：本批 delta 执行 HINCRBY 加回，留给下轮
```

**故障语义（已知且接受）**：原子取删之后、落库之前进程崩溃 → 该批增量丢失（at-most-once）。浏览量为展示型数据，"宁可丢不可重"是明确取舍；若换成"先落库后扣减"则是 at-least-once（崩溃导致重复计数），同样可接受，按此取舍选定原子查删。

### 3.4 与详情缓存的联动

flush 任务**不 DEL** `post:info:{postId}`。详情缓存中的浏览量是快照，滞后容忍；flush 后 DEL 会让热帖每周期集体回源，属于自造雪崩。

## 4. Lua 使用边界

- 适用：把"几条命令的微序列"打包成原子操作（本设计唯一候选：Redis < 7.4 时的 HGET+HDEL）；
- 铁律：Lua 执行期间独占 Redis 单线程，脚本耗时 = Redis 阻塞时长。脚本只允许几行，**批量遍历、循环、业务逻辑一律留在 Java 侧**；
- 建议：使用 EVALSHA + 脚本缓存，避免每次传输脚本正文。

## 5. 测试方案

- 对比法：无缓存基线 vs 引入缓存，同一 JMeter 场景各跑一轮；
- 场景 A 冷读（key 全 miss，应与基线持平）；场景 B 热读（固定少数 postId 反复打，测命中路径）；
- 指标：QPS、p95/p99（Tomcat access log `%D` 已配置）、**MySQL 查询 QPS**（命中时 DB 读应趋近 0，最硬证据）、Redis 命中率；
- **注意**：当前 `GET /posts/get/{postId}` 调的是 `view()`，含一条浏览量 UPDATE，会把 QPS 天花板压死。测纯读路径前需将"读详情"与"浏览自增"拆开，否则会得出"缓存无用"的错误结论。

## 6. 实现清单

1. `RedisService` 扩展 Hash 能力：`hIncrBy`、`hScan`（游标分批）、原子取删（7.4+ 走 HGETDEL，否则执行微型 Lua）；
2. `PostServiceImpl.getById` 接读缓存三分支逻辑；`update` / `delete` / `create` 接 DEL 失效；
3. `view()` 路径的浏览量改为 `hIncrBy`（浏览量落库策略按既定决策后续单独设计）；
4. flush 定时任务：HSCAN 分批 → 原子取删 → 批量增量落库 → 失败补偿；
5. ObjectMapper 从容器注入，禁止 new；
6. 测试：按第 5 节跑基线与缓存对比。

## 7. 实现记录与压测结果（2026-08-31）

### 7.1 实现状态

| 清单项 | 状态 | 说明 |
|---|---|---|
| 1. RedisService 扩展 | 部分完成 | `hIncrBy` 已落地；`hScan` / 原子取删随 flush 任务（第 4 项）一起实现 |
| 2. getById 缓存三分支 + 写路径 DEL | 已完成 | `getCachedPost` 单键三分支；create/update/delete 先 DB 后 DEL |
| 3. view() 改 hIncrBy | 已完成 | 当前无 flush，增量暂存 Hash（按既定决策"只做记账"） |
| 4. flush 定时任务 | 未开始 | 后续任务 |
| 5. ObjectMapper 容器注入 | 已完成 | 实测 LocalDateTime 序列化为 ISO 字符串 |
| 6. 基线与缓存对比 | 已完成 | 数据见 7.3 与《帖子查询接口压测报告》 |

环境实测 Redis 版本 **7.4.11**，flush 实现时可直接使用 HGETDEL，整段跳过 Lua。

### 7.2 实现补充决策

- **毒 key 删除加二次保护**：反序列化失败分支的 DEL 自身被 try-catch 包裹，Redis 闪断时删除失败仅告警、本次仍回源，保证"缓存故障不 500"；残留毒 key 由 TTL 自愈。
- **已知取舍（未做防击穿互斥）**：热键 TTL 过期瞬间允许一小波并发回源（单次主键 SELECT，量级与改造前常态相当），未引入单飞/分布式锁的复杂度；后续若出现超热键再评估。
- `PostMapper.increaseViewNumber`（单帖 +1 UPDATE）已无调用方，暂保留，供 flush 批量增量落库实现时参考改造。

### 7.3 压测结果摘要

60 线程 × 10s 热读固定热帖（1000000000000010000），同口径对比（本地 jar，基线代码经 git worktree 从 HEAD 构建）：

| 版本 | QPS（稳定区间） | avg(ms) | p95(ms) | p99(ms) | 错误 |
|---|---|---|---|---|---|
| 基线（SELECT + UPDATE 每请求） | 339 ~ 344 | 166 ~ 168 | 255 ~ 364 | 323 ~ 502 | 0 |
| 缓存后（GET + HINCRBY 命中路径） | 1078 ~ 1278 | 45 ~ 53 | 88 ~ 140 | 119 ~ 172 | 0 |

**QPS 约 3.5 倍，平均延迟下降约 71%，命中路径 MySQL 读写归零。** 全量分位数、场景配置与功能验证过程见《帖子查询接口压测报告》。
