package com.wangnan.currentactivity.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.RequiresApi;

import com.wangnan.currentactivity.receiver.MAccessibilityServiceReceiver;
import com.wangnan.currentactivity.ui.activity.MainActivity;
import com.wangnan.currentactivity.ui.window.WindowViewContainer;
import com.wangnan.currentactivity.util.NotificationUtil;

/**
 * @ClassName: MAccessibilityService
 * @Description: 辅助服务
 * @Author wangnan7
 * @Date: 2018/4/1
 */

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
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    /**
     * 接收辅助服务事件
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED: // 窗口状态改变
                if (event.getPackageName() != null && event.getClassName() != null) {
                    // 更新窗口视图
                    mWindowViewContainer.updateWindowView(event.getPackageName() + "\n" + event.getClassName());
                }
                break;
            default:
                break;
        }
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
