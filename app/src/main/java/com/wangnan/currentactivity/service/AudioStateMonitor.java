package com.wangnan.currentactivity.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.wangnan.currentactivity.ui.window.WindowViewContainer;

/**
 * @ClassName: AudioStateMonitor
 * @Description: 音频状态监控服务
 * 
 * 功能说明：
 * 1. 监听音频焦点变化事件
 * 2. 监听音量变化事件
 * 3. 监听静音模式切换事件
 * 4. 实时更新悬浮窗显示的音频状态
 * 
 * 工作原理：
 * - 通过广播接收器监听系统音频事件
 * - 通过音频焦点监听器监听播放状态变化
 * - 当音频状态发生变化时，回调通知更新悬浮窗
 * 
 * @Author wangnan7
 * @Date: 2025/9/15
 */
public class AudioStateMonitor {
    
    private static final String TAG = "AudioStateMonitor";
    
    /**
     * 音频状态变化回调接口
     */
    public interface AudioStateChangeListener {
        /**
         * 音频状态发生变化时的回调
         * @param newAudioStatus 新的音频状态字符串
         */
        void onAudioStateChanged(String newAudioStatus);
    }
    
    private Context mContext;
    private AudioManager mAudioManager;
    private AudioStateChangeListener mListener;
    private Handler mHandler;
    
    // 悬浮窗容器引用
    private WindowViewContainer mWindowViewContainer;
    
    // 广播接收器
    private AudioStateBroadcastReceiver mBroadcastReceiver;
    
    // 音频焦点监听器
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener;
    
    // 当前音频状态缓存
    private String mLastAudioStatus = "";
    
    /**
     * 构造函数
     * @param context 上下文
     * @param windowViewContainer 悬浮窗容器
     * @param listener 音频状态变化监听器（可选，用于额外的回调通知）
     */
    public AudioStateMonitor(Context context, WindowViewContainer windowViewContainer, AudioStateChangeListener listener) {
        this.mContext = context;
        this.mWindowViewContainer = windowViewContainer;
        this.mListener = listener;
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        
        initAudioFocusListener();
        initBroadcastReceiver();
    }
    
    /**
     * 构造函数（仅悬浮窗更新，无回调）
     * @param context 上下文
     * @param windowViewContainer 悬浮窗容器
     */
    public AudioStateMonitor(Context context, WindowViewContainer windowViewContainer) {
        this(context, windowViewContainer, null);
    }
    
    /**
     * 初始化音频焦点监听器
     */
    private void initAudioFocusListener() {
        mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                Log.d(TAG, "🎵 音频焦点变化: " + getFocusChangeString(focusChange));
                
                // 延迟更新，确保状态已经稳定
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateAudioStatus();
                    }
                }, 100);
            }
        };
    }
    
    /**
     * 初始化广播接收器
     */
    private void initBroadcastReceiver() {
        mBroadcastReceiver = new AudioStateBroadcastReceiver();
    }
    
    /**
     * 开始监听音频状态变化
     */
    public void startMonitoring() {
        try {
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            
            // 音量变化广播
            filter.addAction("android.media.VOLUME_CHANGED_ACTION");
            filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
            
            // 音频状态变化广播
            filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            filter.addAction("android.media.AUDIO_BECOMING_NOISY");
            
            // 耳机插拔广播
            filter.addAction(Intent.ACTION_HEADSET_PLUG);
            filter.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
            
            mContext.registerReceiver(mBroadcastReceiver, filter);
            
            // 请求音频焦点以监听焦点变化（不播放音频）
            if (mAudioManager != null) {
                // 使用临时焦点请求来注册监听器
                try {
                    mAudioManager.requestAudioFocus(mAudioFocusListener, 
                        AudioManager.STREAM_MUSIC, 
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                    // 立即放弃焦点，我们只是为了注册监听器
                    mAudioManager.abandonAudioFocus(mAudioFocusListener);
                } catch (Exception e) {
                    Log.e(TAG, "注册音频焦点监听器失败: " + e.getMessage());
                }
            }
            
            Log.d(TAG, "音频状态监控已启动");
            
            // 初始化当前状态
            updateAudioStatus();
            
        } catch (Exception e) {
            Log.e(TAG, "启动音频监控失败: " + e.getMessage());
        }
    }
    
    /**
     * 停止监听音频状态变化
     */
    public void stopMonitoring() {
        try {
            // 注销广播接收器
            if (mBroadcastReceiver != null) {
                mContext.unregisterReceiver(mBroadcastReceiver);
            }
            
            // 放弃音频焦点
            if (mAudioManager != null && mAudioFocusListener != null) {
                mAudioManager.abandonAudioFocus(mAudioFocusListener);
            }
            
            Log.d(TAG, "音频状态监控已停止");
            
        } catch (Exception e) {
            Log.e(TAG, "停止音频监控失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新音频状态
     */
    private void updateAudioStatus() {
        try {
            String newAudioStatus = getCurrentAudioStatus();
            
            // 只有状态发生变化时才通知更新
            if (!newAudioStatus.equals(mLastAudioStatus)) {
                mLastAudioStatus = newAudioStatus;
                
                Log.d(TAG, "🎵 音频状态变化: " + mLastAudioStatus + " → " + newAudioStatus);
                
                // 直接更新悬浮窗音频状态
                updateWindowAudioStatus(newAudioStatus);
                
                // 可选的回调通知
                if (mListener != null) {
                    mListener.onAudioStateChanged(newAudioStatus);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "更新音频状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 直接更新悬浮窗的音频状态
     * 
     * @param newAudioStatus 新的音频状态
     */
    private void updateWindowAudioStatus(String newAudioStatus) {
        try {
            if (mWindowViewContainer == null) {
                Log.w(TAG, "悬浮窗容器为空，无法更新音频状态");
                return;
            }
            
            // 获取当前悬浮窗显示的内容
            String currentText = mWindowViewContainer.getCurrentDisplayText();
            if (currentText == null || currentText.isEmpty()) {
                Log.w(TAG, "当前悬浮窗内容为空，无法更新音频状态");
                return;
            }
            
            // 解析当前显示的内容
            String[] lines = currentText.split("\n");
            if (lines.length < 3) {
                Log.w(TAG, "当前悬浮窗内容格式不正确，行数: " + lines.length);
                return;
            }
            
            // 提取当前的状态行（第一行）
            String currentStatusLine = lines[0];
            String packageName = lines[1];
            String className = lines[2];
            
            // 解析状态行，替换音频部分
            String newStatusLine = replaceAudioStatusInLine(currentStatusLine, newAudioStatus);
            
            // 构建新的显示内容
            String newDisplayText = newStatusLine + "\n" + packageName + "\n" + className;
            
            // 直接更新悬浮窗显示
            mWindowViewContainer.updateWindowView(newDisplayText);
            
            Log.d(TAG, "✅ AudioStateMonitor 独立更新悬浮窗音频状态: " + newAudioStatus);
            
        } catch (Exception e) {
            Log.e(TAG, "AudioStateMonitor 更新悬浮窗失败: " + e.getMessage());
        }
    }
    
    /**
     * 替换状态行中的音频状态
     * 
     * @param statusLine 原始状态行，如："14:30:20 🔋85%电量 📱亮屏🔒锁定🔊大声"
     * @param newAudioStatus 新的音频状态，如："🎵播放"
     * @return 替换后的状态行
     */
    private String replaceAudioStatusInLine(String statusLine, String newAudioStatus) {
        try {
            // 音频状态的可能模式
            String[] audioPatterns = {
                "🎵播放", "🔊大声", "🔉中声", "🔈小声", "🔇静音", "🔇无声", "📳震动", "🔊未知"
            };
            
            String result = statusLine;
            
            // 尝试替换已存在的音频状态
            for (String pattern : audioPatterns) {
                if (result.contains(pattern)) {
                    result = result.replace(pattern, newAudioStatus);
                    Log.d(TAG, "🔄 音频状态替换: '" + pattern + "' → '" + newAudioStatus + "'");
                    return result;
                }
            }
            
            // 如果没找到现有音频状态，在末尾添加
            result = result + newAudioStatus;
            Log.d(TAG, "➕ 在状态行末尾添加音频状态: " + newAudioStatus);
            
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "替换音频状态失败: " + e.getMessage());
            return statusLine + newAudioStatus; // 降级处理
        }
    }
    
    /**
     * 获取当前音频状态
     * 复用 MAccessibilityService 中的逻辑
     */
    private String getCurrentAudioStatus() {
        try {
            if (mAudioManager == null) {
                return "🔊未知";
            }
            
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
            Log.e(TAG, "获取音频状态失败: " + e.getMessage());
            return "🔊未知";
        }
    }
    
    /**
     * 音频状态广播接收器
     */
    private class AudioStateBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                Log.d(TAG, "🔊 收到音频广播: " + action);
                
                // 针对不同的广播类型设置不同的延迟时间
                int delayTime = 200;
                if ("android.media.VOLUME_CHANGED_ACTION".equals(action)) {
                    delayTime = 50; // 音量变化响应更快
                } else if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(action)) {
                    delayTime = 100; // 响铃模式变化
                }
                
                // 延迟更新，确保系统状态已经稳定
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateAudioStatus();
                    }
                }, delayTime);
                
            } catch (Exception e) {
                Log.e(TAG, "处理音频广播失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 手动触发音频状态更新
     * 用于外部需要强制更新音频状态的场景
     */
    public void forceUpdateAudioStatus() {
        updateAudioStatus();
    }
    
    /**
     * 获取当前缓存的音频状态
     */
    public String getLastAudioStatus() {
        return mLastAudioStatus;
    }
    
    /**
     * 获取音频焦点变化的描述字符串
     */
    private String getFocusChangeString(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                return "获得焦点";
            case AudioManager.AUDIOFOCUS_LOSS:
                return "失去焦点";
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                return "短暂失去焦点";
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                return "短暂失去焦点(可降低音量)";
            default:
                return "未知(" + focusChange + ")";
        }
    }
}