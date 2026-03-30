# AiTrip 智能旅游服务平台

> 基于 Spring AI + Spring Boot 3 构建的多 Agent 智能旅游规划与高并发票务秒杀系统

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术架构](#2-技术架构)
3. [模块说明](#3-模块说明)
4. [环境要求](#4-环境要求)
5. [基础设施启动](#5-基础设施启动)
6. [数据库初始化](#6-数据库初始化)
7. [Kafka Topic 初始化](#7-kafka-topic-初始化)
8. [应用服务编译与启动](#8-应用服务编译与启动)
9. [接口说明与功能测试](#9-接口说明与功能测试)
10. [集成测试运行](#10-集成测试运行)
11. [监控与运维](#11-监控与运维)
12. [常见问题排查](#12-常见问题排查)
13. [项目关闭与清理](#13-项目关闭与清理)

---

## 1. 项目概述

AiTrip 是一个将 **AI 智能规划** 与 **高并发票务秒杀** 融合的旅游服务平台，由三个 Maven 模块组成：

| 模块 | 端口 | 职责 |
|---|---|---|
| `aitrip-common` | — | 公共工具类、统一异常、响应封装 |
| `aitrip-backend` | 8080 | 景区管理、票务秒杀、订单处理、消息消费 |
| `aitrip-agent` | 8081 | AI 多 Agent 旅行规划、MCP Tool、WorkFlow |

### 核心能力

- **智能旅行规划**：基于 Spring AI + ReAct Agent，通过自然语言生成完整旅行方案，支持 SSE 流式输出
- **多 Agent 协作**：景区推荐 Agent + 票务查询 Agent + 行程编排 Agent，通过 A2A 协议协同工作
- **高并发秒杀**：Lua 原子脚本防超卖，令牌桶限流，Kafka 异步削峰，幂等消费保证
- **多层缓存**：Caffeine L1 本地缓存 + 布隆过滤器防穿透 + Redis L2 分布式缓存
- **Outbox 模式**：保证消息发送与数据库操作的最终一致性

---

## 2. 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                    前端 / 客户端                          │
│         HTTP REST / SSE (Server-Sent Events)             │
└───────────────┬────────────────────┬────────────────────┘
                │                    │
         ┌──────▼──────┐      ┌──────▼──────┐
         │ aitrip-agent │      │aitrip-backend│
         │   :8081      │      │   :8080      │
         │              │      │              │
         │ ReAct Agent  │      │ 秒杀接口     │
         │ WorkFlow     │ MCP  │ 景区接口     │
         │ A2A Agents   ├─────►│ 通知接口     │
         │ MCP Tools    │ HTTP │              │
         └──────────────┘      └──────┬───────┘
                                       │
         ┌─────────────────────────────▼───────────────┐
         │              基础设施层                       │
         │  MySQL:3308   Redis:6379   Kafka:9092        │
         │  MySQL分库:3307            ZooKeeper:2181    │
         └─────────────────────────────────────────────┘
```

### 秒杀链路

```
用户请求 → 令牌桶限流 → Lua 原子预检（库存+防重） → Kafka 发消息
         → Kafka Consumer 幂等消费 → MySQL 落单 → Outbox 补偿
```

### AI Agent 链路

```
用户消息 → IntentAnalyzeNode（意图解析）
         → AttractionRecommendAgent（景区推荐）
         → TicketQueryAgent（票务查询）
         → TripScheduleAgent（行程编排）
         → SSE 流式返回
```

---

## 3. 模块说明

### aitrip-common

```
com.aitrip.common
├── exception/     BusinessException、ErrorCode（统一异常体系）
├── response/      Result<T>（统一响应封装）
└── util/          JsonUtil、SnowflakeIdGenerator（雪花 ID）
```

### aitrip-backend

```
com.aitrip.backend
├── cache/         MultiLevelCacheManager、BloomFilterInitializer
│                  CaffeineCacheConfig、BloomFilterConfig
├── constant/      RedisKeyConstants（所有 Redis Key 常量）
├── consumer/      SeckillOrderConsumer（Kafka 幂等消费 + DLQ）
├── controller/    SeckillController、AttractionController、NotifyController
├── entity/        TbAttraction、TbVoucher、TbVoucherOrder 等 10 张表
├── mapper/        MyBatis Plus Mapper 接口
├── outbox/        OutboxDispatcher（指数退避重试）
├── ratelimit/     @RateLimit 注解、TokenBucketRateLimiter（Lua 令牌桶）
└── service/impl/  SeckillServiceImpl、AttractionServiceImpl 等
```

### aitrip-agent

```
com.aitrip.agent
├── a2a/       AttractionRecommendAgent、TicketQueryAgent、TripScheduleAgent
├── config/    AgentConfig（ChatClient、ChatModel 配置）
├── controller/ TripPlanController（SSE + 同步接口）
├── mcp/       BackendMcpTools（6 个 @Tool 方法）
├── react/     TripPlanReActAgent（流式 ReAct 推理）
└── workflow/  TripWorkFlowService、IntentAnalyzeNode、TripPlanState
```

---

## 4. 环境要求

| 依赖 | 版本要求   | 说明                      |
|---|--------|-------------------------|
| JDK | 21     | 必须使用 JDK 21，不支持 JDK 17+ |
| Maven | 3.6+   | 构建工具                    |
| Docker | 20.10+ | 运行基础设施                  |
| Docker Compose | 2.0+   | 编排容器                    |

### 确认 Java 版本

```bash
java -version
# 期望输出：java version "21.x.x"

javac -version
# 期望输出：javac 21.x.x
```

> ⚠️ 若系统有多个 JDK，请确保 `JAVA_HOME` 指向 JDK 21，否则编译产物类文件版本不兼容，会报
> `UnsupportedClassVersionError: class file version 65.0`

**Windows 确认/设置 JAVA_HOME：**
```powershell
$env:JAVA_HOME = "C:\develop\Java\jdk-21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
```

---

## 5. 基础设施启动

### 5.1 启动所有基础设施容器

```bash
cd aitrip/docker
docker compose up -d
```

### 5.2 验证容器健康状态

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

期望输出（所有容器状态为 `healthy`）：

```
NAMES                STATUS                    PORTS
aitrip-kafka-ui      Up X minutes              0.0.0.0:8090->8080/tcp//要改动
aitrip-mysql         Up X minutes (healthy)    0.0.0.0:3308->3306/tcp
aitrip-mysql-shard   Up X minutes (healthy)    0.0.0.0:3307->3306/tcp
aitrip-kafka         Up X minutes (healthy)    0.0.0.0:9092->9092/tcp
aitrip-redis         Up X minutes (healthy)    0.0.0.0:6379->6379/tcp
```

### 5.3 各组件连接信息

| 服务 | 地址 | 账号/密码 |
|---|---|---|
| MySQL 主库 | `localhost:3308` / DB: `aitrip` | root / aitrip123 |
| MySQL 分库 | `localhost:3307` / DB: `hmdp_0`, `hmdp_1` | root / aitrip123 |
| Redis | `localhost:6379` | 密码: aitrip123 |
| Kafka | `localhost:9092` | 无认证 |
| Kafka UI | http://localhost:8090 | 无需登录 |

---

## 6. 数据库初始化

容器启动时会自动执行初始化 SQL（通过 Docker volume 挂载），无需手动执行。

### 验证主库表结构

```bash
docker exec -it aitrip-mysql mysql -uroot -paitrip123 aitrip -e "SHOW TABLES;"
```

期望输出（8 张主库表）：

```
tb_attraction      # 景区信息
tb_voucher         # 优惠券
tb_seckill_voucher # 秒杀配置
tb_voucher_order   # 订单（主库，分片库另有副本）
tb_user            # 用户
tb_blog            # 游记
tb_follow          # 关注
tb_outbox          # Outbox 消息表
tb_idempotent      # 幂等记录表
```

### 验证分库表结构

```bash
docker exec -it aitrip-mysql-shard mysql -uroot -paitrip123 hmdp_0 -e "SHOW TABLES;"
```

---

## 7. Kafka Topic 初始化

```bash
# Windows（Git Bash 或 WSL 中执行）
bash scripts/init-kafka-topics.sh

# 或手动创建（PowerShell）
docker exec aitrip-kafka kafka-topics --create --if-not-exists \
  --bootstrap-server localhost:9092 --topic aitrip.ticket.seckill --partitions 8 --replication-factor 1

docker exec aitrip-kafka kafka-topics --create --if-not-exists \
  --bootstrap-server localhost:9092 --topic aitrip.ticket.seckill.dlq --partitions 2 --replication-factor 1

docker exec aitrip-kafka kafka-topics --create --if-not-exists \
  --bootstrap-server localhost:9092 --topic aitrip.order.create --partitions 8 --replication-factor 1

docker exec aitrip-kafka kafka-topics --create --if-not-exists \
  --bootstrap-server localhost:9092 --topic aitrip.notify.send --partitions 4 --replication-factor 1
```

### 验证 Topic 创建

访问 Kafka UI：http://localhost:8090 → Topics 页面查看。

---

## 8. 应用服务编译与启动

### 8.1 全量编译

```bash
cd aitrip
mvn clean install -DskipTests
```

期望输出：
```
[INFO] AiTrip Common ...................................... SUCCESS
[INFO] AiTrip Backend ..................................... SUCCESS
[INFO] AiTrip Agent ....................................... SUCCESS
[INFO] BUILD SUCCESS
```

### 8.2 启动 aitrip-backend（端口 8080）

**方式一：IntelliJ IDEA（推荐）**

1. 打开 `aitrip-backend/src/main/java/com/aitrip/backend/AiTripBackendApplication.java`
2. 点击 `main` 方法旁的绿色运行箭头
3. 等待控制台输出：`Started AiTripBackendApplication in X.XXX seconds`

**方式二：命令行**

```bash
# Windows CMD（新开一个窗口）
C:\develop\Java\jdk-21\bin\java.exe -jar aitrip-backend\target\aitrip-backend-1.0.0-SNAPSHOT.jar
```

### 8.3 启动 aitrip-agent（端口 8081）

**方式一：IntelliJ IDEA（推荐）**

1. 打开 `aitrip-agent/src/main/java/com/aitrip/agent/AiTripAgentApplication.java`
2. 点击 `main` 方法旁的绿色运行箭头
3. 等待控制台输出：`Started AiTripAgentApplication in X.XXX seconds`

**方式二：命令行**

```bash
# Windows CMD（新开一个窗口）
C:\develop\Java\jdk-17\bin\java.exe -jar aitrip-agent\target\aitrip-agent-1.0.0-SNAPSHOT.jar
```

### 8.4 验证服务启动

```powershell
# PowerShell 验证
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8081/actuator/health
```

期望响应：
```json
{"status": "UP", "components": {...}}
```

---

## 9. 接口说明与功能测试

### 9.1 健康检查

```powershell
# Backend 健康
Invoke-RestMethod http://localhost:8080/actuator/health

# Agent 健康
Invoke-RestMethod http://localhost:8081/actuator/health
```

---

### 9.2 景区接口（Backend）

#### 按关键词搜索景区

```powershell
Invoke-RestMethod "http://localhost:8080/api/attractions/search?keyword=故宫"
Invoke-RestMethod "http://localhost:8080/api/attractions/search?city=北京&keyword=长城"
```

期望响应：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {"id": 1, "name": "故宫博物院", "city": "北京", "score": 4.9, ...}
  ]
}
```

#### 查询景区详情

```powershell
Invoke-RestMethod "http://localhost:8080/api/attractions/1"
```

#### 查询景区票务库存

```powershell
Invoke-RestMethod "http://localhost:8080/api/attractions/1/stock"
```

期望响应：
```json
{"code": 200, "data": {"attractionId": 1, "stock": 100, "available": true}}
```

---

### 9.3 秒杀接口（Backend）

#### Step 1：初始化秒杀库存

```powershell
$body = '{"voucherId": 1, "stock": 100}'
Invoke-RestMethod -Uri "http://localhost:8080/api/seckill/init-stock" `
    -Method POST -Body $body -ContentType "application/json"
```

#### Step 2：预校验秒杀资格

```powershell
Invoke-RestMethod "http://localhost:8080/api/seckill/pre-check?voucherId=1&userId=1001"
```

期望响应（有资格）：
```json
{"code": 200, "data": "{\"eligible\": true, \"stock\": 100}"}
```

#### Step 3：执行秒杀

```powershell
$body = '{"voucherId": 1, "userId": 1001}'
Invoke-RestMethod -Uri "http://localhost:8080/api/seckill/execute" `
    -Method POST -Body $body -ContentType "application/json"
```

期望响应：
```json
{"code": 200, "data": {"orderId": 123456789, "message": "抢票成功！订单正在处理中"}}
```

#### 防超卖验证（同用户重复购买）

```powershell
# 第一次成功，第二次应返回业务错误
$body = '{"voucherId": 1, "userId": 1001}'
Invoke-RestMethod -Uri "http://localhost:8080/api/seckill/execute" `
    -Method POST -Body $body -ContentType "application/json"
# 期望：code=400，message 包含 "已购买" 或 "库存不足"
```

---

### 9.4 AI 旅行规划接口（Agent）

#### SSE 流式规划（推荐，浏览器/curl 测试）

```bash
# 使用系统 curl.exe（Windows）
curl.exe -N "http://localhost:8081/api/agent/plan?message=帮我规划北京3日游&userId=1001&conversationId=session001"
```

SSE 响应格式：
```
data: 好的，我来为您规划北京3日游行程...
data: Day 1：天安门广场 → 故宫博物院 → 景山公园...
data: [DONE]
```

#### 同步规划（调试用）

```powershell
$body = '"帮我规划西安2日游，重点是历史文化景点"'
Invoke-RestMethod -Uri "http://localhost:8081/api/agent/plan/sync" `
    -Method POST -Body $body -ContentType "application/json"
```

#### 多轮对话（带 conversationId）

```bash
# 第一轮
curl.exe -N "http://localhost:8081/api/agent/plan?message=帮我规划北京3日游&userId=1001&conversationId=conv-001"

# 第二轮（继续追问，保持上下文）
curl.exe -N "http://localhost:8081/api/agent/plan?message=故宫需要提前多久预约？&userId=1001&conversationId=conv-001"
```

---

### 9.5 通知接口（Backend）

```powershell
# 查询通知列表
Invoke-RestMethod "http://localhost:8080/api/notify/list?userId=1001"

# 标记通知已读
$body = '{"notifyId": 1, "userId": 1001}'
Invoke-RestMethod -Uri "http://localhost:8080/api/notify/read" `
    -Method POST -Body $body -ContentType "application/json"
```

---

## 10. 集成测试运行

> ⚠️ 集成测试需要 Docker 基础设施（MySQL + Redis + Kafka）处于运行状态。

### 10.1 运行所有集成测试

```bash
mvn test -pl aitrip-backend
```

### 10.2 单独运行指定测试

```bash
# 秒杀并发超卖测试（200 线程并发抢 100 库存，验证无超卖）
mvn test -pl aitrip-backend -Dtest=SeckillServiceTest

# 多层缓存测试（布隆过滤器、空值防穿透、并发击穿防护）
mvn test -pl aitrip-backend -Dtest=MultiLevelCacheManagerTest

# Kafka 幂等消费测试（重复消息不重复落单）
mvn test -pl aitrip-backend -Dtest=SeckillOrderConsumerTest
```

### 10.3 测试说明

| 测试类 | 验证场景 | 期望结果 |
|---|---|---|
| `SeckillServiceTest` | 200并发抢100库存 | 成功数 ≤ 100，Redis余量 ≥ 0 |
| `SeckillServiceTest` | 同用户重复购买 | 第二次抛出 BusinessException |
| `SeckillServiceTest` | 库存为0时购买 | 抛出 BusinessException |
| `MultiLevelCacheManagerTest` | 布隆过滤器拦截非法ID | 返回 null |
| `MultiLevelCacheManagerTest` | 空值缓存防穿透 | DB 只查询一次 |
| `MultiLevelCacheManagerTest` | 50线程并发击穿 | DB 只查询一次 |
| `SeckillOrderConsumerTest` | 正常消息落单 | 幂等记录写入成功 |
| `SeckillOrderConsumerTest` | 重复消息过滤 | 订单表只有一条记录 |

---

## 11. 监控与运维

### 11.1 Actuator 端点

```powershell
# Backend 监控端点
Invoke-RestMethod http://localhost:8080/actuator/health    # 健康状态
Invoke-RestMethod http://localhost:8080/actuator/info      # 应用信息
Invoke-RestMethod http://localhost:8080/actuator/metrics   # 性能指标

# Agent 监控端点
Invoke-RestMethod http://localhost:8081/actuator/health
```

### 11.2 Kafka 消息监控

访问 Kafka UI：**http://localhost:8090**

- **Topics**：查看各 Topic 消息堆积量
- **Consumer Groups**：查看消费进度（lag）
- **Messages**：实时查看消息内容

关键监控指标：

| Topic | 正常 lag | 告警阈值 |
|---|---|---|
| `aitrip.ticket.seckill` | < 1000 | > 5000 |
| `aitrip.ticket.seckill.dlq` | = 0 | > 0（有死信需人工处理）|

### 11.3 Redis 状态查看

```bash
# 查看秒杀库存
docker exec aitrip-redis redis-cli -a aitrip123 GET seckill:stock:1

# 查看布隆过滤器
docker exec aitrip-redis redis-cli -a aitrip123 EXISTS bloom:attraction

# 查看所有 seckill 相关 Key
docker exec aitrip-redis redis-cli -a aitrip123 KEYS seckill:*
```

### 11.4 日志级别

应用日志默认配置：
- `com.aitrip.*`：DEBUG 级别（开发模式）
- `org.springframework.kafka`：INFO 级别
- `org.springframework.ai`：INFO 级别（agent 模块）

生产环境建议将 `com.aitrip` 改为 INFO：

```yaml
# application.yml
logging:
  level:
    com.aitrip: INFO
```

---

## 12. 常见问题排查

### Q1：启动报 `UnsupportedClassVersionError: class file version 65.0`

**原因**：编译时使用了 JDK 21，但运行时 JVM 是 JDK 17。

**解决**：
```bash
# 确认 JAVA_HOME 指向 JDK 17
java -version  # 应输出 17.x.x

# 重新用 JDK 17 编译
mvn clean package -DskipTests
```

### Q2：启动报 `OpenAI API key must be set`（aitrip-agent）

**原因**：`spring-ai-openai-spring-boot-starter` 自动装配要求 API key。

**解决**：确认 `aitrip-agent/src/main/resources/application.yml` 中已配置：
```yaml
spring:
  ai:
    openai:
      api-key: your-api-key-here
      base-url: https://dashscope.aliyuncs.com/compatible-mode
```

### Q3：启动报 `No qualifying bean of type 'SnowflakeIdGenerator'`

**原因**：`@SpringBootApplication` 默认只扫描当前包，未扫描 `aitrip-common`。

**解决**：确认 `AiTripBackendApplication` 有以下注解：
```java
@SpringBootApplication(scanBasePackages = {"com.aitrip.backend", "com.aitrip.common"})
```

### Q4：景区搜索返回 500 错误

**原因**：`city` 参数为空时 SQL 报错。

**解决**：已修复，`city` 参数为可选项，可不传：
```powershell
# 正确写法（不传 city 也可以）
Invoke-RestMethod "http://localhost:8080/api/attractions/search?keyword=故宫"
```

### Q5：`BloomFilterInitializer` 初始化失败

**原因**：数据库表不存在或连接失败。

**表现**：启动日志中有 `WARN [BloomFilter] 景区布隆过滤器初始化失败`，但**不影响服务启动**。

**解决**：确认 Docker MySQL 容器健康，并且初始化 SQL 已执行完毕。

### Q6：Kafka Consumer 消费失败

**原因**：Topic 不存在或序列化配置错误。

**解决**：
```bash
# 检查 Topic 是否存在
docker exec aitrip-kafka kafka-topics --list --bootstrap-server localhost:9092

# 重新执行初始化脚本
bash scripts/init-kafka-topics.sh
```

### Q7：SSE 接口无响应

**原因**：PowerShell `curl` 是 `Invoke-WebRequest` 的别名，不支持 SSE。

**解决**：使用系统 `curl.exe`：
```bash
curl.exe -N "http://localhost:8081/api/agent/plan?message=帮我规划北京3日游&userId=1001"
```

---

## 13. 项目关闭与清理

### 13.1 停止应用服务

- **IntelliJ IDEA**：点击工具栏红色停止按钮
- **命令行**：在对应 CMD 窗口按 `Ctrl+C`
- **强制终止（PowerShell）**：

```powershell
# 终止所有 Java 进程
Get-Process java | Stop-Process -Force
```

### 13.2 停止基础设施容器

```bash
cd aitrip/docker

# 停止容器（保留数据卷）
docker compose stop

# 停止并删除容器（保留数据卷）
docker compose down

# 停止并删除容器及数据卷（完全清理）
docker compose down -v
```

### 13.3 清理编译产物

```bash
cd aitrip
mvn clean
```

### 13.4 清理 Redis 测试数据

```bash
# 仅清理秒杀相关 Key（不影响其他数据）
docker exec aitrip-redis redis-cli -a aitrip123 --scan --pattern 'seckill:*' | \
  xargs docker exec aitrip-redis redis-cli -a aitrip123 DEL

# 清空全部缓存（谨慎操作）
docker exec aitrip-redis redis-cli -a aitrip123 FLUSHALL
```

---

## 附录：项目配置速查

### 关键配置文件

| 文件路径 | 说明 |
|---|---|
| `aitrip-backend/src/main/resources/application.yml` | Backend 主配置（DB、Redis、Kafka）|
| `aitrip-backend/src/main/resources/redisson.yml` | Redisson 连接配置 |
| `aitrip-backend/src/main/resources/sharding.yml` | ShardingSphere 分库分表配置 |
| `aitrip-backend/src/main/resources/lua/seckill.lua` | 秒杀原子 Lua 脚本 |
| `aitrip-backend/src/main/resources/lua/token_bucket.lua` | 令牌桶限流 Lua 脚本 |
| `aitrip-agent/src/main/resources/application.yml` | Agent 主配置（AI、Redis、MCP）|
| `aitrip-agent/src/main/resources/redisson.yml` | Agent Redisson 配置 |
| `docker/docker-compose.yml` | 基础设施编排 |

### Redis Key 规范

| Key 前缀 | 说明 | 示例 |
|---|---|---|
| `seckill:stock:{voucherId}` | 秒杀库存 | `seckill:stock:1` |
| `seckill:qualify:{voucherId}` | 已购用户集合 | `seckill:qualify:1` |
| `cache:attraction:{id}` | 景区缓存 | `cache:attraction:100` |
| `bloom:attraction` | 景区布隆过滤器 | — |
| `bloom:voucher` | 优惠券布隆过滤器 | — |
| `lock:rebuild:{key}` | 缓存重建分布式锁 | — |

### Kafka Topic 规范

| Topic | 分区数 | 说明 |
|---|---|---|
| `aitrip.ticket.seckill` | 8 | 秒杀消息主队列 |
| `aitrip.ticket.seckill.dlq` | 2 | 秒杀死信队列 |
| `aitrip.order.create` | 8 | 订单创建队列 |
| `aitrip.notify.send` | 4 | 通知发送队列 |

### 端口速查

| 服务 | 端口 | 说明 |
|---|---|---|
| aitrip-backend | 8080 | 后端 REST API |
| aitrip-agent | 8081 | AI Agent SSE API |
| MySQL 主库 | 3308 | 主业务数据库 |
| MySQL 分库 | 3307 | 订单分库 |
| Redis | 6379 | 缓存 + 分布式锁 |
| Kafka | 9092 | 消息队列 |
| Kafka UI | 8090 | 消息监控面板 |

---

*AiTrip — Powered by Spring AI + Spring Boot 3 + Apache Kafka*
  