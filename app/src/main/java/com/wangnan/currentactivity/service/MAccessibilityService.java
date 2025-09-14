package com.wangnan.currentactivity.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.RequiresApi;

import com.wangnan.currentactivity.receiver.MAccessibilityServiceReceiver;
import com.wangnan.currentactivity.ui.activity.MainActivity;
import com.wangnan.currentactivity.ui.window.WindowViewContainer;
import com.wangnan.currentactivity.util.NotificationUtil;

/**
 * @ClassName: MAccessibilityService
 * @Description: 无障碍辅助服务 - 用于获取前台应用页面信息的核心服务
 * 
 * 功能说明：
 * 1. 监听系统窗口状态变化事件
 * 2. 实时获取前台应用的包名和Activity类名
 * 3. 通过悬浮窗显示获取到的信息
 * 4. 提供通知栏控制功能
 * 
 * 工作流程：
 * 1. 用户在系统设置中开启此无障碍服务
 * 2. 服务启动后创建前台通知和悬浮窗
 * 3. 监听 TYPE_WINDOW_STATE_CHANGED 事件
 * 4. 当用户切换应用或Activity时，自动获取并显示包名信息
 * 
 * 权限要求：
 * - 无障碍服务权限 (用户手动授权)
 * - 悬浮窗权限 (SYSTEM_ALERT_WINDOW)
 * - 前台服务权限 (FOREGROUND_SERVICE)
 * 
 * @Author wangnan7
 * @Date: 2018/4/1
 * @Update: 2025/9/14 - 适配Android 15，优化获取逻辑
 */

@SuppressLint("AccessibilityPolicy")
public class MAccessibilityService extends AccessibilityService {

    /**
     * 辅助服务名称（包名+"/"+完整类名）
     */
    public static final String SERVCE_NAME = "com.wangnan.currentactivity/com.wangnan.currentactivity.service.MAccessibilityService";

    /**
     * 通知栏ID
     */
    public static final int NOTIFICATION_ID = 0x1000;

    /**
     * 窗口视图容器
     */
    private WindowViewContainer mWindowViewContainer;

    /**
     * 广播接收器
     */
    private MAccessibilityServiceReceiver mReceiver;

    /**
     * 通知栏管理器
     */
    private NotificationManager mNotificationManager;

    /**
     * 服务连接完成
     */
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SuppressLint({"UnspecifiedRegisterReceiverFlag", "ObsoleteSdkInt"})
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        
        try {
            // 添加通知栏消息（将服务提升到前台）- 必须先启动前台服务
            addNotification();
            
            // 添加窗口
            mWindowViewContainer = WindowViewContainer.getInstance(this);
            mWindowViewContainer.addWindowView();
            
            // 注册广播接收器
            mReceiver = new MAccessibilityServiceReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(MAccessibilityServiceReceiver.SWITCH_ACTION);
            intentFilter.addAction(MAccessibilityServiceReceiver.CLOSE_ACTION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(mReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(mReceiver, intentFilter);
            }
            
            // 更新主界面UI
            if (MainActivity.mActivity != null) {
                MainActivity.mActivity.updateUI();
            }
        } catch (Exception e) {
            Log.d("ERROR", Log.getStackTraceString(e));
        }
    }

    /**
     * 添加通知
     */
    @SuppressLint("ObsoleteSdkInt")
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void addNotification() {
        try {
            // 获取通知管理器
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotificationManager == null) {
                return;
            }
            
            // Android O以上需要配置通知渠道
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    NotificationUtil.CHANNEL_ID, 
                    NotificationUtil.CHANNEL_NAME, 
                    NotificationManager.IMPORTANCE_LOW // 改为低重要性，避免打扰用户
                );
                channel.setDescription("CurrentActivity 辅助服务通知");
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setShowBadge(false);
                mNotificationManager.createNotificationChannel(channel);
            }
            
            // 获取Notification实例
            Notification notification = NotificationUtil.getNotificationByVersion(this);
            
            // 将辅助服务设置为前台服务
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            Log.d("ERROR", Log.getStackTraceString(e));
        }
    }

    /**
     * 接收辅助服务事件 - 这是获取前台页面包名的核心方法
     * 
     * 工作原理：
     * 1. Android 系统在窗口状态发生变化时会触发无障碍事件
     * 2. 我们监听 TYPE_WINDOW_STATE_CHANGED 事件来捕获Activity切换
     * 3. 从事件中提取包名和类名信息
     * 4. 将信息显示在悬浮窗中
     * 
     * @param event 无障碍事件对象，包含窗口状态变化的详细信息
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 安全检查：确保事件对象不为空
        if (event == null) {
            return;
        }
        
        // 检查事件类型：只处理窗口状态改变事件
        // TYPE_WINDOW_STATE_CHANGED: 当Activity启动、切换或窗口状态发生变化时触发
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            // 获取包名和类名
            // getPackageName(): 返回当前前台应用的包名 (如: com.android.settings)
            // getClassName(): 返回当前前台Activity的完整类名 (如: com.android.settings.Settings)
            CharSequence packageName = event.getPackageName();
            CharSequence className = event.getClassName();
            
            // 验证包名和类名都不为空
            if (packageName != null && className != null) {
                
                // 格式化显示信息：包名换行类名
                // 例如显示：
                // com.android.settings
                // com.android.settings.Settings
                String displayText = packageName + "\n" + className;
                
                // 更新悬浮窗显示内容
                // 这里将获取到的前台页面信息传递给悬浮窗进行显示
                if (mWindowViewContainer != null) {
                    mWindowViewContainer.updateWindowView(displayText);
                }
                
                // 调试日志：可以在开发时查看获取到的信息
                android.util.Log.d("CurrentActivity", 
                    "前台应用包名: " + packageName + ", Activity类名: " + className);
            }
        }
        
        // 注意：还可以监听其他类型的事件来获取更多信息
        // 例如：TYPE_WINDOW_CONTENT_CHANGED, TYPE_VIEW_CLICKED 等
        // 但为了性能考虑，我们只监听窗口状态变化事件
    }

    /**
     * 服务中断
     */
    @Override
    public void onInterrupt() {
    }


    /**
     * 服务退出
     */
    @Override
    public void onDestroy() {
        // 移除窗口视图，销毁视图容器
        if (mWindowViewContainer != null) {
            mWindowViewContainer.destory();
            mWindowViewContainer = null;
        }
        // 取消通知栏消息显示
        if (mNotificationManager != null) {
            mNotificationManager.cancel(MAccessibilityService.NOTIFICATION_ID);
            mNotificationManager = null;
        }
        // 解注册广播接收器
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        // 如果主界面未被销毁，更新主界面UI
        if (MainActivity.mActivity != null) {
            MainActivity.mActivity.updateUI();
        }
        // 停止前台服务
        stopForeground(true);
        super.onDestroy();
    }
}
