# MQ 可靠传输测试错误记录（追加式）

## 20260901232900 outbox 双臂故障注入

### 1. 发件箱中继被调度线程饿死（严重，已修复）

- 现象：注入 20 条 status=0 消息，broker 宕机 72s 内 `retry_count` 恒为 0，
  无"发件箱本轮发送"/"消息发布失败"日志；线程转储显示唯一调度线程
  `scheduling-1` RUNNABLE 卡在 `ViewCountFlushTask.persist`（百万行 ch_post
  逐条 UPDATE 长任务），5s 轮询中继被单线程调度器饿死。
- 原因：Spring 默认 `TaskScheduler` 单线程，长任务与短周期任务共用一根线程。
- 解决：`spring.task.scheduling.pool.size: 4`（dev/prod），修复后宕机期
  avg_retry=7、broker 恢复 15s 内全量补发 confirm。

### 2. 帖子互动 MQ 拓扑声明丢失（严重，已修复）

- 现象：合并后干净 broker 无 `campushub.post.interaction.queue`，
  `missingQueuesFatal` 使应用启动即失败退出（QueuesNotAvailableException）。
- 原因：commit 057b681 移除误入订单分支的互动拓扑声明，后续排行榜分支只
  补回了常量未补回 Queue/Exchange/Binding Bean。
- 解决：RabbitMqConfig 按 MqConstants 补齐互动交换机/队列/DLX/DLQ/绑定。

### 3. outbox 主键越界（测试脚本）

- 现象：`ERROR 1264 Out of range value for column 'id'`，9800000000000000001
  超出 BIGINT 上限 9223372036854775807。
- 解决：测试数据改用 8.8e18 段（88000000000000000NN）。

### 4. PowerShell 管道写 SQL/脚本导致中文乱码（环境）

- 现象：`Get-Content | Set-Content` 或管道注入容器后，UTF-8 中文被按 GBK
  双重编码，注释/正文乱码。
- 解决：SQL 与脚本一律 `docker cp` 进容器或用支持 UTF-8 的编辑器改写，
  mysql 客户端显式 `--default-character-set=utf8mb4`。
