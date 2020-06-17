package com.jack.fpsmonitor;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * @author jack
 * @since 2020/6/17
 * <p>
 * 应用前后台操作类
 * <p>
 * 1. 获取前台单例对象，传递一个上下文或应用程序对象，除非你确定单例对象已经在别处被初始化了。
 * <p>
 * 2.a) 执行直接的同步检查:Foreground.isForeground() / .isBackground()
 * <p>
 * 或者
 * <p>
 * 2.b) 注册被通知(有用的服务或其他非ui组件):
 * <p>
 * Foreground.Listener myListener = new Foreground.Listener(){
 * public void onBecameForeground(){
 * // ... 无论你想做什么
 * }
 * public void onBecameBackground(){
 * // ... 无论你想做什么
 * }
 * }
 * <p>
 * public void onCreate(){
 * super.onCreate();
 * Foreground.get(this).addListener(listener);
 * }
 * <p>
 * public void onDestroy(){
 * super.onCreate();
 * Foreground.get(this).removeListener(listener);
 * }
 */
public class Foreground implements Application.ActivityLifecycleCallbacks {

    /**
     * handler.post 延迟检查（为了切换activity导致误判不在前台）
     */
    public static final long CHECK_DELAY = 600;
    private static final String TAG = "Foreground";

    /**
     * 应用在前后台监听
     */
    public interface Listener {
        /**
         * 在前台
         */
        public void onBecameForeground();

        /**
         * 在后台
         */
        public void onBecameBackground();

    }

    private volatile static Foreground instance;

    /**
     * 是否在前台，是否onPause
     */
    private boolean foreground = true, paused = true;
    private Handler handler = new Handler();
    private List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    private Runnable check;


    private Foreground(Application application) {
        application.registerActivityLifecycleCallbacks(this);
    }

    public static Foreground get(Context ctx) {
        if (instance == null) {
            synchronized (Foreground.class) {
                if (instance == null) {
                    Context appCtx = ctx.getApplicationContext();
                    if (appCtx instanceof Application) {
                        instance = new Foreground((Application) appCtx);
                    }
                }
            }
        }
        return instance;
    }


    /**
     * 是否在前台
     */
    public boolean isForeground() {
        return foreground;
    }

    /**
     * 是否在后台
     */
    public boolean isBackground() {
        return !foreground;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        paused = false;
        boolean wasBackground = !foreground;
        foreground = true;

        if (check != null)
            handler.removeCallbacks(check);
        //原本就在前台不用重新回调
        if (wasBackground) {
            Log.i(TAG, "切换到前台");
            for (Listener l : listeners) {
                try {
                    l.onBecameForeground();
                } catch (Exception exc) {
                    Log.e(TAG, "Listener threw exception!", exc);
                }
            }
        } else {
            Log.i(TAG, "仍然在前台");
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        paused = true;

        if (check != null)
            handler.removeCallbacks(check);
        //postDelayed为了切换activity导致误判不在前台
        handler.postDelayed(check = new Runnable() {
            @Override
            public void run() {
                if (foreground && paused) {
                    foreground = false;
                    Log.i(TAG, "切换到后台");
                    for (Listener l : listeners) {
                        try {
                            l.onBecameBackground();
                        } catch (Exception exc) {
                            Log.e(TAG, "Listener threw exception!", exc);
                        }
                    }
                } else {
                    Log.i(TAG, "仍然在前台");
                }
            }
        }, CHECK_DELAY);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}
