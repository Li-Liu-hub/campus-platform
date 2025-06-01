# Docker 指令集合

## 1. 启动数据库

```powershell
docker compose up -d mysql
```

## 2. 使用当前文件更新 Spring Boot 镜像

```powershell
docker compose build app
```

## 3. 启动 Spring Boot 项目

```powershell
docker compose up -d app
```

## 4. 查看 Spring Boot 日志

```powershell
docker compose logs -f app
```
