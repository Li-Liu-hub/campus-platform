# Repository Guidelines

## 代码注释规范

### 简单功能

简单方法或功能使用一行注释说明用途即可，注释应简洁、准确，避免重复代码本身的含义。

```java
// 根据用户 ID 查询用户信息
User getById(Long userId);
```

### 复杂功能

复杂方法、核心业务逻辑或容易产生误解的代码，使用完整注释说明以下内容：

1. 功能：说明该方法或代码块实现的业务功能。
2. 传入参数：说明每个参数的含义、格式和必要限制。
3. 返回参数：说明返回值的含义及可能的状态。
4. 抛出异常：说明可能抛出的异常及触发条件。

```java
/**
 * 功能：根据用户名和密码校验用户身份并创建登录会话。
 *
 * @param username 登录用户名，不允许为空
 * @param password 登录密码，使用明文传入并在服务内部校验
 * @return 登录令牌，后续请求使用该令牌进行身份认证
 * @throws IllegalArgumentException 用户名或密码为空时抛出
 * @throws AuthenticationException 用户不存在或密码错误时抛出
 */
String login(String username, String password);
```

### 编写要求

- 注释使用中文，表达清晰，重点说明业务意图。
- 修改代码逻辑时同步更新相关注释。
- 不要编写与代码完全重复的无意义注释。
- 公共接口、核心业务和异常处理逻辑应优先补充注释。

## 性能实验规范

性能优化（缓存、SQL、异步化等）的验证采用对照实验，实验是体验工具效果的手段。

### 实验流程

1. 动代码前：从 `JMeterTest/_TEMPLATE/` 复制模板到
   `JMeterTest/results/{日期}-{两位序号}-{场景}/`，填写 experiment.md
   （假设、自变量、控制变量、可量化判据）。
2. 完成修改后正常提交代码，notes.md 记录起点/终点 commit hash。
3. 每个 arm（自变量取值）各跑 warmup 1 轮（丢弃）+ 正式 3 轮（取中位数）。
4. notes.md 结论包含：预期 vs 实测、MySQL/Redis 计数器差分证据。

### 变量与文件夹结构

- 一个实验一个文件夹（一个假设），内部按自变量取值建 arm 子文件夹
  （如 cache-off/、cache-on/；代码型用 before/、after/）。
- 每个 arm 内：run*.jtl + counters/（计数器前后值）。
- 变量类型：配置型优先（同一 commit 切配置重启）；代码型记录两个
  commit 并 checkout 切换。
- experiment.md 与 notes.md 进 git；run*.jtl 默认不进。

### Commit 规则

实验不改变项目提交流程，commit message 按项目常规编写；
代码状态与实验的对应关系由 notes.md 记录的 commit hash 承载。
