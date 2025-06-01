# Redis light 使用手册

## 查看 Redis 数据

1. 打开 Redis light，进入 **Redis Databases** 页面。
2. 点击顶部的 **Connect existing database**，或者点击页面中间的 **+ Add Redis database**。
3. 填写 Redis 连接信息：

   - **Host**：`localhost`
   - **Port**：`6379`
   - **Username**：没有用户名时留空
   - **Password**：本地 Redis 没有密码时留空
   - **Database**：`0`

4. 点击 **Test Connection** 测试连接，成功后点击 **Add Database** 或 **Save** 保存。
5. 在数据库列表中点击刚添加的数据库连接，进入数据库详情页。
6. 点击左侧的 **Browser**（数据浏览）或 **Data Explorer**。
7. 选择 **DB 0**，即可看到当前数据库中的所有 Redis 键。
8. 点击某个键，即可查看它保存的具体数据和值。

如果看不到数据，请确认连接地址、端口和 Database 是否与项目配置一致。CampusHub 开发环境默认连接 `localhost:6379` 的 `DB 0`。
