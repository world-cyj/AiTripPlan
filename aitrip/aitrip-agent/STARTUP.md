# AiTrip Agent 启动说明

## Agent 服务无法启动的常见原因

### 原因1：Spring AI Alibaba 依赖未安装

Agent 使用 `spring-ai-openai-spring-boot-starter` 作为占位，
实际调用阿里云 DashScope，需要配置正确的 API Key。

检查 `aitrip-agent/src/main/resources/application.yml`：
```yaml
spring:
  ai:
    openai:
      api-key: sk-xxxxxxxx  # ← 替换为你的 DashScope API Key
      base-url: https://dashscope.aliyuncs.com/compatible-mode
```

获取 API Key：https://dashscope.aliyun.com/

---

### 原因2：MCP Server 配置冲突

`application.yml` 中有：
```yaml
spring:
  ai:
    mcp:
      server:
        transport: STDIO  # ← 这会导致 Web 服务无法正常启动
```

**修复方法**：将 transport 改为 `HTTP` 或注释掉 MCP server 配置。

---

### 原因3：Redis 连接失败

Agent 需要 Redis 存储对话记忆，确保 Docker Redis 正在运行：
```powershell
docker ps | findstr redis
```

---

## 在 IDEA 中启动

1. 打开 `aitrip-agent` 模块
2. 找到 `AiTripAgentApplication.java`
3. 右键 → Run
4. 控制台出现 `Started AiTripAgentApplication on port 8081` 即成功

## 验证

```powershell
curl.exe http://localhost:8081/actuator/health
```

期望返回：`{"status":"UP"}`

---

## 临时方案：如果 Agent 无法启动

前端 AI 规划页会显示「Agent 未连接」提示。
可以直接测试后端接口验证其他功能（秒杀、景区、订单等）。
