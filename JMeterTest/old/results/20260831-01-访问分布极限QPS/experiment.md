# 实验 01：访问分布对帖子详情接口极限 QPS 的影响

> 本文件在跑任何一轮之前填写完成。数据产出后不得回头修改"假设"与"判据"。

## 假设

在缓存策略（详情缓存 + 空值缓存 + 浏览量 HINCRBY 聚合）已上线的前提下，接口极限 QPS 由 **Lettuce 共享单连接的串行往返**决定：每请求的 Redis 命令数 × 本地 RTT（≈66μs）给出命令速率上限 ≈15000 cmds/s，折算为请求上限：

- **hot-single（100% 热帖）**：每请求 3 条命令（checkLogin 读 + GET + HINCRBY）→ 预期极限 ≈ **5000 QPS**（前序实测 5062 已验证）
- **mixed-80-20（80% 热 + 20% 冷）**：冷请求多一条 SET 写回（4 条命令）→ 加权 3.2 条/请求 → 预期极限 ≈ **4700 QPS**
- **cold-all（100% 冷帖）**：每请求 4 条命令（checkLogin + GET miss + SET + HINCRBY）→ 预期极限 ≈ **3700 QPS**；DB 主键点查能力（万级 QPS）远高于所需（≤4000），不应成为瓶颈

瓶颈不在 Redis 服务端（执行仅占 ~8% 时间），不在 MySQL（冷场景点查压力上限约 4000 QPS），而在客户端单连接串行化。

## 自变量

- 类型：配置型（同一 commit、同一代码，仅压测请求分布不同）
- 取值：请求分布 hot-single（arm/hot-single）/ mixed-80-20（arm/mixed-80-20）/ cold-all（arm/cold-all）

## 控制变量

- commit：起点 = 终点 = a52d3d7（feat: 帖子详情缓存策略与浏览量聚合记账）
- 负载：扫描梯度 threads = 10/30/60/100（ramp=1s，duration=20s/级）；确认级 warmup 1 轮 + 正式 3 轮取中位数；HTTP KeepAlive 开启
- 混合比例实现：mixed-80-20 为两个并行线程组，线程数按 80:20 分配（10→8+2、30→24+6、60→48+12、100→80+20）
- 数据：ch_post = 1,010,004 行；热帖 postId = 1000000000000010000；冷读 ID 池 = 全量 1,010,004 个主键序 ID 顺序消费（recycle，单遍不循环，杜绝温读污染）；Redis 起点状态：热帖缓存已预热（首扫自然回源），冷 ID 零缓存
- 环境：同一 Windows 宿主机；JMeter 与本地 jar 应用同机；MySQL/Redis 容器化（rabbitmq/app 容器已停）

## 因变量与判据（跑前定死，必须可量化）

- 极限 QPS 定义：相邻梯度 QPS 增幅 <5% 的最早梯度级，该级 warmup 1 + 正式 3 轮的中位数吞吐
- hot-single 极限 QPS ∈ [4500, 5500]（预期 5000±10%）
- mixed-80-20 极限 QPS ∈ [4200, 5200]（预期 4700±10%）
- cold-all 极限 QPS ∈ [3300, 4100]（预期 3700±10%）
- 计数器差分：hot-single 确认轮 MySQL Com_select 差分/请求数 ≈ 0（<0.01）；cold-all ≈ 1（每请求一次回源 SELECT）；mixed-80-20 ≈ 0.2
- Redis total_commands_processed 差分/请求数：hot-single ≈ 3；mixed-80-20 ≈ 3.2；cold-all ≈ 4（±20% 内视为吻合，含 sa-token 续期等噪声）
- 全部轮次错误率 = 0

## 步骤

1. 每 arm：梯度扫描 10/30/60/100 线程（scan-N.jtl），定位饱和级（若 100 线程仍 >5% 增长，启用 150 线程追加）
2. 每 arm：确认级抓基线计数（counters/runN-before.txt）→ warmup 1 轮（丢弃）→ 3×正式轮（run1~3.jtl）→ 每轮抓终态计数（runN-after.txt）
3. 离线统计各轮 QPS/p95/p99/错误率，填入 notes.md
4. 计数器差分除以请求数，对照判据逐项结论，填入 notes.md
