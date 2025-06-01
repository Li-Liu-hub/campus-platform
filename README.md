# CampusHub

CampusHub 是一个基于 Java 17、Spring Boot 3.x 和 Maven 的多模块 Web 应用，采用模块化单体架构。当前项目只搭建基础模块，具体业务模块按需求逐步增加。

## 项目结构

```text
CampusHub_Final/
├─ pom.xml                         # 根项目，统一管理 Maven 模块和版本
├─ campushub-common/               # 公共常量、异常、响应对象和工具类
├─ campushub-infrastructure/       # MyBatis-Plus、MySQL 等基础设施
├─ campushub-system/               # 系统管理、角色和权限
├─ campushub-user/                 # 平台用户和用户资料相关功能
├─ campushub-web/                  # Spring Boot 启动模块和 HTTP 接口
├─ Dockerfile                      # 构建 Spring Boot Docker 镜像
└─ docker-compose.yml              # 管理应用和 MySQL 容器
```
