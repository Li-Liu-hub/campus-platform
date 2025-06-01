FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 只复制未被 .dockerignore 排除的项目内容。
COPY . .

# 使用 BuildKit 持久化 Maven 本地仓库，避免源码更新后重复下载未变化的依赖。
RUN --mount=type=cache,id=campushub-maven,target=/root/.m2,sharing=locked \
    mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/campushub-web/target/campushub-web-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
