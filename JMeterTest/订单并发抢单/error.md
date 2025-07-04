# error.md — 订单并发抢单测试踩坑记录

## 2026-09-01 功能与并发测试过程

### 1. JMeter CSV（tokens）文件中文路径导致线程 0 样本

- **现象**：`jmeter -n` 正常结束但 `summary = 0 in 00:00:00`，JTL 只有表头；jmeter.log 报
  `IllegalArgumentException: File ... must exist and be readable`（路径乱码）。
- **原因**：`-JtokenFile=<含中文的绝对路径>` 经 Windows 命令行传给 JVM（file.encoding=GBK）
  时编码不一致，CSVDataSet 打不开文件，线程直接结束。
- **解决**：token 文件写到纯 ASCII 路径（`%TEMP%\campushub-grab-tokens-rN.txt`）再传给 `-J`。

### 2. PS5.1 脚本模式裸词 `-Jx=$var` 不展开

- **现象**：jmeter.log 出现 `Setting JMeter property: tokenFile=$tokenFile`，属性值是字面量；
  同一行里 `-l $jtl`（独立变量 token）却正常。
- **原因**：PowerShell 5.1 脚本（`-File` 执行）中，裸词参数 `-Jx=$var` 未做变量插值（交互式
  会话与脚本模式行为差异），导致 JVM 收到字面量 `$orderId` / `$tokenFile`。
- **解决**：所有含变量的参数改为双引号字符串并拆分数组后展开调用：
  `$jmeterArgs = @('-n','-t',$jmx,'-l',$jtl,'-j',$jlog,"-JorderId=$orderId","-JtokenFile=$tokenFile",'‑Jport=8081'); & jmeter @jmeterArgs`。

### 3. mysql 命令行密码警告中断 PS 脚本

- **现象**：`docker exec campushub-mysql mysql -p123456 ...` 的 stderr 警告
  `Using a password on the command line interface can be insecure` 触发
  `NativeCommandError`，在 `$ErrorActionPreference='Stop'` 下脚本直接终止。
- **原因**：PS5.1 将原生命令 stderr 输出视为错误记录；重定向 `2>$null` 也可能抛错。
- **解决**：`docker exec -e MYSQL_PWD=123456 ...` 在容器内注入密码环境变量，规避命令行密码；
  注意宿主机 `$env:MYSQL_PWD` 不会传进容器，必须用 `docker exec -e`。

### 4. 无 BOM 的 UTF-8 ps1 中文解析失败

- **现象**：`powershell -File xxx.ps1` 报"字符串缺少终止符"等解析错误，中文显示为乱码。
- **原因**：Windows PowerShell 5.1 对无 BOM 文件按 ANSI（GBK）读取，UTF-8 中文被误读破坏语法。
- **解决**：写盘后统一转带 BOM 的 UTF-8：
  `[IO.File]::WriteAllText($p, [IO.File]::ReadAllText($p,[Text.Encoding]::UTF8), (New-Object Text.UTF8Encoding $true))`。

### 5. docker pull 直连 Docker Hub 失败

- **现象**：`docker compose up -d redis` 拉取 `redis:8-alpine` 报
  `dial tcp ... registry-1.docker.io:443 connectex failed`（Docker Desktop 无代理直连超时）。
- **原因**：网络环境无法直连 Docker Hub。
- **解决**：经镜像源拉取后重打标签：
  `docker pull docker.1ms.run/library/redis:8-alpine && docker tag ... redis:8-alpine`。
  （`docker.m.daocloud.io` 在当前网络下长时间无响应。）

### 6. 抢单幂等语义与测试用例设计

- **现象**：用发布者 A 对已被抢走的订单再次抢，期望 409，实际返回 400"不能抢自己发布的订单"。
- **原因**：自抢校验先于状态校验，属预期业务行为，是测试用例选错用户。
- **解决**：用接单人 B（非发布者）再次抢，命中状态校验返回 409。

### 7. campushub-redis 从 7.4 升级 8.0 的原因记录

- **现象**：Redis 7.4.11 上 `HSETEX`/`HGETDEL` 报 `ERR unknown command`。
- **原因**：这两条哈希字段级原子命令自 **Redis 8.0** 起提供；7.4 仅提供 HEXPIRE 等字段 TTL 命令，
  无法用内置原子命令完成"带 TTL 的获取 + 校验释放"。
- **解决**：docker-compose 镜像改 `redis:8-alpine` 并重建容器（AOF 数据兼容直接加载）；
  Lettuce 6.6.0（Spring Boot 3.5.16 管理）已内置 `hsetex`/`hgetdel` 类型安全 API，无需自定义命令分发。
