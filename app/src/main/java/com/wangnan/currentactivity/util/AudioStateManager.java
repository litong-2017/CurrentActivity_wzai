package com.wangnan.currentactivity.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: AudioStateManager
 * @Description: 音频状态管理器 - 用于协调无障碍服务和音频监控服务之间的音频状态
 *
 * 功能说明：
 * 1. 统一管理音频状态
 * 2. 协调无障碍服务和音频监控服务之间的状态同步
 * 3. 提供音频状态的获取和更新接口
 * 4. 支持多个监听器同时监听音频状态变化
 *
 * @Author wangnan7
 * @Date: 2025/9/15
 */
public class AudioStateManager {

    private static AudioStateManager mInstance;
    private String mCurrentAudioStatus = "";
    private List<AudioStatusListener> mListeners;

    /**
     * 音频状态监听器
     */
    public interface AudioStatusListener {
        void onAudioStatusChanged(String newStatus);
    }

    private AudioStateManager() {
        // 私有构造函数，防止外部实例化
        mListeners = new ArrayList<>();
    }

    /**
     * 获取音频状态管理器实例
     */
    public static synchronized AudioStateManager getInstance() {
        if (mInstance == null) {
            mInstance = new AudioStateManager();
        }
        return mInstance;
    }

    /**
     * 更新音频状态
     */
    public synchronized void updateAudioStatus(String newStatus) {
        try {
            if (newStatus != null && !newStatus.equals(mCurrentAudioStatus)) {
                String oldStatus = mCurrentAudioStatus;
                mCurrentAudioStatus = newStatus;

                Log.d("AudioStateManager", "🎵 音频状态更新: " + oldStatus + " -> " + newStatus);

                // 通知所有监听器
                for (AudioStatusListener listener : mListeners) {
                    if (listener != null) {
                        try {
                            listener.onAudioStatusChanged(newStatus);
                        } catch (Exception e) {
                            Log.e("AudioStateManager", "🎵 通知监听器失败: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("AudioStateManager", "🎵 更新音频状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前音频状态
     */
    public synchronized String getCurrentAudioStatus() {
        return mCurrentAudioStatus;
    }

    /**
     * 添加音频状态监听器
     */
    public void addAudioStatusListener(AudioStatusListener listener) {
        if (listener != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    /**
     * 移除音频状态监听器
     */
    public void removeAudioStatusListener(AudioStatusListener listener) {
        if (listener != null) {
            mListeners.remove(listener);
        }
    }

    /**
     * 清除所有音频状态监听器
     */
    public void clearAudioStatusListener() {
        mListeners.clear();
    }

    /**
     * 重置音频状态
     */
    public synchronized void reset() {
        mCurrentAudioStatus = "";
        mListeners = null;
    }
}