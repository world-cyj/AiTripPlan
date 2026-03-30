# 重启后端并验证 CORS 脚本
# 在 PowerShell 中执行此脚本

Write-Host "=== 步骤1: 停止现有 Java 进程 ===" -ForegroundColor Yellow
$javaProcs = Get-Process java -ErrorAction SilentlyContinue
if ($javaProcs) {
    $javaProcs | ForEach-Object {
        Write-Host "停止 Java 进程 PID: $($_.Id)"
        Stop-Process -Id $_.Id -Force
    }
    Start-Sleep -Seconds 2
    Write-Host "Java 进程已停止" -ForegroundColor Green
} else {
    Write-Host "没有运行中的 Java 进程" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== 步骤2: 设置 JDK 21 环境 ===" -ForegroundColor Yellow
$jdkPath = "C:\develop\Java\jdk-21"
if (Test-Path $jdkPath) {
    $env:JAVA_HOME = $jdkPath
    $env:Path = "$jdkPath\bin;$env:Path"
    $ver = & java -version 2>&1 | Select-Object -First 1
    Write-Host "JDK: $ver" -ForegroundColor Green
} else {
    Write-Host "未找到 $jdkPath，使用系统默认 Java" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== 步骤3: 启动后端（后台运行）===" -ForegroundColor Yellow
$projectDir = "C:\Users\admin\Desktop\aitrip"
Set-Location $projectDir

# 后台启动，日志输出到文件
$logFile = "$projectDir\backend.log"
Write-Host "日志文件: $logFile"
$proc = Start-Process -FilePath "mvn" `
    -ArgumentList "spring-boot:run", "-pl", "aitrip-backend", "-am", "-q" `
    -WorkingDirectory $projectDir `
    -RedirectStandardOutput $logFile `
    -RedirectStandardError "$projectDir\backend-err.log" `
    -PassThru -NoNewWindow

Write-Host "后端启动中 (PID: $($proc.Id))，等待30秒..." -ForegroundColor Yellow

# 等待启动
$started = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    $content = Get-Content $logFile -ErrorAction SilentlyContinue
    if ($content -match "Started AiTripBackendApplication") {
        $started = $true
        break
    }
    if ($content -match "APPLICATION FAILED") {
        Write-Host "启动失败！查看日志: $logFile" -ForegroundColor Red
        break
    }
    Write-Host "." -NoNewline
}

Write-Host ""
if ($started) {
    Write-Host "后端启动成功！" -ForegroundColor Green
} else {
    Write-Host "等待超时，请手动检查日志: $logFile" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== 步骤4: 验证接口和CORS ===" -ForegroundColor Yellow
Start-Sleep -Seconds 3

try {
    $health = Invoke-RestMethod "http://localhost:8080/actuator/health" -TimeoutSec 5
    Write-Host "健康检查: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "健康检查失败: $_" -ForegroundColor Red
}

try {
    $result = Invoke-WebRequest "http://localhost:8080/api/attractions/search?city=&keyword=" `
        -Headers @{"Origin"="null"; "Access-Control-Request-Method"="GET"} `
        -Method OPTIONS -TimeoutSec 5
    $corsHeader = $result.Headers["Access-Control-Allow-Origin"]
    if ($corsHeader) {
        Write-Host "CORS 配置正常: Access-Control-Allow-Origin = $corsHeader" -ForegroundColor Green
    } else {
        Write-Host "CORS 响应头未找到，请检查 WebMvcConfig" -ForegroundColor Red
    }
} catch {
    Write-Host "CORS 检查: $_" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== 完成！现在可以打开前端 ==="  -ForegroundColor Cyan
Write-Host "前端路径: C:\Users\admin\Desktop\aitrip\aitrip-frontend\index.html" -ForegroundColor Cyan
