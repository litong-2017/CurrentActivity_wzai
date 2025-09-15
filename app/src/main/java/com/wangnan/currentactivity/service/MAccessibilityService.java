package com.wangnan.currentactivity.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
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
     * 音频状态监控器
     */
    private AudioStateMonitor mAudioStateMonitor;
    
    /**
     * 当前缓存的应用信息
     */
    private String mCurrentPackageName = "未知应用";
    private String mCurrentClassName = "未知Activity";
    
    /**
     * 当前缓存的音频状态
     */
    private String mCurrentAudioStatus = "";

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
            
            // 启动音频状态监控（传入悬浮窗容器）
            startAudioMonitoring();
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
                
                // 更新缓存的应用信息
                mCurrentPackageName = packageName.toString();
                mCurrentClassName = className.toString();
                
                // 调试日志：可以在开发时查看获取到的信息
                android.util.Log.d("CurrentActivity", 
                    "前台应用包名: " + packageName + ", Activity类名: " + className);
                
                // 触发完整的悬浮窗更新（包含新的应用信息）
                updateCompleteWindowDisplay();
            }
        }
        
        // 注意：还可以监听其他类型的事件来获取更多信息
        // 例如：TYPE_WINDOW_CONTENT_CHANGED, TYPE_VIEW_CLICKED 等
        // 但为了性能考虑，我们只监听窗口状态变化事件
    }

    /**
     * 获取完整的设备状态信息（使用缓存的音频状态）
     * 
     * 优先使用音频监控器缓存的状态，如果没有则实时获取
     * 
     * @return 格式化的设备状态字符串，如 "🔋85%电量 📱亮屏🔒锁定🎵播放"
     */
    private String getDeviceStatusWithCachedAudio() {
        try {
            StringBuilder statusBuilder = new StringBuilder();
            
            // 1. 电池电量信息
            statusBuilder.append(getBatteryInfo());
            
            // 2. 屏幕状态
            statusBuilder.append(" ").append(getScreenStatus());
            
            // 3. 锁屏状态  
            statusBuilder.append(getLockScreenStatus());
            
            // 4. 音乐播放状态 - 优先使用缓存的状态
            String audioStatus;
            if (!mCurrentAudioStatus.isEmpty()) {
                audioStatus = mCurrentAudioStatus;
                Log.d("DeviceStatus", "🎵 使用缓存的音频状态: " + audioStatus);
            } else {
                audioStatus = getMusicStatus();
                Log.d("DeviceStatus", "🎵 实时获取音频状态: " + audioStatus);
            }
            statusBuilder.append(audioStatus);
            
            return statusBuilder.toString();
            
        } catch (Exception e) {
            Log.e("DeviceStatus", "获取设备状态失败: " + e.getMessage());
            return "🔋??%电量 📱未知";
        }
    }

    /**
     * 获取完整的设备状态信息（实时获取所有状态）
     * 
     * 集成电量、屏幕状态、锁屏状态、充电状态、音乐播放状态等信息
     * 
     * @return 格式化的设备状态字符串，如 "🔋85%电量 📱亮屏🔒锁定🎵播放"
     */
    private String getDeviceStatus() {
        try {
            StringBuilder statusBuilder = new StringBuilder();
            
            // 1. 电池电量信息
            statusBuilder.append(getBatteryInfo());
            
            // 2. 屏幕状态
            statusBuilder.append(" ").append(getScreenStatus());
            
            // 3. 锁屏状态  
            statusBuilder.append(getLockScreenStatus());
            
            // 4. 音乐播放状态
            statusBuilder.append(getMusicStatus());
            
            return statusBuilder.toString();
            
        } catch (Exception e) {
            Log.e("DeviceStatus", "获取设备状态失败: " + e.getMessage());
            return "🔋??%电量 📱未知";
        }
    }

    /**
     * 获取电池电量信息
     * 
     * 通过 BatteryManager 获取当前设备的电池状态信息
     * 
     * @return 格式化的电量信息字符串，如 "🔋85%" 或 "⚡85%"（充电时）
     */
    private String getBatteryInfo() {
        try {
            // 获取电池管理器
            BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager == null) {
                return "🔋??%";
            }
            
            // 获取当前电量百分比
            int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            
            // 获取充电状态
            Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            boolean isCharging = false;
            if (batteryIntent != null) {
                int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                           status == BatteryManager.BATTERY_STATUS_FULL;
            }
            
            // 根据充电状态选择图标
            String batteryIcon = isCharging ? "⚡" : "🔋";
            
            // 根据电量显示不同图标
            if (batteryLevel <= 15) {
                batteryIcon = isCharging ? "⚡" : "🪫"; // 低电量
            } else if (batteryLevel <= 50) {
                batteryIcon = isCharging ? "⚡" : "🔋"; // 中等电量
            } else {
                batteryIcon = isCharging ? "⚡" : "🔋"; // 高电量
            }
            
            return batteryIcon + batteryLevel + "%电量";
            
        } catch (Exception e) {
            Log.e("BatteryInfo", "获取电池信息失败: " + e.getMessage());
            return "🔋??%电量";
        }
    }

    /**
     * 获取屏幕状态
     * 
     * 检测屏幕是否亮起
     * 
     * @return 屏幕状态图标，"📱亮屏"（亮屏）或 "📴熄屏"（熄屏）
     */
    private String getScreenStatus() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                return "📱未知";
            }
            
            boolean isScreenOn;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                isScreenOn = powerManager.isInteractive();
            } else {
                isScreenOn = powerManager.isScreenOn();
            }
            
            return isScreenOn ? "📱亮屏" : "📴熄屏";
            
        } catch (Exception e) {
            Log.e("ScreenStatus", "获取屏幕状态失败: " + e.getMessage());
            return "📱未知";
        }
    }

    /**
     * 获取锁屏状态
     * 
     * 检测设备是否处于锁屏状态
     * 
     * @return 锁屏状态图标，"🔒锁定"（锁屏）或 "🔓解锁"（未锁屏）
     */
    private String getLockScreenStatus() {
        try {
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager == null) {
                return "🔒未知";
            }
            
            boolean isLocked = keyguardManager.isKeyguardLocked();
            boolean isSecure = keyguardManager.isKeyguardSecure();
            
            if (isLocked) {
                return isSecure ? "🔒锁定" : "🔐简锁"; // 🔒安全锁屏 🔐简单锁屏
            } else {
                return "🔓解锁"; // 未锁屏
            }
            
        } catch (Exception e) {
            Log.e("LockStatus", "获取锁屏状态失败: " + e.getMessage());
            return "🔒未知";
        }
    }

    /**
     * 获取音乐播放状态
     * 
     * 检测是否有音乐正在播放和音频状态
     * 
     * @return 音乐播放状态图标，"🎵播放"（播放中）、"🔇静音"（静音）等
     */
    private String getMusicStatus() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                return "";
            }
            
            // 检查音乐是否活跃（更准确的播放状态检测）
            boolean isMusicActive = audioManager.isMusicActive();
            
            // 检查是否静音模式
            int ringerMode = audioManager.getRingerMode();
            boolean isSilentMode = ringerMode == AudioManager.RINGER_MODE_SILENT;
            boolean isVibrateMode = ringerMode == AudioManager.RINGER_MODE_VIBRATE;
            
            // 检查媒体音量
            int musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            
            // 检查是否有音频焦点（更准确的播放检测）
            boolean hasAudioFocus = false;
            try {
                // 通过反射获取音频焦点状态（Android内部方法）
                hasAudioFocus = isMusicActive; // 暂时使用isMusicActive作为替代
            } catch (Exception e) {
                // 如果反射失败，使用isMusicActive
                hasAudioFocus = isMusicActive;
            }
            
            // 优先显示播放状态
            if (isMusicActive || hasAudioFocus) {
                return "🎵播放"; // 音乐播放中
            }
            
            // 然后检查静音状态
            if (isSilentMode) {
                return "🔇静音"; // 静音模式
            } else if (isVibrateMode) {
                return "📳震动"; // 震动模式
            } else if (musicVolume == 0) {
                return "🔇无声"; // 媒体音量为0
            } else {
                // 根据音量显示不同状态
                float volumePercent = (float) musicVolume / maxVolume;
                if (volumePercent > 0.7) {
                    return "🔊大声"; // 高音量
                } else if (volumePercent > 0.3) {
                    return "🔉中声"; // 中音量  
                } else if (volumePercent > 0) {
                    return "🔈小声"; // 低音量
                } else {
                    return "🔇无声"; // 无音量
                }
            }
            
        } catch (Exception e) {
            Log.e("MusicStatus", "获取音乐状态失败: " + e.getMessage());
            return "🔊未知";
        }
    }

    /**
     * 启动音频状态监控
     */
    private void startAudioMonitoring() {
        try {
            if (mAudioStateMonitor == null && mWindowViewContainer != null) {
                // 直接传入悬浮窗容器，让 AudioStateMonitor 自主更新悬浮窗
                mAudioStateMonitor = new AudioStateMonitor(this, mWindowViewContainer);
            }
            if (mAudioStateMonitor != null) {
                mAudioStateMonitor.startMonitoring();
                Log.d("AudioMonitor", "🎵 音频监控服务已启动，AudioStateMonitor 将直接管理悬浮窗音频状态");
            }
        } catch (Exception e) {
            Log.e("AudioMonitor", "启动音频监控失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止音频状态监控
     */
    private void stopAudioMonitoring() {
        try {
            if (mAudioStateMonitor != null) {
                mAudioStateMonitor.stopMonitoring();
                mAudioStateMonitor = null;
            }
            Log.d("AudioMonitor", "音频监控服务已停止");
        } catch (Exception e) {
            Log.e("AudioMonitor", "停止音频监控失败: " + e.getMessage());
        }
    }
    
    
    /**
     * 完整更新悬浮窗显示
     * 
     * 这个方法由前台应用变化和音频状态变化共同调用
     * 确保所有状态信息都是最新的
     */
    private void updateCompleteWindowDisplay() {
        try {
            if (mWindowViewContainer == null) {
                return;
            }
            
            // 获取当前时间
            long currentTime = System.currentTimeMillis();
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
            String currentTimeStr = timeFormat.format(new java.util.Date(currentTime));
            
            // 获取完整的设备状态信息（实时获取所有状态）
            String deviceStatus = getDeviceStatus();
            
            // 格式化显示信息：时间 + 设备状态 + 包名 + 类名
            // 显示格式：
            // 23:45:30 🔋85%电量 📱亮屏🔒锁定🎵播放 (时间+电量+屏幕+锁屏+音乐状态)
            // com.android.settings (应用包名)
            // com.android.settings.Settings (Activity类名)
            String displayText = currentTimeStr + " " + deviceStatus + "\n" + 
                               mCurrentPackageName + "\n" + mCurrentClassName;
            
            // 更新悬浮窗显示
            mWindowViewContainer.updateWindowView(displayText);
            
            Log.d("WindowUpdate", "🔄 前台应用变化，悬浮窗完整更新 - 应用: " + mCurrentPackageName + 
                  ", 状态: " + deviceStatus);
            
        } catch (Exception e) {
            Log.e("WindowUpdate", "完整更新悬浮窗失败: " + e.getMessage());
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
        // 停止音频状态监控
        stopAudioMonitoring();
        
        // 停止前台服务
        stopForeground(true);
        super.onDestroy();
    }
}
