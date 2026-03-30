# AiTrip 前端使用指南

## 快速启动

### 1. 启动基础服务（Docker）

```powershell
cd C:\Users\admin\Desktop\aitrip
docker-compose -f docker/docker-compose.yml up -d
```

### 2. 启动后端服务（在 IDEA 中）

打开 `aitrip-backend` 模块，运行 `AiTripBackendApplication`

**或在终端：**
```powershell
cd C:\Users\admin\Desktop\aitrip
mvn spring-boot:run -pl aitrip-backend
```

> 默认端口 **8080**，若被占用会自动换端口，前端可通过 ⚙️ 按钮修改

### 3. 初始化测试数据（首次运行）

```powershell
# 导入景区/优惠券数据
docker cp docker/init-sql/03-more-test-data.sql aitrip-mysql:/tmp/test-data.sql
docker exec aitrip-mysql bash -c "mysql -uroot -paitrip123 aitrip < /tmp/test-data.sql"

# 初始化 Redis 秒杀库存
docker exec aitrip-redis redis-cli -a aitrip123 SET seckill:stock:1001 100
docker exec aitrip-redis redis-cli -a aitrip123 SET seckill:stock:1002 50
docker exec aitrip-redis redis-cli -a aitrip123 SET seckill:stock:1003 30
docker exec aitrip-redis redis-cli -a aitrip123 SET seckill:stock:1004 80
docker exec aitrip-redis redis-cli -a aitrip123 SET seckill:stock:1005 40
```

### 4. 打开前端

直接用浏览器打开 `aitrip-frontend/index.html`（无需 Node.js）

---

## 端口说明

| 服务 | 默认端口 | 说明 |
|---|---|---|
| aitrip-backend | 8080 | 主后端服务 |
| aitrip-agent | 8081 | AI Agent 服务 |
| MySQL 主库 | 3308 | aitrip 数据库 |
| MySQL 分片 | 3307 | hmdp_0/hmdp_1 |
| Redis | 6379 | 缓存/队列 |
| Kafka | 9092 | 消息队列 |

**端口被占用时**：前端右上角点 ⚙️ 修改端口，点「🔌 测试连接」确认可用

---

## 功能测试流程

### 测试账号
- 手机号：`13800000001` / `13800000002` / `13800000003`
- 验证码：固定 `123456`

### 完整测试流程

1. **登录** → 点右上角「登录」，输入手机号 + 123456
2. **浏览景区** → 点「探索」，搜索「西安」或点城市快捷按钮
3. **查看热榜** → 首页热榜（首次需要点击几个景区产生热度数据）
4. **预检资格** → 秒杀页选择景区 → 点「预检资格」
5. **秒杀抢票** → 确认后点「立即抢票」
6. **查看订单** → 点「我的订单」查看抢票结果
7. **AI 规划** → 点「AI规划」输入「帮我规划北京3日游」（需启动 Agent 服务）

---

## 常见问题

**Q: 打开页面显示「后端服务未运行」**  
A: 需要先在 IDEA 启动 `AiTripBackendApplication`，或检查端口是否正确

**Q: 热榜数据为空**  
A: 点击几个景区查看详情，系统会自动记录访问热度，热榜会自动更新

**Q: 秒杀失败「库存为空」**  
A: 重新初始化 Redis 库存（见上方步骤3）

**Q: 端口每次启动都不一样**  
A: 在 `aitrip-backend/src/main/resources/application.yml` 固定端口：
```yaml
server:
  port: 8080  # 改为固定端口
```
或每次通过前端 ⚙️ 按钮修改端口
