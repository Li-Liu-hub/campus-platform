# 优化更新 Spring Boot 镜像速度

## 一、本次问题记录

### 1. 问题描述

执行以下命令更新 Spring Boot 应用镜像时，构建过程耗时较长：

```powershell
docker compose build app
```

本次记录的构建时间为：**859.1 秒（约 14 分 19.1 秒）**。

本次没有发现构建错误，主要耗时集中在基础镜像拉取和 Maven 依赖下载阶段。

优化后重新构建本地应用镜像已验证成功，本次宿主机实际总耗时为：**13.0 秒**。基础镜像和 Maven 依赖缓存均已命中，未重复下载依赖。

### 2. 当前构建流程

当前项目使用 Docker 多阶段构建：

1. Docker 将构建上下文发送到 Docker 构建环境。
2. Maven 构建阶段基于 `maven:3.9-eclipse-temurin-17` 镜像创建。
3. `COPY . .` 将构建上下文复制到 Maven 构建阶段的 `/build` 目录。
4. 在 Maven 构建阶段执行 `mvn -DskipTests package`，完成所有模块的编译和打包。
5. 运行阶段基于 `eclipse-temurin:17-jre` 镜像创建。
6. 只将构建阶段生成的 Spring Boot JAR 复制到最终运行镜像中。

最终运行镜像不会包含完整项目源码，但源码会参与前面的临时构建阶段。

### 3. 日志中的主要耗时

根据本次构建日志：

- `eclipse-temurin:17-jre` 基础镜像拉取和解压耗时约 68 秒。
- `maven:3.9-eclipse-temurin-17` 基础镜像拉取和解压耗时约 197 秒。
- Docker 构建上下文约 231.74 MB。
- Maven 从 Maven Central 下载依赖时速度较慢，部分依赖下载速度只有几 KB/s 到几十 KB/s。
- 日志最后仍处于 `RUN mvn -DskipTests package` 的依赖下载阶段。

本次优化后复测日志显示：

- Docker 构建上下文约 2.99 KB。
- Maven 构建步骤耗时约 6.6 秒，Maven 内部统计耗时约 4.581 秒。
- Maven 缓存命中，日志中没有再次下载项目依赖。
- 最终本地镜像 `campushub_final-app:latest` 构建成功。

## 二、问题原因

### 1. Maven 构建阶段没有持久化缓存

当前 Dockerfile 直接在构建阶段执行 Maven，但没有挂载 `/root/.m2` 缓存目录。

当源码变化导致 Docker 构建层失效时，Maven 可能再次下载依赖。

### 2. `COPY . .` 会使缓存层容易失效

当前 Dockerfile 先复制整个项目，再执行 Maven 构建：

```dockerfile
COPY . .
RUN mvn -DskipTests package
```

任何源码、配置或项目文件发生变化，都可能导致后续 Maven 构建层重新执行。

### 3. 构建上下文过大

当前构建上下文约为 231.74 MB，说明 `.git`、`.idea`、构建产物等不参与镜像构建的内容也可能被发送给 Docker。

### 4. 基础镜像和 Maven 仓库存在网络下载耗时

首次构建需要拉取 Maven 和 JRE 基础镜像，同时还需要从 Maven Central 下载项目依赖。当前日志显示网络下载速度较慢。

### 5. `-DskipTests` 仍可能解析测试相关插件

`-DskipTests` 通常只是不执行测试，但仍可能进行测试代码编译或解析 Surefire 等测试插件。当前项目没有测试代码时，可以评估使用 `-Dmaven.test.skip=true`。

## 三、优化思路

本次已完成基础优化，后续可以继续按以下方向优化：

### 1. 配置 `.dockerignore`

排除不需要发送给 Docker 的内容：

```text
.git
.idea
**/target
.codex
*.iml
*.log
```

这样可以减小构建上下文，减少文件传输和 `COPY . .` 的处理时间。

本次已创建 `.dockerignore`，构建上下文从约 231.74 MB 降低到约 3.38 KB。

### 2. 使用 Maven 缓存挂载

在 Dockerfile 中为 Maven 本地仓库配置持久化缓存：

```dockerfile
RUN --mount=type=cache,id=campushub-maven,target=/root/.m2,sharing=locked \
    mvn -Dmaven.test.skip=true package
```

修改源码后，即使 Maven 构建步骤重新执行，也可以复用未变化的依赖，只下载新增或版本变化的依赖。

本次已在 Dockerfile 中启用该缓存，缓存 ID 为 `campushub-maven`。

### 3. 分离 POM 文件和源码复制步骤

先复制根项目和各模块的 `pom.xml`，提前解析依赖；依赖不变时，源码更新不会导致依赖解析层失效。

### 4. 评估跳过测试编译

如果镜像构建阶段不负责执行测试，并且测试在其他流程中完成，可以使用：

```text
-Dmaven.test.skip=true
```

### 5. 采用本地打包、Docker 只复制 JAR 的方式

开发环境也可以先在宿主机执行 Maven 打包，再使用只包含 JRE 和 JAR 的运行镜像，从而避免每次在 Docker 构建阶段重新执行 Maven。

## 四、时间对比记录

| 项目 | 构建时间 |
|---|---:|
| 优化前 | 859.1 秒（约 14 分 19.1 秒） |
| 优化后首次构建 | 778.8 秒（约 12 分 58.8 秒） |
| Maven 缓存命中后的再次构建 | 约 10 秒（Maven 步骤 7.1 秒） |
| 本次重新构建（缓存命中） | 13.0 秒（Maven 步骤 6.6 秒） |
| 首次构建节省时间 | 80.3 秒 |
| 本次重新构建相比优化前节省时间 | 846.1 秒，约 98.5% |

## 五、补充说明

- `docker compose build app` 只负责构建镜像，不会启动 `campushub-app` 容器。
- 基础镜像首次下载完成后，后续构建通常可以复用基础镜像缓存。
- Maven 缓存挂载不会因为依赖变化而全部失效，只会下载新增或变化的依赖。
- 本次记录的是镜像构建耗时，不是 Spring Boot 应用启动耗时。
- 本次优化后的首次构建仍需要填充 Maven 缓存，后续源码更新时预计只需重新编译并复用已下载依赖。
- 二次构建已验证 Maven 缓存生效：即使 Maven 步骤重新执行，也没有再次下载依赖，Maven 构建步骤耗时约 7.1 秒。
- 本次重新构建再次验证 Maven 缓存生效：总耗时 13.0 秒，Maven 构建步骤耗时约 6.6 秒，未重复下载依赖。
