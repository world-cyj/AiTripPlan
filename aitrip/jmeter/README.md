# JMeter 压测使用指南

## 目录结构

```
jmeter/
├── aitrip-test-plan.jmx   # JMeter 测试计划（4个场景）
├── data/
│   ├── cities.csv          # 城市数据集（场景3搜索用）
│   └── users.csv           # 用户数据集
└── results/                # 压测结果输出目录
    ├── seckill-result.jtl  # 场景1: 秒杀结果
    ├── bloom-result.jtl    # 场景2: 缓存穿透结果
    ├── search-result.jtl   # 场景3: 搜索结果
    └── rank-result.jtl     # 场景4: 热榜结果
```

## 压测场景说明

| 场景 | 接口 | 并发 | 时长/次数 | 预期指标 |
|---|---|---|---|---|
| 场景1：秒杀防超卖 | `POST /api/seckill/execute` | 500线程 | 60秒 | 成功≤100，无超卖，P99<500ms |
| 场景2：缓存穿透防护 | `GET /api/attractions/{随机非法ID}` | 200线程 | 50次/线程 | 布隆拦截>95%，DB零压力 |
| 场景3：景区搜索 | `GET /api/attractions/search` | 300线程 | 20次/线程 | P95<200ms，错误率<1% |
| 场景4：热榜接口 | `GET /api/rank/hot` | 200线程 | 30次/线程 | P95<100ms（Redis缓存命中） |

## 前置准备

### 1. 确认服务已启动

```powershell
Invoke-RestMethod http://localhost:8083/actuator/health
# 期望：status=UP
```

### 2. 初始化秒杀库存（场景1必须）

```powershell
$body = '{"voucherId": 1001, "stock": 100}'
Invoke-RestMethod -Uri http://localhost:8083/api/seckill/init-stock `
    -Method POST -Body $body -ContentType "application/json"
```

### 3. 确认 JMeter 已安装

```powershell
jmeter --version
# 需要 JMeter 5.6+
```

## 运行方式

### GUI 模式（推荐调试）

```bash
# 打开 JMeter GUI
jmeter -t jmeter/aitrip-test-plan.jmx
```

1. 打开后在左侧选择要运行的场景（其余场景右键 → Disable）
2. 点击绿色运行按钮
3. 查看「聚合报告」结果

### 命令行模式（推荐正式压测）

```bash
# 场景1：秒杀压测
jmeter -n -t jmeter/aitrip-test-plan.jmx \
  -l jmeter/results/seckill-result.jtl \
  -e -o jmeter/results/seckill-report \
  -Jhost=localhost -Jbackend_port=8083

# 生成 HTML 报告
jmeter -g jmeter/results/seckill-result.jtl \
  -o jmeter/results/seckill-html-report
```

### Windows PowerShell 一键运行

```powershell
# 见 jmeter/run-all.ps1
.\jmeter\run-all.ps1
```

## 压测结果分析

### 关键指标

| 指标 | 说明 | 秒杀目标 | 搜索目标 |
|---|---|---|---|
| TPS | 每秒事务数 | ≥ 500 | ≥ 1000 |
| P95 | 95%请求响应时间 | < 500ms | < 200ms |
| P99 | 99%请求响应时间 | < 1000ms | < 500ms |
| Error Rate | 错误率（HTTP非200） | < 0.1% | < 1% |
| 成功订单数 | 秒杀成功数量 | ≤ 100（库存上限） | — |

### 验证无超卖

压测结束后执行：

```powershell
# 查看 Redis 剩余库存（应 ≥ 0）
docker exec aitrip-redis redis-cli -a aitrip123 GET seckill:stock:1001

# 查看数据库订单数（应 ≤ 100）
docker exec aitrip-mysql-shard mysql -uroot -paitrip123 hmdp_0 `
    -e "SELECT COUNT(*) as order_count FROM tb_voucher_order WHERE voucher_id=1001;"
```

### 验证缓存穿透防护

```powershell
# 查看 Redis 命中数（应接近0 DB查询）
docker exec aitrip-redis redis-cli -a aitrip123 INFO stats | grep keyspace_hits

# 查看 MySQL 慢查询日志（应为0）
docker exec aitrip-mysql mysql -uroot -paitrip123 -e "SHOW STATUS LIKE 'Slow_queries';"
```

## 常见问题

### Q: 场景1运行时出现大量 Connection refused

**原因**：并发500线程超过 Tomcat 最大连接数。  
**解决**：在 `application.yml` 调整：
```yaml
server:
  tomcat:
    max-connections: 8192
    threads:
      max: 400
      min-spare: 50
    accept-count: 1000
```

### Q: 场景2 布隆过滤器未生效（DB仍有压力）

**原因**：布隆过滤器启动时初始化失败。  
**排查**：查看启动日志中是否有 `[BloomFilter] 景区布隆过滤器初始化完成`。

### Q: JMeter CSV 文件找不到

**原因**：JMeter 工作目录不在项目根目录。  
**解决**：在 JMeter GUI 中将 CSV 路径改为绝对路径：
```
C:\Users\admin\Desktop\aitrip\jmeter\data\cities.csv
```
