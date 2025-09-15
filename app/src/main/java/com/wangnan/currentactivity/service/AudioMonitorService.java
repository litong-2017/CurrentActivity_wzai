package com.wangnan.currentactivity.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.wangnan.currentactivity.service.MAccessibilityService;
import com.wangnan.currentactivity.ui.window.WindowViewContainer;
import com.wangnan.currentactivity.util.AudioStateManager;
import com.wangnan.currentactivity.util.NotificationUtil;

/**
 * @ClassName: AudioMonitorService
 * @Description: 音频监控服务 - 独立于前台应用切换监控的音频状态监控
 *
 * 功能说明：
 * 1. 独立监控手机的音频播放状态
 * 2. 实时检测音乐播放、音量变化、静音状态等
 * 3. 通过悬浮窗显示音频状态
 * 4. 与无障碍服务协同工作，但独立运行
 *
 * 工作流程：
 * 1. 服务启动后创建前台通知
 * 2. 定期检测音频状态变化
 * 3. 当音频状态变化时，更新悬浮窗显示
 * 4. 与主服务共享悬浮窗容器
 *
 * @Author wangnan7
 * @Date: 2025/9/15
 */
public class AudioMonitorService extends Service {

    /**
     * 通知栏ID
     */
    public static final int NOTIFICATION_ID = 0x2000;

    /**
     * 音频状态检测间隔（毫秒）
     */
    private static final long AUDIO_CHECK_INTERVAL = 2000; // 2秒检测一次

    /**
     * 音频状态变化监听器
     */
    public interface AudioStatusListener {
        void onAudioStatusChanged(String newStatus);
    }

    /**
     * 服务绑定器
     */
    private final IBinder mBinder = new AudioMonitorBinder();

    /**
     * 音频管理器
     */
    private AudioManager mAudioManager;

    /**
     * 通知栏管理器
     */
    private NotificationManager mNotificationManager;

    /**
     * 窗口视图容器
     */
    private WindowViewContainer mWindowViewContainer;

    /**
     * 音频状态监听器
     */
    private AudioStatusListener mAudioStatusListener;

    /**
     * 当前音频状态
     */
    private String mCurrentAudioStatus = "";

    /**
     * 上一次检测的音频状态
     */
    private String mLastAudioStatus = "";

    /**
     * 音频状态检测Handler
     */
    private Handler mAudioCheckHandler;

    /**
     * 音频状态检测Runnable
     */
    private Runnable mAudioCheckRunnable;

    /**
     * 服务是否运行中
     */
    private boolean mIsRunning = false;

    /**
     * 服务绑定器类
     */
    public class AudioMonitorBinder extends Binder {
        public AudioMonitorService getService() {
            return AudioMonitorService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("AudioMonitor", "🎵 音频监控服务启动");

        // 初始化音频管理器
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // 获取窗口视图容器实例
        mWindowViewContainer = WindowViewContainer.getInstance(this);

        // 初始化Handler
        mAudioCheckHandler = new Handler(Looper.getMainLooper());

        // 设置音频状态管理器监听器
        AudioStateManager.getInstance().addAudioStatusListener(new AudioStateManager.AudioStatusListener() {
            @Override
            public void onAudioStatusChanged(String newStatus) {
                Log.d("AudioMonitor", "🎵 通过状态管理器接收到音频状态变化: " + newStatus);
                // 可以在这里添加额外的处理逻辑
            }
        });

        // 初始化音频检测Runnable
        mAudioCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkAudioStatus();
                if (mIsRunning) {
                    mAudioCheckHandler.postDelayed(this, AUDIO_CHECK_INTERVAL);
                }
            }
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("AudioMonitor", "🎵 音频监控服务 onStartCommand");

        try {
            // 创建前台通知 - 使用specialUse类型而非mediaPlayback
            Notification notification = createNotification();
            if (notification != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // 使用specialUse类型，因为我们只是监控音频状态而非播放媒体
                    startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                } else {
                    startForeground(NOTIFICATION_ID, notification);
                }
            }
        } catch (SecurityException e) {
            Log.e("AudioMonitor", "🎵 启动前台服务失败: " + e.getMessage());
            // 如果权限不足，尝试不使用特殊类型
            try {
                Notification notification = createNotification();
                if (notification != null) {
                    startForeground(NOTIFICATION_ID, notification);
                }
            } catch (Exception fallbackException) {
                Log.e("AudioMonitor", "🎵 前台服务启动失败: " + fallbackException.getMessage());
                stopSelf();
                return START_NOT_STICKY;
            }
        } catch (Exception e) {
            Log.e("AudioMonitor", "🎵 创建前台通知失败: " + e.getMessage());
            stopSelf();
            return START_NOT_STICKY;
        }

        // 开始音频状态检测
        startAudioMonitoring();

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * 开始音频监控
     */
    private void startAudioMonitoring() {
        if (mIsRunning) {
            return;
        }

        mIsRunning = true;
        Log.d("AudioMonitor", "🎵 开始音频状态监控");

        // 立即执行一次检测
        mAudioCheckHandler.post(mAudioCheckRunnable);
    }

    /**
     * 停止音频监控
     */
    private void stopAudioMonitoring() {
        mIsRunning = false;
        mAudioCheckHandler.removeCallbacks(mAudioCheckRunnable);
        Log.d("AudioMonitor", "🎵 停止音频状态监控");
    }

    /**
     * 检查音频状态
     */
    private void checkAudioStatus() {
        try {
            if (mAudioManager == null) {
                return;
            }

            // 获取当前音频状态
            String newAudioStatus = getAudioStatus();

            // 如果音频状态发生变化
            if (!newAudioStatus.equals(mLastAudioStatus)) {
                Log.d("AudioMonitor", "🎵 音频状态变化: " + mLastAudioStatus + " -> " + newAudioStatus);

                mLastAudioStatus = newAudioStatus;
                mCurrentAudioStatus = newAudioStatus;

                // 更新音频状态管理器（这会自动通知所有监听器）
                AudioStateManager.getInstance().updateAudioStatus(newAudioStatus);

                // 通知监听器
                if (mAudioStatusListener != null) {
                    mAudioStatusListener.onAudioStatusChanged(newAudioStatus);
                }

                // 直接更新悬浮窗显示（不依赖无障碍服务）
                updateFloatingWindowDisplay();

                Log.d("AudioMonitor", "🎵 音频状态更新完成: " + newAudioStatus);
            }

        } catch (Exception e) {
            Log.e("AudioMonitor", "🎵 检查音频状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取音频状态
     *
     * @return 音频状态字符串
     */
    private String getAudioStatus() {
        try {
            // 检查音乐是否活跃（更准确的播放状态检测）
            boolean isMusicActive = mAudioManager.isMusicActive();

            // 检查是否静音模式
            int ringerMode = mAudioManager.getRingerMode();
            boolean isSilentMode = ringerMode == AudioManager.RINGER_MODE_SILENT;
            boolean isVibrateMode = ringerMode == AudioManager.RINGER_MODE_VIBRATE;

            // 检查媒体音量
            int musicVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            // 优先显示播放状态
            if (isMusicActive) {
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
            Log.e("AudioMonitor", "🎵 获取音频状态失败: " + e.getMessage());
            return "🔊未知";
        }
    }

    /**
     * 更新悬浮窗显示
     */
    private void updateFloatingWindowDisplay() {
        try {
            if (mWindowViewContainer == null) {
                return;
            }

            // 获取当前悬浮窗显示内容
            String currentText = mWindowViewContainer.getCurrentDisplayText();
            if (currentText.isEmpty()) {
                return;
            }

            // 解析当前显示内容并更新音频状态
            String[] lines = currentText.split("\n");
            if (lines.length >= 1) {
                String firstLine = lines[0];

                // 更精确地移除旧的音频状态，保留时间和其他状态信息
                // 格式: "2025-09-15 14:30:45 🔋85%电量 📱亮屏🔒锁定🎵播放"
                // 我们需要只替换音频相关的部分，保留时间、电量、屏幕、锁屏状态

                // 移除所有音频状态图标和描述
                String audioPattern = "[🎵🔊🔉🔈🔇📳](?:播放|大声|中声|小声|静音|无声|震动)";
                firstLine = firstLine.replaceAll(audioPattern, "");

                // 在合适的位置添加新的音频状态（通常在最后）
                if (!mCurrentAudioStatus.isEmpty()) {
                    firstLine += mCurrentAudioStatus;
                }

                // 重新组装显示内容
                StringBuilder newText = new StringBuilder(firstLine);
                for (int i = 1; i < lines.length; i++) {
                    newText.append("\n").append(lines[i]);
                }

                // 更新悬浮窗
                mWindowViewContainer.updateWindowView(newText.toString());

                Log.d("AudioMonitor", "🎵 音频监控器更新悬浮窗: " + mCurrentAudioStatus + ", 完整行: " + firstLine);
            }

        } catch (Exception e) {
            Log.e("AudioMonitor", "🎵 更新悬浮窗显示失败: " + e.getMessage());
        }
    }

    /**
     * 检查无障碍服务是否正在运行
     */
    private boolean isAccessibilityServiceRunning() {
        // 这里简化处理，实际可以通过检查服务状态来判断
        // 由于两个服务共享同一个悬浮窗容器，我们可以假设如果容器有内容，
        // 那么无障碍服务可能正在运行
        return !mWindowViewContainer.getCurrentDisplayText().isEmpty();
    }

    /**
     * 通知无障碍服务音频状态变化
     */
    private void notifyAccessibilityService(String audioStatus) {
        try {
            // 尝试通过多种方式通知无障碍服务

            // 方法1: 通过音频状态管理器更新状态
            AudioStateManager.getInstance().updateAudioStatus(audioStatus);

            // 方法2: 如果无障碍服务正在运行，尝试直接调用其更新方法
            if (isAccessibilityServiceRunning()) {
                Log.d("AudioMonitor", "🎵 通知无障碍服务音频状态变化: " + audioStatus);

                // 尝试获取无障碍服务实例并调用其更新方法
                // 这里简化处理，主要通过音频状态管理器进行协调
                // 无障碍服务会监听音频状态管理器的变化
            }

            // 方法3: 发送广播（如果需要跨进程通信）
            // Intent intent = new Intent("com.wangnan.currentactivity.AUDIO_STATE_CHANGED");
            // intent.putExtra("audio_status", audioStatus);
            // sendBroadcast(intent);

        } catch (Exception e) {
            Log.e("AudioMonitor", "🎵 通知无障碍服务失败: " + e.getMessage());
        }
    }

    /**
     * 创建通知
     */
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SuppressLint("ObsoleteSdkInt")
    private Notification createNotification() {
        try {
            // 获取通知管理器
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotificationManager == null) {
                return null;
            }

            // Android O以上需要配置通知渠道
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    "audio_monitor_channel",
                    "音频监控",
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("音频播放状态监控服务");
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setShowBadge(false);
                mNotificationManager.createNotificationChannel(channel);
            }

            // 创建通知
            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, "audio_monitor_channel");
            } else {
                builder = new Notification.Builder(this);
            }

            builder.setSmallIcon(android.R.drawable.ic_media_play)
                   .setContentTitle("音频监控服务")
                   .setContentText("正在监控音频播放状态")
                   .setPriority(Notification.PRIORITY_LOW)
                   .setOngoing(true);

            return builder.build();

        } catch (Exception e) {
            Log.e("AudioMonitor", "🎵 创建通知失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 设置音频状态监听器
     */
    public void setAudioStatusListener(AudioStatusListener listener) {
        mAudioStatusListener = listener;
    }

    /**
     * 获取当前音频状态
     */
    public String getCurrentAudioStatus() {
        return mCurrentAudioStatus;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("AudioMonitor", "🎵 音频监控服务停止");

        // 停止音频监控
        stopAudioMonitoring();

        // 注意：不清除音频状态管理器的监听器，因为其他服务可能还在使用
        // AudioStateManager.getInstance().clearAudioStatusListener();

        // 停止前台服务
        stopForeground(true);

        // 取消通知
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
    }
}