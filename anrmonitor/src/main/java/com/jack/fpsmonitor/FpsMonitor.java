package com.jack.fpsmonitor;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Choreographer;
import android.view.Display;
import android.view.WindowManager;

import com.jack.fpsmonitor.ui.TinyCoach;


/**
 * FPS检测行为/UI构建类
 *
 * @author jack
 * @since 2020/6/17
 */
public class FpsMonitor {
    /**
     * FPS显示及检测配置参数
     */
    private FpsConfig fpsConfig;
    private FpsFrameCallback fpsFrameCallback;
    /**
     * FPS显示悬浮窗
     */
    private TinyCoach tinyCoach;
    /**
     * 应用全局前后台监听
     */
    private Foreground.Listener foregroundListener = new Foreground.Listener() {
        @Override
        public void onBecameForeground() {
            tinyCoach.show();
        }

        @Override
        public void onBecameBackground() {
            tinyCoach.hide(false);
        }
    };


    private FpsMonitor() {
        fpsConfig = new FpsConfig();
    }

    private static class SingletonInstance {
        private static FpsMonitor INSTANCE = new FpsMonitor();
    }

    public static FpsMonitor getInstance() {
        return SingletonInstance.INSTANCE;
    }


    /**
     * 将fpsConfig配置到设备的硬件
     * 每秒钟播放的帧数 eg. 60fps
     * 每秒钟 60 帧的屏幕刷新频率 eg. 16.6ms
     *
     * @param context
     */
    private void setFrameRate(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        fpsConfig.deviceRefreshRateInMs = 1000f / display.getRefreshRate();
        fpsConfig.refreshRate = display.getRefreshRate();
    }

    /**
     * 停止帧回调和前台监听器
     * 空出静态变量
     * 在静态上下文中从FPSLibrary调用
     */
    public void hide(Context context) {
        //告诉回调停止注册自己
        if (fpsFrameCallback != null)
            fpsFrameCallback.setEnabled(false);
        if (foregroundListener != null)
            Foreground.get(context).removeListener(foregroundListener);
        if (tinyCoach != null) {
            // 从窗口移除视图
            tinyCoach.destroy();
            tinyCoach = null;
            fpsFrameCallback = null;
        }
        fpsConfig = null;
    }


    /**
     * 显示fps仪表，注册FrameCallback
     * 收集fps信息并将其显示
     */
    public void show(Context context) {
        //首先要去申请悬浮窗权限.
        if (overlayPermRequest(context.getApplicationContext())) {
            //一旦授予了权限，您就必须再次调用show()
            return;
        }

        if (tinyCoach != null) {
            tinyCoach.show();
            return;
        }
        if (fpsConfig == null)
            fpsConfig = new FpsConfig();
        // 设置设备的帧速率信息到配置
        setFrameRate(context.getApplicationContext());

        // 创建更新视图的演示器
        tinyCoach = new TinyCoach((Application) context.getApplicationContext(), fpsConfig);

        // 创建 choreographer
        fpsFrameCallback = new FpsFrameCallback(fpsConfig, tinyCoach);
        //注册回调FrameCallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Choreographer.getInstance().postFrameCallback(fpsFrameCallback);
        }

        //注册activity前后台监听
        Foreground.get(context.getApplicationContext()).addListener(foregroundListener);
    }

    /**
     * 增加FrameDataCallback
     */
    public FpsMonitor addFrameDataCallback(FrameDataCallback callback) {
        fpsConfig.frameDataCallback = callback;
        return this;
    }

    /**
     * 设置红色标记百分比，默认为20%
     */
    public FpsMonitor redFlagPercentage(float percentage) {
        fpsConfig.redFlagPercentage = percentage;
        return this;
    }

    /**
     * 设置红旗百分比，默认为5%
     */
    public FpsMonitor yellowFlagPercentage(float percentage) {
        fpsConfig.yellowFlagPercentage = percentage;
        return this;
    }

    /**
     * fps仪表的x起点位置 default：200px
     */
    public FpsMonitor startingXPosition(int xPosition) {
        fpsConfig.startingXPosition = xPosition;
        fpsConfig.xOrYSpecified = true;
        return this;
    }

    /**
     * fps仪表的y起点位置 default：600px
     */
    public FpsMonitor startingYPosition(int yPosition) {
        fpsConfig.startingYPosition = yPosition;
        fpsConfig.xOrYSpecified = true;
        return this;
    }

    /**
     * fps仪表的启动gravity default：Gravity.TOP | Gravity.START;
     */
    public FpsMonitor startingGravity(int gravity) {
        fpsConfig.startingGravity = gravity;
        fpsConfig.gravitySpecified = true;
        return this;
    }

    /**
     * 申请悬浮窗权限
     */
    private boolean overlayPermRequest(Context context) {
        boolean permNeeded = false;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                permNeeded = true;
            }
        }
        return permNeeded;
    }

}
