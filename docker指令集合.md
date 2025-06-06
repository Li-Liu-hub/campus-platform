# Docker 指令集合

## 1. 启动数据库

```powershell
docker compose up -d mysql
```

## 2. 启动 Redis

```powershell
docker compose up -d redis
```

## 3. 使用当前文件更新 Spring Boot 镜像

```powershell
docker compose build app
```

## 4. 启动 Spring Boot 项目

```powershell
docker compose up -d app
```

## 5. 查看 Spring Boot 日志

```powershell
docker compose logs -f app
```
