# 本地打包、Docker 只复制 JAR：避免容器内 Maven 构建的解析与环境问题，详见 docs/优化更新SpringBoot镜像速度.md
FROM docker.1ms.run/library/eclipse-temurin:21-jre

WORKDIR /app

COPY campushub-web/target/campushub-web-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
