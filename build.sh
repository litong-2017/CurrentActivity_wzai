#!/bin/bash

echo "========================================"
echo "CurrentActivity 自动构建脚本"
echo "========================================"

# 检查 Gradle Wrapper 是否存在
if [ ! -f "./gradlew" ]; then
    echo "错误: gradlew 文件不存在！"
    exit 1
fi

# 清理项目
echo "清理项目..."
./gradlew clean

# 构建 Debug 版本
echo "构建 Debug 版本..."
./gradlew assembleDebug
if [ $? -ne 0 ]; then
    echo "Debug 构建失败!"
    exit 1
fi

# 构建 Release 版本
echo "构建 Release 版本..."
./gradlew assembleRelease
if [ $? -ne 0 ]; then
    echo "Release 构建失败!"
    exit 1
fi

echo "========================================"
echo "构建完成！"
echo "Debug APK: app/build/outputs/apk/debug/app-debug.apk"
echo "Release APK: app/build/outputs/apk/release/app-release.apk"
echo "========================================"

# 显示 APK 信息
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    DEBUG_SIZE=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
    echo "Debug APK 大小: $DEBUG_SIZE"
fi

if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    RELEASE_SIZE=$(du -h app/build/outputs/apk/release/app-release.apk | cut -f1)
    echo "Release APK 大小: $RELEASE_SIZE"
fi