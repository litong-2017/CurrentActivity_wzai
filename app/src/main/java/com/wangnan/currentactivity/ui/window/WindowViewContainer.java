package com.wangnan.currentactivity.ui.window;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.wangnan.currentactivity.R;

/**
 * @ClassName: WindowViewContainer
 * @Description: 窗口视图容器
 * @Author wangnan7
 * @Date: 2018/4/1
 */

public class WindowViewContainer {

    /***
     * 窗口视图容器（引用变量）
     */
    @SuppressLint("StaticFieldLeak")
    private static WindowViewContainer mCustomWindowView;

    /**
     * 私有构造器
     */
    private WindowViewContainer(Context context) {
        mContext = context;
        initView(context);
    }

    /**
     * 获取窗口视图容器
     */
    public static synchronized WindowViewContainer getInstance(Context context) {
        if (mCustomWindowView == null) {
            mCustomWindowView = new WindowViewContainer(context);
        }
        return mCustomWindowView;
    }

    /*******************************************************************************************/

    private final Context mContext;

    /**
     * 窗口文本视图（显示包名+类名）
     */
    private TextView mTextView;

    /**
     * 窗口管理器
     */
    private WindowManager mWindowManager;

    /**
     * 是否已添加窗口
     */
    private boolean isAdded;

    /**
     * 是否处于显示状态
     */
    private boolean isShow;

    /**
     * 初始化视图
     *
     */
    @SuppressLint("InflateParams")
    private void initView(Context context) {
        mTextView = (TextView) LayoutInflater.from(context).inflate(R.layout.lay_window, null);
    }

    /**
     * 添加窗口视图
     */
    public void addWindowView() {
        // 如果窗口已添加直接返回
        if (isAdded) {
            return;
        }
        addView();
    }

    /**
     * 添加窗口视图
     */
    @SuppressLint("RtlHardcoded")
    private void addView() {
        // 创建布局参数
        WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();
        // 获取窗口管理器
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        // 设置类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android O 以上，使用TYPE_APPLICATION_OVERLAY弹窗类型
            mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            // Android O 以下，使用TYPE_SYSTEM_ALERT弹窗类型
            mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        // 设置标签（FLAG_NOT_FOCUSABLE表示窗口不会获取焦点；FLAG_NOT_TOUCHABLE表示窗口不会接收Touch事件，即将Touch事件向下层分发）
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        // 设置位图模式 （PixelFormat.RGBA_8888可以使背景透明。不设置默认PixelFormat.OPAQUE，即不透明）
        mParams.format = PixelFormat.RGBA_8888;
        // 设置分布位置（距左对齐 + 距顶对齐）
        mParams.gravity = Gravity.LEFT | Gravity.TOP;
        // 设置布局宽/高为自适应
        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        // 添加TextView
        mWindowManager.addView(mTextView, mParams);
        // 记录视图已被添加、显示
        isAdded = true;
        isShow = true;
    }

    /**
     * 更新窗口视图 - 显示前台页面包名、类名和切换时间
     * 
     * 这个方法负责将从无障碍服务获取到的前台页面信息显示在悬浮窗中
     * 
     * 显示格式：
     * 第一行：切换时间 (如: 23:45:30)
     * 第二行：应用包名 (如: com.tencent.mm)
     * 第三行：Activity类名 (如: com.tencent.mm.ui.LauncherUI)
     * 
     * @param text 要显示的文本内容，格式为"时间\n包名\nActivity类名"
     */
    public void updateWindowView(String text) {
        // 检查悬浮窗是否已经添加到窗口管理器中
        if (isAdded && mTextView != null) {
            
            // 更新悬浮窗文本内容
            // text 的典型格式：
            // "23:45:30\ncom.android.settings\ncom.android.settings.Settings"
            // 第一行：窗口切换时间
            // 第二行：应用包名  
            // 第三行：Activity完整类名
            mTextView.setText(text);
            
            // 兼容性处理：防止某些情况下窗口被系统意外移除
            // 在一些低版本设备或特定ROM上，当用户按Back键或应用切换时，
            // 悬浮窗可能被系统自动移除，这里尝试重新添加确保显示正常
            try {
                // 如果窗口已经存在，addView()会直接返回，不会重复添加
                addView();
            } catch (Exception e) {
                // 记录异常但不影响主要功能
                // 可能的异常：WindowManager.BadTokenException, SecurityException等
                Log.d("WindowViewContainer", "重新添加悬浮窗时发生异常: " + e.getMessage());
                Log.d("ERROR", Log.getStackTraceString(e));
            }
        }
    }

    /**
     * 移除窗口视图
     */
    public void removeWindowView() {
        if (!isAdded) {
            return;
        }
        mWindowManager.removeView(mTextView);
        isAdded = false;
        isShow = false;
    }

    /**
     * 开/关窗口视图（隐藏/显示窗口视图）
     */
    public void switchWindowView() {
        if (isAdded) {
            isShow = !isShow;
            mTextView.setVisibility(isShow ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /**
     * 获取窗口视图的显示状态
     */
    public boolean getWinodwViewShowState() {
        return isAdded && isShow;
    }

    /**
     * 销毁视图容器
     */
    public void destory() {
        removeWindowView();
        mCustomWindowView = null;
    }
}
