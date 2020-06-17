package com.jack.monitor;

import android.content.Context;

import com.jack.anrmonitor.AnrMonitor;
import com.jack.fpsmonitor.FpsMonitor;

/**
 * Monitor统一启动管理器
 *
 * @author jack
 * @since 2020/6/17 11:21
 */
public class Monitor {

    private AnrMonitor anrMonitor;

    private Monitor() {
    }

    private static class SingletonInstance {
        private static Monitor INSTANCE = new Monitor();
    }

    public static Monitor install() {
        return SingletonInstance.INSTANCE;
    }


    /**
     * 快捷开发Monitor
     */
    public void start(Context context) {
        startAnr(context);
        startFps(context);
    }

    /**
     * 默认：
     * 无FrameDataCallback回调
     * BAD：丢帧20%
     * MEDIUM：丢帧5%
     * startLoacation(200,600)
     */
    public void startFps(Context context) {
        FpsMonitor.getInstance().show(context.getApplicationContext());
    }

    /**
     * 自由设置FpsMonitor配置参数
     * 最后必须自己调用show()
     */
    public FpsMonitor startFpsCustom() {
        return FpsMonitor.getInstance();
    }

    /**
     * 默认：
     * 阻塞检测周期:5s
     * debuging模式下不上报
     * 打印所有的线程堆栈信息
     * 发生阻塞抛出阻塞错误并使应用程序崩溃。
     * 提前拦截阻塞震动提醒
     */
    public void startAnr(Context context) {
        anrMonitor = new AnrMonitor(context.getApplicationContext());
        anrMonitor.start();
    }

    /**
     * 自由设置AnrMonitor配置参数
     * 最后必须自己调用start()
     *
     * @param monitorInterval 每隔monitorInterval检测一次主线程是否阻塞
     */
    public AnrMonitor startAnrCustom(Context context, int monitorInterval) {
        anrMonitor = new AnrMonitor(context.getApplicationContext(), monitorInterval);
        return anrMonitor;
    }


    /**
     * 停止Monitor
     */
    public void stop(Context context) {
        if (anrMonitor != null)
            anrMonitor.stopAnrMonitor();
        FpsMonitor.getInstance().hide(context.getApplicationContext());
    }
}
