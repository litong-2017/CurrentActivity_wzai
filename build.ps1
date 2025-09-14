Write-Host "========================================" -ForegroundColor Green
Write-Host "CurrentActivity 自动构建脚本" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# 检查 Gradle Wrapper
if (-not (Test-Path ".\gradlew.bat")) {
    Write-Host "错误: gradlew.bat 文件不存在！" -ForegroundColor Red
    exit 1
}

try {
    # 清理项目
    Write-Host "清理项目..." -ForegroundColor Yellow
    & .\gradlew.bat clean
    
    # 构建 Debug 版本
    Write-Host "构建 Debug 版本..." -ForegroundColor Yellow
    & .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) { throw "Debug 构建失败" }
    
    # 构建 Release 版本
    Write-Host "构建 Release 版本..." -ForegroundColor Yellow
    & .\gradlew.bat assembleRelease
    if ($LASTEXITCODE -ne 0) { throw "Release 构建失败" }
    
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "构建完成！" -ForegroundColor Green
    Write-Host "Debug APK: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
    Write-Host "Release APK: app\build\outputs\apk\release\app-release.apk" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Green
    
    # 显示文件大小
    if (Test-Path "app\build\outputs\apk\debug\app-debug.apk") {
        $debugSize = (Get-Item "app\build\outputs\apk\debug\app-debug.apk").Length / 1MB
        Write-Host "Debug APK 大小: $([math]::Round($debugSize, 2)) MB" -ForegroundColor Cyan
    }
    
    if (Test-Path "app\build\outputs\apk\release\app-release.apk") {
        $releaseSize = (Get-Item "app\build\outputs\apk\release\app-release.apk").Length / 1MB
        Write-Host "Release APK 大小: $([math]::Round($releaseSize, 2)) MB" -ForegroundColor Cyan
    }
    
} catch {
    Write-Host "构建失败: $_" -ForegroundColor Red
    exit 1
}