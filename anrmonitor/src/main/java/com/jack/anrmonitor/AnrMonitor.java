package com.jack.anrmonitor;

import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;

/**
 * 一个专门用来检测主线程是否阻塞的监视器
 *
 * @author jack
 * @since 2020/5/14 09:15
 */
public class AnrMonitor extends Thread {

    /**
     * 默认阻塞检测周期时长
     */
    private static final int DEFAULT_ANR_TIMEOUT = 5000;

    /**
     * 阻塞检测周期，默认5s
     */
    private final int monitorInterval;

    /**
     * 需要过滤的线程名字日志
     */
    private String filterStr = "";

    /**
     * 是否需要记录没有堆栈信息的线程
     */
    private boolean logThreadsWithoutStackTrace = false;

    /**
     * debuging模式下是否上报
     */
    private boolean ignoreDebugger = false;

    /**
     * AnrMonitor是否停止检测
     */
    private boolean isStop = false;

    /**
     * 专门用来判断主线程是否阻塞的标志位
     */
    private volatile long tick = 0;
    private volatile boolean reported = false;

    private Context context;

    /**
     * 默认AnrListener监听
     */
    private final AnrMonitorListener.AnrListener DEFAULT_ANR_LISTENER = new AnrMonitorListener.AnrListener() {
        @Override
        public void onAppNotResponding(AnrException anrException) {
            //默认直接抛出异常
            throw anrException;
        }
    };

    /**
     * AnrInterceptor
     */
    private final AnrMonitorListener.AnrInterceptor DEFAULT_ANR_INTERCEPTOR = new AnrMonitorListener.AnrInterceptor() {
        @Override
        public long intercept(long duration, AnrException anrException) {
            vibrator();
            return 0;
        }
    };

    /**
     * 默认InterruptionListener监听
     */
    private final AnrMonitorListener.InterruptionListener DEFAULT_INTERRUPTION_LISTENER = new AnrMonitorListener.InterruptionListener() {
        @Override
        public void onInterrupted(InterruptedException exception) {
            Log.e(Constant.TAG, "Interrupted: " + exception.getMessage());
        }
    };

    private AnrMonitorListener.AnrListener anrListener = DEFAULT_ANR_LISTENER;
    private AnrMonitorListener.AnrInterceptor anrInterceptor = DEFAULT_ANR_INTERCEPTOR;
    private AnrMonitorListener.InterruptionListener interruptionListener = DEFAULT_INTERRUPTION_LISTENER;

    /**
     * 专门用来处理tick,reported
     */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 每隔5秒检测一次主线程
     */
    public AnrMonitor(Context context) {
        this(context, DEFAULT_ANR_TIMEOUT);
    }

    /**
     * 每隔monitorInterval检测一次主线程
     *
     * @param monitorInterval
     */
    public AnrMonitor(Context context, int monitorInterval) {
        super();
        this.context = context;
        this.monitorInterval = monitorInterval;
    }

    /**
     * 设置要记录的线程的名称。注意主线程总是被记录。默认为""。
     *
     * @param filterStr 要记录的线程名的前缀。
     */
    public AnrMonitor setReportThreadNameFilter(String filterStr) {
        if (filterStr == null) {
            this.filterStr = "";
        }
        this.filterStr = filterStr;
        return this;
    }

    /**
     * 设置只记录主线程。
     */
    public AnrMonitor setReportMainThreadOnly() {
        this.filterStr = null;
        return this;
    }

    /**
     * 设置将记录所有线程(默认)。
     */
    public AnrMonitor setReportAllThreads() {
        this.filterStr = "";
        return this;
    }

    /**
     * 是否记录无堆栈信息的Thread
     *
     * @param logThreadsWithoutStackTrace 是否没有堆栈信息的线程
     */
    public AnrMonitor setLogThreadsWithoutStackTrace(boolean logThreadsWithoutStackTrace) {
        this.logThreadsWithoutStackTrace = logThreadsWithoutStackTrace;
        return this;
    }

    /**
     * 设置在检测ANR时是否忽略Debuging模式。
     * 默认false。
     *
     * @param ignoreDebugger 是否忽略debuging。
     */
    public AnrMonitor setIgnoreDebugger(boolean ignoreDebugger) {
        this.ignoreDebugger = ignoreDebugger;
        return this;
    }

    /**
     * @return 获取当前检测主线程阻塞的周期时长
     */
    public int getMonitorInterval() {
        return monitorInterval;
    }

    /**
     * 主线程的Runable，重置是否阻塞标志位
     */
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tick = 0;
            reported = false;
        }
    };

    @Override
    public void run() {
        setName(Constant.TAG);
        long interval = monitorInterval;
        while (!isStop && !isInterrupted()) {
            boolean needPost = tick == 0;
            tick += interval;
            if (needPost) {
                mainHandler.post(ticker);
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                interruptionListener.onInterrupted(e);
                return;
            }

            //防止线程之间并发问题,用局部变量保存
            long duration = tick;
            // 如果主线程没有处理ticker，就说明被阻塞了。
            if (duration != 0 && !reported) {

                //检测是否存在debuging模式中
                if (!ignoreDebugger && (Debug.isDebuggerConnected() || Debug.waitingForDebugger())) {
                    Log.w(Constant.TAG, "检测到发生阻塞，默认是忽略它的，但是可以使用setIgnoreDebugger(true)不忽略");
                    reported = true;
                    continue;
                }

                interval = anrInterceptor.intercept(duration, AnrException.createThreadException(duration, "", false));
                //根据返回值处理是否终结本次onAppNotResponding回调
                if (interval > 0) {
                    continue;
                }

                //确定发生了Anr，开始生成AnrException.
                AnrException anrException;
                if (filterStr != null) {
                    anrException = AnrException.createThreadException(duration, filterStr, logThreadsWithoutStackTrace);
                } else {
                    anrException = AnrException.createMainThreadException(duration);
                }
                anrListener.onAppNotResponding(anrException);
                interval = monitorInterval;
                reported = true;
            }
        }
    }

    /**
     * 停止AnrMonitor检测器
     */
    public void stopAnrMonitor() {
        mainHandler.removeCallbacksAndMessages(null);
        isStop = true;
        this.interrupt();
    }


    /**
     * 设置AnrListener接口。如果未设置，默认是抛出一个错误并使应用程序崩溃。
     */
    public AnrMonitor setAnrListener(AnrMonitorListener.AnrListener listener) {
        if (listener == null) {
            anrListener = DEFAULT_ANR_LISTENER;
        } else {
            anrListener = listener;
        }
        return this;
    }

    /**
     * 设置AnrInterceptor被发生Anr之前拦截它们。默认会震动提醒
     */
    public AnrMonitor setAnrInterceptor(AnrMonitorListener.AnrInterceptor interceptor) {
        if (interceptor == null) {
            anrInterceptor = DEFAULT_ANR_INTERCEPTOR;
        } else {
            anrInterceptor = interceptor;
        }
        return this;
    }

    /**
     * 设置InterruptionListener，已供停止AnrMonitor检测回调使用
     */
    public AnrMonitor setInterruptionListener(AnrMonitorListener.InterruptionListener listener) {
        if (listener == null) {
            interruptionListener = DEFAULT_INTERRUPTION_LISTENER;
        } else {
            interruptionListener = listener;
        }
        return this;
    }


    /**
     * 震动提醒
     */
    public void vibrator() {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        long[] patter = {1, 500, 100, 500};
        vibrator.vibrate(patter, -1);
    }


}
