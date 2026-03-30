# AiTrip HTTP 接口测试说明

## 工具要求

**VS Code**：安装 [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client) 插件  
**IDEA**：直接支持 `.http` 文件，无需插件  

打开任意 `.http` 文件，点击请求上方的 `Send Request` 即可发送。

---

## 文件说明

| 文件 | 接口模块 | 说明 |
|---|---|---|
| `00-variables.http` | 全局变量 | 配置 baseUrl、token、ID 等 |
| `01-user-auth.http` | 用户认证 | 登录、退出、用户信息、旅行偏好 |
| `02-attractions.http` | 景区管理 | 搜索、详情、库存查询 |
| `03-seckill.http` | 票务秒杀 | 初始化库存、预检资格、执行秒杀 |
| `04-orders.http` | 订单管理 | 列表、详情、取消订单 |
| `05-rank.http` | 热榜系统 | 全站热榜、城市热榜、访问埋点 |
| `06-notify.http` | 订阅通知 | 订阅、取消订阅、排队位置 |
| `07-e2e-flow.http` | 端到端流程 | 完整业务链路测试 |
| `08-system.http` | 系统接口 | 健康检查、压测场景 |

---

## 快速开始

### 第一步：登录获取 Token

打开 `01-user-auth.http`，执行「登录 - 账号1」请求：

```json
// 请求
POST http://localhost:8080/api/user/login
{ "phone": "13800000001", "code": "123456" }

// 响应示例
{
  "code": 200,
  "message": "success",
  "data": "{\"userId\":1,\"nickName\":\"用户_0001\",\"token\":\"abc123...\",\"level\":1}"
}
```

从响应 `data` 字段解析出 `token` 和 `userId`。

### 第二步：填入 Token

在需要认证的 `.http` 文件顶部修改：
```
@token = 粘贴你的token
@userId = 1
```

### 第三步：按顺序执行（推荐用 07-e2e-flow.http）

---

## 接口速查表

### 用户认证
```
POST /api/user/login              登录（返回token）
POST /api/user/logout             退出登录
GET  /api/user/{id}               用户信息
GET  /api/user/{id}/preference    旅行偏好
POST /api/user/{id}/preference    更新偏好
```

### 景区
```
GET /api/attractions/search?city=&keyword=   搜索景区
GET /api/attractions/{id}                    景区详情（多层缓存）
GET /api/attractions/{id}/stock              门票库存
```

### 秒杀
```
POST /api/seckill/init-stock    初始化Redis库存
GET  /api/seckill/pre-check     预检资格（不扣库存）
POST /api/seckill/execute       执行秒杀（Lua原子+Kafka异步）
```

### 订单
```
GET  /api/orders/user/{userId}      用户订单列表
GET  /api/orders/{orderId}          订单详情
POST /api/orders/{orderId}/cancel   取消订单（库存回补）
```

### 热榜
```
GET  /api/rank/hot?topN=10          全站热榜（Redis ZSet）
GET  /api/rank/city/{city}?topN=5   城市热榜
POST /api/rank/visit?attractionId&userId  记录访问热度
```

### 订阅通知
```
POST   /api/notify/subscribe?voucherId&userId   订阅到票提醒
DELETE /api/notify/subscribe?voucherId&userId   取消订阅
GET    /api/notify/position?voucherId&userId    查询排队位置
```

### 系统
```
GET /actuator/health   健康检查
```

---

## 测试账号

| 手机号 | 验证码 | 说明 |
|---|---|---|
| 13800000001 | 123456 | 测试账号1 |
| 13800000002 | 123456 | 测试账号2 |
| 13800000003 | 123456 | 测试账号3 |

---

## 常见问题

**Q: 请求返回 "无法连接"**  
A: 后端服务未启动，在 IDEA 运行 `AiTripBackendApplication`

**Q: 秒杀返回 "库存不足"**  
A: 先执行 `03-seckill.http` 中的「初始化库存」请求

**Q: 热榜返回空数组**  
A: 先执行 `08-system.http` 中的「批量访问记录」请求填充热度数据

**Q: 登录返回 data 是字符串而非对象**  
A: 正常，后端统一返回 JSON 字符串，需要二次 JSON.parse 解析
