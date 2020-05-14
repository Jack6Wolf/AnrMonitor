package com.jack.anrmonitor;

/**
 * 用来承装所有AnrMonitor的回调接口
 *
 * @author jack
 * @since 2020/5/14 09:16
 */
public class AnrMonitorListener {
    private AnrMonitorListener() {
    }

    /**
     * 发生anr的回调
     */
    public interface AnrListener {
        /**
         * 发生Anr时返回{@link AnrException}
         *
         * @param anrException 应用各线程堆栈信息
         */
        void onAppNotResponding(AnrException anrException);
    }

    /**
     * 主线程发生{@link AnrMonitor#monitorInterval}阻塞时的回调
     */
    public interface AnrInterceptor {
        /**
         * 当主线程阻塞的时间超过超时定义的时间时调用。
         *
         * @param duration 卡顿判定周期
         * @return 大于0则推迟上报，小于等于0则立即上报，触发{@link AnrListener#onAppNotResponding(AnrException)}
         */
        long intercept(long duration, AnrException anrException);
    }

    /**
     * Thread发生InterruptedException时的回调，用于关闭监听器
     */
    public interface InterruptionListener {
        /**
         * 发生中断的异常回调
         */
        void onInterrupted(InterruptedException exception);
    }
}
