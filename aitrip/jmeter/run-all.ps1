# run-all.ps1 — AiTrip JMeter 一键压测脚本
# 使用方式: .\jmeter\run-all.ps1 [-Scene 1] [-Host localhost] [-Port 8083]

param(
    [int]$Scene = 0,          # 0=全部场景, 1-4=指定场景
    [string]$Host = "localhost",
    [int]$Port = 8083,
    [string]$JmeterHome = "C:\apache-jmeter-5.6.3"
)

$JmeterBin = "$JmeterHome\bin\jmeter.bat"
$TestPlan  = "$PSScriptRoot\aitrip-test-plan.jmx"
$ResultDir = "$PSScriptRoot\results"

# 确认 JMeter 存在
if (-not (Test-Path $JmeterBin)) {
    Write-Error "JMeter 未找到: $JmeterBin，请修改 -JmeterHome 参数"
    exit 1
}

# 确认服务健康
Write-Host "[INFO] 检查后端服务健康状态..."
try {
    $health = Invoke-RestMethod "http://${Host}:${Port}/actuator/health" -TimeoutSec 5
    if ($health.status -ne "UP") {
        Write-Error "服务状态异常: $($health.status)"
        exit 1
    }
    Write-Host "[OK] 服务健康: $($health.status)"
} catch {
    Write-Error "无法连接到服务 http://${Host}:${Port}，请先启动 aitrip-backend"
    exit 1
}

# 初始化秒杀库存
Write-Host "[INFO] 初始化秒杀库存 (voucherId=1001, stock=100)..."
try {
    $body = '{"voucherId": 1001, "stock": 100}'
    Invoke-RestMethod -Uri "http://${Host}:${Port}/api/seckill/init-stock" `
        -Method POST -Body $body -ContentType "application/json" | Out-Null
    Write-Host "[OK] 库存初始化完成"
} catch {
    Write-Warning "库存初始化失败（可能已初始化）: $_"
}

# 创建结果目录
New-Item -ItemType Directory -Force -Path $ResultDir | Out-Null

function Run-Scene {
    param([int]$SceneNum, [string]$Name, [string]$ResultFile)
    Write-Host ""
    Write-Host "========================================"
    Write-Host "[SCENE $SceneNum] $Name"
    Write-Host "========================================"
    
    $jtl  = "$ResultDir\$ResultFile.jtl"
    $html = "$ResultDir\$ResultFile-html"
    
    # 清理旧结果
    if (Test-Path $jtl)  { Remove-Item $jtl -Force }
    if (Test-Path $html) { Remove-Item $html -Recurse -Force }
    
    # 运行 JMeter
    $args = @(
        "-n", "-t", $TestPlan,
        "-l", $jtl,
        "-e", "-o", $html,
        "-Jhost=$Host",
        "-Jbackend_port=$Port",
        "-Jscene=$SceneNum"
    )
    Write-Host "[INFO] 运行中..."
    $start = Get-Date
    & $JmeterBin @args 2>&1 | Where-Object { $_ -match 'summary|ERROR|WARN' }
    $elapsed = (Get-Date) - $start
    
    Write-Host "[OK] 场景$SceneNum 完成，耗时: $($elapsed.ToString('mm\:ss'))"
    Write-Host "[INFO] JTL 结果: $jtl"
    Write-Host "[INFO] HTML 报告: $html\index.html"
}

# 执行场景
switch ($Scene) {
    0 {
        Run-Scene 1 "秒杀接口(500并发/60s/库存100)" "seckill-result"
        Run-Scene 2 "缓存穿透(200并发/随机非法ID)" "bloom-result"
        Run-Scene 3 "景区搜索(300并发)" "search-result"
        Run-Scene 4 "热榜接口(200并发)" "rank-result"
    }
    1 { Run-Scene 1 "秒杀接口" "seckill-result" }
    2 { Run-Scene 2 "缓存穿透" "bloom-result" }
    3 { Run-Scene 3 "景区搜索" "search-result" }
    4 { Run-Scene 4 "热榜接口" "rank-result" }
    default { Write-Error "无效场景号: $Scene (有效值: 0-4)" }
}

# 压测后验证
Write-Host ""
Write-Host "========== 压测后验证 =========="

# 验证秒杀无超卖
try {
    $stock = docker exec aitrip-redis redis-cli -a aitrip123 GET seckill:stock:1001 2>&1
    Write-Host "[验证] Redis 剩余库存: $stock (应 >= 0)"
} catch {
    Write-Warning "无法获取 Redis 库存: $_"
}

Write-Host ""
Write-Host "[完成] 所有压测场景执行完毕！"
Write-Host "[提示] 用浏览器打开 HTML 报告查看详细数据"
