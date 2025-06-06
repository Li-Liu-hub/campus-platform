# Redis light 使用手册

## 项目环境说明

### 测试环境（dev）

- Spring Boot 应用在宿主机本地启动，例如通过 IDE 或 Maven 运行。
- Redis 等中间件运行在 Docker 容器中。
- 本地应用通过 Docker 映射到宿主机的端口访问 Redis。
- 当前 Redis 的本机访问地址为 `127.0.0.1:6380`，容器内部端口仍为 `6379`。
- 测试环境不能将 Redis 地址配置为 `redis:6379`，因为本地运行的 Spring Boot 不在 Docker Compose 网络中。

### 生产环境（prod）

- Spring Boot 应用、Redis 和其他依赖服务均运行在 Docker 容器中。
- 应用容器通过 Docker 服务名访问 Redis：`redis:6379`。
- 生产环境使用容器内部端口，不使用宿主机的 `127.0.0.1:6380`。

## 连接 Redis 容器

在截图中的 **Connection URI** 输入框内，删除原来的内容，填写：

```text
redis://root:123456@127.0.0.1:6380/0
```

然后按以下顺序操作：

1. 点击 **Test connection**。
2. 测试成功后，点击 **Add database**。
3. 点击刚添加的 Redis 数据库。
4. 点击 **Browser**，选择 **DB 0**。
5. 点击任意键，即可查看保存的数据。

> 项目中的 Redis 容器使用 ACL 认证，用户名为 `root`，密码为 `123456`。

## 出现 Authentication failed 时

1. 点击 **Connection settings**。
2. 将 **Host** 填为 `127.0.0.1`，**Port** 填为 `6380`，数据库选择 `0`。
3. 用户名填写 `root`，密码填写 `123456`。
4. 保存设置后重新点击 **Test connection**。

不要填写 `default` 用户名，当前 Redis 已关闭默认用户。
