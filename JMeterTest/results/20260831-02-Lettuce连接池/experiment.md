# 实验 02：Lettuce 连接池对帖子详情接口极限 QPS 的影响

> 本文件在跑任何一轮之前填写完成。数据产出后不得回头修改"假设"与"判据"。

## 假设

实验 01 结论指出瓶颈非 Redis 服务端亦非命令速率单因子，而是 Lettuce 默认共享单连接在同步 API 下的串行化等待。给 Lettuce 开启 commons-pool2 连接池并关闭 shareNativeConnection（池化 arm），普通读写改为从池中借还连接后：

- **pool-off arm**：与实验 01 水平相当（共享单连接串行化，宿主机状态漂移导致绝对值不可跨实验比较，仅作同实验内对照）；
- **pool-on arm**：多连接并行消除单连接排队，瓶颈转移至 Tomcat / 宿主机 CPU / 日志 I/O 复合上限，hot-single 预期 ≥ 9000 QPS（实验 01 notes 下一步动作的预期值）；
- cold-all 受 MySQL 点查与回源路径制约，提升幅度有限，不作强预期。

## 自变量

- 类型：配置型（同一 commit、同一代码，启动参数切换 + 重启）
- 取值：`--campushub.redis.pool-enabled=false`（pool-off arm）/ `--campushub.redis.pool-enabled=true`（pool-on arm）

## 控制变量

- commit：起点 = 终点（配置型）
- 负载：梯度扫描 threads = 30/60/100/150（ramp=1s，duration=20s/级）定位饱和级；确认级 warmup 1 轮（丢弃）+ 正式 3 轮取中位数；HTTP KeepAlive 开启
- 混合比例实现：mixed-80-20 两个并行线程组按 80:20 分配（30→24+6、60→48+12、100→80+20、150→120+30）
- 数据：ch_post = 1,010,004 行；热帖 postId = 1000000000000010000（缓存预热）；冷 ID = 全量主键序 ID 切成两个不相交区间，pool-off arm 消费前半段、pool-on arm 消费后半段（避免跨 arm 温读污染），每轮注入独立段文件（JMeter CLI 重置 CSV 游标，单遍不循环）
- 池参数：max-active=16、max-idle=8、min-idle=4、max-wait=100ms
- 环境：同一 Windows 宿主机；JMeter 与应用同机；MySQL/Redis 容器化；rabbitmq/app 容器保持停止（与实验 01 一致）

## 因变量与判据（跑前定死，必须可量化）

- 极限 QPS 定义：相邻梯度 QPS 增幅 <5% 的最早梯度级，该级 warmup 1 + 正式 3 轮的中位数吞吐
- hot-single：pool-on 极限 QPS ≥ 1.5 × pool-off 极限 QPS（同实验内对照）
- mixed-80-20：pool-on 极限 QPS ≥ 1.3 × pool-off 极限 QPS
- cold-all：pool-on ≥ pool-off（不下降即通过，提升幅度仅记录不判）
- 全部轮次错误率 = 0；若出现池借取超时（borrow 异常），判为池容量红线，调参重测并在 notes 记录
- 命令数守恒：Redis total_commands_processed 差分/请求数 hot ≈ 3、mixed ≈ 3.2、cold ≈ 4（±20%），池化不应改变命令数
- MySQL 卸载守恒：Com_select 差分/请求数 cold ≈ 1、mixed ≈ 0.2、hot ≈ 0（<0.01）
- 池生效机制证据：pool-on arm 压测窗口内应用侧 Redis 连接数（INFO clients 的 connected_clients）≥ min_idle+1 且 ≤ max_active+2（含 redis-cli 自身）；pool-off arm ≈ 1~2

## 步骤

1. 构建应用，pool-off 启动：池生效预检（压 30s 小流量，抓 connected_clients，应为 1~2）
2. pool-off arm 三场景：梯度扫描 → 定位饱和级 → 抓基线计数 → warmup 1 + 正式 3 轮 → 每轮抓终态计数
3. 切 pool-on 重启：池生效预检（connected_clients 应 ≥ min_idle+1）
4. pool-on arm 三场景：同步骤 2
5. gawk summarize.awk 统计各轮指标，填 notes.md；计数器差分对照判据逐项结论
