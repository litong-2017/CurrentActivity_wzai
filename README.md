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
        minSdk 21 // 提升最低支持版本
        targetSdk 34 // 更新目标版本
        versionCode 2
        versionName "2.0"
        
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
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.core:core:1.12.0'
    
    // 测试依赖
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

#### Gradle Wrapper 更新
```properties
# gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
```

## 安装和使用

### 1. 环境要求
- Android Studio Arctic Fox 2020.3.1+ 
- Android SDK API 34+
- Gradle 8.0+
- Java 8+

### 2. 构建步骤
```bash
# 克隆项目
git clone [项目地址]

# 同步依赖
./gradlew sync

# 构建APK
./gradlew assembleDebug
```

### 3. 使用流程
1. **安装应用**：安装编译好的APK文件
2. **授权悬浮窗权限**：首次启动会提示授权悬浮窗权限
3. **开启无障碍服务**：在设置-无障碍功能中开启"CurrentActivity"服务
4. **开启悬浮窗**：返回应用点击"打开悬浮窗"
5. **查看Activity信息**：悬浮窗会实时显示当前Activity信息

### 4. 功能说明
- **打开/关闭悬浮窗**：控制悬浮窗的显示状态
- **权限检查**：自动检查并提示所需权限状态
- **通知栏控制**：通过通知栏快速控制悬浮窗显示
- **服务管理**：可以直接关闭无障碍服务

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

## 更新日志

### v2.0 (推荐版本)
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