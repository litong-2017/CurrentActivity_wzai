@echo off
echo ========================================
echo CurrentActivity 自动构建脚本
echo ========================================

echo 清理项目...
call gradlew clean

echo 构建 Debug 版本...
call gradlew assembleDebug
if %errorlevel% neq 0 (
    echo Debug 构建失败!
    pause
    exit /b 1
)

echo 构建 Release 版本...
call gradlew assembleRelease
if %errorlevel% neq 0 (
    echo Release 构建失败!
    pause
    exit /b 1
)

echo ========================================
echo 构建完成！
echo Debug APK: app\build\outputs\apk\debug\app-debug.apk
echo Release APK: app\build\outputs\apk\release\app-release.apk
echo ========================================

pause