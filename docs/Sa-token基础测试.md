# Sa-Token 基础测试报告

## 一、登录请求

### 请求信息

```text
请求方法：POST
请求地址：http://localhost:8080/api/v1/auth/login
```

### 请求体

```json
{
  "userName": "test001",
  "userPassword": "123456"
}
```

本次基础登录未使用 `X-Device-Id`、`X-Device-Type` 等多设备请求头。

### 响应结果

```text
HTTP 状态码：200
响应耗时：472 ms
响应大小：564 B
```

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 2092954516224405506,
    "userName": "test001",
    "userNickname": "test001",
    "tokenValue": "d81dc805-f046-48d6-96c7-d70475292129"
  }
}
```

## 二、登录后的 Redis 数据

### Redis 连接信息

```text
地址：127.0.0.1:6380
逻辑数据库：DB 0
扫描结果：2 个键
```

Redis 中生成了 1 个账号会话键和 1 个 Token 映射键。

### 1. 用户账号会话

```text
键名：Authorization:login:session:2092954516224405506
类型：String
长度：555
占用空间：736 B
TTL：2591989 秒
```

Redis 中保存的值：

```json
{
  "@class": "cn.dev33.satoken.session.SaSession",
  "id": "Authorization:login:session:2092954516224405506",
  "type": "Account-Session",
  "loginType": "login",
  "loginId": [
    "java.lang.Long",
    2092954516224405506
  ],
  "token": null,
  "historyTerminalCount": 1,
  "createTime": 1787901513323,
  "dataMap": {
    "@class": "java.util.concurrent.ConcurrentHashMap"
  },
  "terminalList": [
    "java.util.Vector",
    [
      {
        "@class": "cn.dev33.satoken.session.SaTerminalInfo",
        "index": 1,
        "tokenValue": "d81dc805-f046-48d6-96c7-d70475292129",
        "deviceType": "DEF",
        "deviceId": null,
        "extraData": null,
        "createTime": 1787901513256
      }
    ]
  ]
}
```

基础 Sa-Token 登录未传入设备信息，因此终端记录使用默认值：

```text
deviceType：DEF
deviceId：null
extraData：null
```

### 2. Token 与用户 ID 的映射

```text
键名：Authorization:login:token:d81dc805-f046-48d6-96c7-d70475292129
类型：String
长度：19
占用空间：120 B
TTL：2591976 秒
值：2092954516224405506
```

## 三、测试结论

1. 基础登录接口返回 HTTP 200，登录成功。
2. 登录接口返回 Sa-Token Token：`d81dc805-f046-48d6-96c7-d70475292129`。
3. Redis 中创建了账号会话键和 Token 映射键，共 2 个键。
4. Token 映射键通过 Token 指向用户 ID `2092954516224405506`。
5. 未传入多设备信息时，Sa-Token 使用默认终端信息，不记录自定义设备 ID 和扩展数据。
