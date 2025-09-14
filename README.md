# CurrentActivity - 当前Activity显示工具

## 项目简介

CurrentActivity 是一个Android开发调试工具应用，主要功能是通过悬浮窗实时显示当前运行的Activity信息（包名和类名）。这个工具对Android开发者来说非常有用，可以帮助快速识别当前正在运行的Activity，方便调试和开发工作。

## 主要功能

### 🔍 实时Activity监控
- 通过Android无障碍服务（AccessibilityService）监听窗口状态变化
- 实时捕获并显示当前Activity的包名和类名
- 悬浮窗显示，不影响正常操作

### 🎛️ 灵活的控制选项
- 一键开启/关闭悬浮窗显示
- 显示/隐藏悬浮窗（保持服务运行但隐藏界面）
- 通知栏快捷操作按钮

### 🔐 权限管理
- 智能检测并提示所需权限
- 悬浮窗权限检查（Android 6.0+）
- 通知栏权限检查（Android 4.4+）
- 无障碍服务权限引导

## 技术架构

### 核心组件

#### 1. MAccessibilityService（无障碍服务）
- **功能**：监听系统窗口状态变化事件
- **权限**：需要用户手动授权无障碍服务权限
- **工作原理**：
  - 监听 `TYPE_WINDOW_STATE_CHANGED` 事件
  - 获取当前Activity的包名和类名
  - 更新悬浮窗显示内容
  - 维护前台服务保持运行

#### 2. WindowViewContainer（悬浮窗容器）
- **功能**：管理悬浮窗的创建、显示、隐藏和销毁
- **特性**：
  - 单例模式设计，确保全局唯一
  - 支持Android不同版本的窗口类型适配
  - 不获取焦点，不影响用户正常操作
  - 自适应内容大小

#### 3. MainActivity（主界面）
- **功能**：应用主控制界面
- **特性**：
  - 权限状态检查和引导
  - 悬浮窗开关控制
  - 权限设置跳转
  - UI状态实时更新

#### 4. 工具类
- **PermissionUtil**：权限检查工具类
- **NotificationUtil**：通知栏管理工具类
- **ActivityUtil**：Activity跳转工具类
- **DialogUtil**：对话框工具类

### 权限处理

#### 无障碍服务权限
```xml
<service
    android:name=".service.MAccessibilityService"
    android:label="@string/service_name"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility"/>
</service>
```

#### 悬浮窗权限
```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.SYSTEM_OVERLAY_WINDOW"/>
```

#### 版本适配
- **Android 6.0 (API 23)** 以下：自动获得悬浮窗权限
- **Android 6.0 (API 23)** 以上：需要用户手动授权悬浮窗权限
- **Android 8.0 (API 26)** 以上：使用 `TYPE_APPLICATION_OVERLAY` 窗口类型
- **Android 7.0 (API 24)** 以上：支持程序化关闭无障碍服务

## 依赖配置更新

### 原版本依赖（已过时）
```groovy
android {
    compileSdkVersion 26
    defaultConfig {
        minSdkVersion 15
        targetSdkVersion 26
    }
}

dependencies {
    implementation 'com.android.support:appcompat-v7:26.0.0-beta1'
    implementation 'com.android.support.constraint:constraint-layout:1.0.2'
    testImplementation 'junit:junit:4.12'
    androidTestImplementation 'com.android.support.test:runner:0.5'
    androidTestImplementation 'com.android.support.test.espresso:espresso-core:2.2.2'
}
```

### 更新后的依赖配置

#### 项目级 build.gradle
```groovy
buildscript {
    repositories {
        google()
        mavenCentral() // 替换已废弃的 jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.1.4' // 更新到稳定版本
    }
}

allprojects {
    repositories {
        google()
        mavenCentral() // 替换已废弃的 jcenter()
    }
}
```

#### 应用级 build.gradle
```groovy
android {
    compileSdk 34 // 更新到最新API
    
    defaultConfig {
        applicationId "com.wangnan.currentactivity"
        minSdk 21 // 提升最低支持版本，确保更好的兼容性
        targetSdk 34 // 更新目标版本
        versionCode 3
        versionName "2.1"
        
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    namespace 'com.wangnan.currentactivity'
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    
    // AndroidX 依赖 - 替换旧的 Support Library
    implementation('androidx.appcompat:appcompat:1.6.1') {
        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk7'
        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk8'
    }
    implementation('androidx.constraintlayout:constraintlayout:2.1.4') {
        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk7'
        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk8'
    }
    implementation('androidx.core:core:1.12.0') {
        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk7'
        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk8'
    }
    
    // 测试依赖
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation('androidx.test.ext:junit:1.1.5') {
        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk7'
        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk8'
    }
    androidTestImplementation('androidx.test.espresso:espresso-core:3.5.1') {
        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk7'
        exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk8'
    }
}
```

#### Gradle Wrapper 更新
```properties
# gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
```

## 构建和打包

### 1. 环境要求
- **Android Studio**: Electric Eel 2022.1.1+ (推荐最新版本)
- **Android SDK**: API 34+ (Android 14+)
- **Gradle**: 8.0+
- **Java**: JDK 8+ (推荐 JDK 17)
- **操作系统**: Windows 10+, macOS 10.14+, Ubuntu 18.04+

### 2. 环境配置

#### 安装 Android Studio
1. 下载并安装 [Android Studio](https://developer.android.com/studio)
2. 启动 Android Studio，通过 SDK Manager 安装：
   - Android SDK Platform 34
   - Android SDK Build-Tools 34.0.0
   - Android SDK Platform-Tools

#### 配置 Gradle（可选）
```bash
# Windows (PowerShell)
$env:GRADLE_OPTS = "-Xmx4g -Dfile.encoding=UTF-8"

# macOS/Linux
export GRADLE_OPTS="-Xmx4g -Dfile.encoding=UTF-8"
```

### 3. 项目导入和构建

#### 方法一：使用 Android Studio（推荐）
```bash
1. 打开 Android Studio
2. 选择 "Open an existing project"
3. 选择项目根目录
4. 等待 Gradle 同步完成
5. 点击 "Build" → "Build Bundle(s) / APK(s)" → "Build APK(s)"
```

#### 方法二：使用命令行
```bash
# 克隆项目（如果从远程获取）
git clone [项目地址]
cd CurrentActivity

# Windows (PowerShell)
.\gradlew clean assembleDebug

# macOS/Linux
./gradlew clean assembleDebug

# 构建 Release 版本
.\gradlew clean assembleRelease  # Windows
./gradlew clean assembleRelease  # macOS/Linux
```

### 4. 构建产物

构建完成后，APK 文件位于：
```
app/build/outputs/apk/
├── debug/
│   ├── app-debug.apk          # 调试版本
│   └── output-metadata.json   # 构建元数据
└── release/
    ├── app-release.apk        # 发布版本（未签名）
    └── output-metadata.json
```

### 5. 签名和发布

#### Debug 版本（自动签名）
- Debug APK 已使用 Android Studio 默认密钥自动签名
- 可直接安装到设备进行测试

#### Release 版本签名
```bash
# 生成签名密钥（首次）
keytool -genkey -v -keystore currentactivity.keystore -alias currentactivity -keyalg RSA -keysize 2048 -validity 10000

# 签名 APK
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore currentactivity.keystore app-release-unsigned.apk currentactivity

# 对齐优化
zipalign -v 4 app-release-unsigned.apk app-release.apk
```

#### 使用 Android Studio 签名
```bash
1. Build → Generate Signed Bundle / APK
2. 选择 APK
3. 创建或选择密钥库
4. 选择构建类型（release）
5. 完成签名
```

### 7. 自动化构建脚本

#### Windows 批处理脚本 (build.bat)
```batch
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
```

#### macOS/Linux 脚本 (build.sh)
```bash
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
```

#### PowerShell 脚本 (build.ps1)
```powershell
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
```

### 6. 安装和使用

#### 安装步骤
1. **下载 APK**：从构建产物中获取 `app-debug.apk` 或 `app-release.apk`
2. **允许未知来源**：在设备设置中允许安装未知来源应用
3. **安装应用**：点击 APK 文件进行安装

#### 首次配置
1. **启动应用**：打开 CurrentActivity 应用
2. **授权悬浮窗权限**：
   - 点击"悬浮窗权限"开关
   - 在弹出的系统设置页面中开启权限
3. **开启无障碍服务**：
   - 点击"关闭辅助服务"按钮
   - 在辅助功能设置中找到"CurrentActivity"
   - 开启服务
4. **配置通知权限**（可选）：
   - 点击"通知栏权限"开关
   - 在通知设置中开启权限

#### 使用功能
- **开启悬浮窗**：点击"打开悬浮窗"按钮
- **查看Activity信息**：悬浮窗实时显示当前Activity的包名和类名
- **通知栏控制**：通过通知栏快速切换悬浮窗显示状态
- **关闭服务**：点击"关闭辅助服务"或通过通知栏关闭

## 代码结构

```
app/src/main/java/com/wangnan/currentactivity/
├── ui/
│   ├── activity/
│   │   └── MainActivity.java          # 主界面Activity
│   └── window/
│       └── WindowViewContainer.java   # 悬浮窗容器管理
├── service/
│   └── MAccessibilityService.java     # 无障碍服务
├── receiver/
│   └── MAccessibilityServiceReceiver.java # 广播接收器
├── util/
│   ├── PermissionUtil.java           # 权限检查工具
│   ├── NotificationUtil.java         # 通知工具
│   ├── ActivityUtil.java             # Activity工具
│   └── DialogUtil.java               # 对话框工具
└── widget/
    └── CustomDialog.java             # 自定义对话框
```

## 关键技术点

### 1. 无障碍服务监听
```java
@Override
public void onAccessibilityEvent(AccessibilityEvent event) {
    if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
        String activityInfo = event.getPackageName() + "\n" + event.getClassName();
        mWindowViewContainer.updateWindowView(activityInfo);
    }
}
```

### 2. 悬浮窗创建
```java
// Android 8.0+ 适配
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
} else {
    mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
}
```

### 3. 权限检查
```java
// 悬浮窗权限检查
public static boolean hasOverlayPermission(Context context) {
    if (Build.VERSION.SDK_INT >= 23) {
        return Settings.canDrawOverlays(context);
    }
    return true;
}
```

## 注意事项

### 🚨 权限要求
- 本应用需要无障碍服务权限，请在系统设置中手动开启
- Android 6.0以上需要悬浮窗权限
- 通知栏权限建议开启以便更好的用户体验

### ⚠️ 兼容性
- 最低支持 Android 5.0 (API 21)
- 在不同厂商的ROM上可能存在权限限制
- 部分厂商可能需要额外的自启动或后台运行权限

### 🔧 开发建议
- 建议在开发环境中使用，正式发布应用前请关闭
- 长时间使用可能会消耗一定的系统资源
- 如遇到权限问题，建议查看系统的权限管理设置

## 常见问题和解决方案

### 构建问题

#### Gradle 同步失败
```bash
# 清理并重试
./gradlew clean
./gradlew --refresh-dependencies

# 检查网络和代理设置
./gradlew build --debug
```

#### 内存不足
```bash
# 在 gradle.properties 中增加内存
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
```

#### 依赖冲突
```bash
# 查看依赖树
./gradlew app:dependencies

# 强制刷新
./gradlew build --refresh-dependencies
```

### 运行问题

#### Android 15 兼容性
- **无障碍服务无法启动**：检查是否在辅助功能设置中正确开启
- **通知权限问题**：确保在应用设置中开启通知权限
- **悬浮窗权限拒绝**：在系统设置中手动授权悬浮窗权限

#### 权限问题
- **前台服务权限**：Android 9+ 需要 FOREGROUND_SERVICE 权限（已自动包含）
- **通知权限**：Android 13+ 需要 POST_NOTIFICATIONS 权限（已自动包含）
- **悬浮窗权限**：Android 6+ 需要用户手动授权

### 调试技巧

#### 查看日志
```bash
# 过滤应用日志
adb logcat | grep "CurrentActivity"

# 查看崩溃信息
adb logcat | grep "AndroidRuntime"
```

#### 权限检查
```bash
# 检查应用权限
adb shell dumpsys package com.wangnan.currentactivity

# 检查无障碍服务状态
adb shell settings get secure enabled_accessibility_services
```

## 更新日志

### v2.1 (当前版本)
- ✅ **Android 15 完全适配**：修复无障碍服务和通知权限问题
- ✅ **PendingIntent 修复**：添加 FLAG_IMMUTABLE 支持 Android 12+
- ✅ **前台服务优化**：支持 specialUse 前台服务类型
- ✅ **权限管理增强**：添加 POST_NOTIFICATIONS 和 QUERY_ALL_PACKAGES 权限
- ✅ **构建配置现代化**：完全迁移到 AndroidX，支持最新构建工具
- ✅ **错误处理改进**：增加异常处理和备用方案
- ✅ **Intent 跳转优化**：添加多层级降级机制

### v2.0 (历史版本)
- ✅ 更新所有依赖到AndroidX
- ✅ 适配Android 14 (API 34)
- ✅ 修复已知的兼容性问题
- ✅ 优化UI界面和用户体验
- ✅ 更新Gradle到8.0版本

### v1.0 (原始版本)
- 🔄 基于Android Support Library
- 🔄 目标API 26
- ⚠️ 依赖已过时，不建议使用

## 开发者信息

- **原作者**：wangnan7
- **创建时间**：2018年4月
- **项目类型**：Android开发工具
- **许可证**：请查看LICENSE文件

## 贡献指南

欢迎提交Issue和Pull Request来改进这个项目。在贡献代码前，请确保：

1. 代码风格符合项目规范
2. 充分测试新功能
3. 更新相应的文档

## 联系方式

如有问题或建议，请通过以下方式联系：

- 提交GitHub Issue
- 发送邮件至项目维护者

---

**声明**：本工具仅供开发调试使用，请遵守相关法律法规和用户隐私协议。