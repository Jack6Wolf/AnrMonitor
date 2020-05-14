package com.jack.anrmonitor;

import android.os.Looper;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * 用来记录ANR时，各线程的堆栈信息
 * 主线程堆栈信息在最前面
 * 另外：并不一定造成的ANR的真正原因是{@link AnrException}
 *
 * @author jack
 * @since 2020/5/14 09:22
 */
public class AnrException extends Error {

    /**
     * 主线程被阻塞的持续时长
     */
    private final long duration;

    private AnrException(StackTrace.AnrThrowable stat, long duration) {
        super("应用程序没有响应至少" + duration + "ms", stat);
        this.duration = duration;
    }

    @Override
    public Throwable fillInStackTrace() {
        setStackTrace(new StackTraceElement[]{});
        return this;
    }

    /**
     * 生成一个自定义记录堆栈信息的AnrException
     *
     * @param filterStr                   过滤Thread.name针对主线程无效
     * @param logThreadsWithoutStackTrace 是否记录无堆栈信息的Thread
     */
    public static AnrException createThreadException(long duration, String filterStr, boolean logThreadsWithoutStackTrace) {
        final Thread mainThread = Looper.getMainLooper().getThread();
        //给堆栈信息排序，主线程在最前面。
        Map<Thread, StackTraceElement[]> stackTracesMap = new TreeMap<>(new Comparator<Thread>() {
            @Override
            public int compare(Thread lhs, Thread rhs) {
                if (lhs == rhs)
                    return 0;
                if (lhs == mainThread)
                    return 1;
                if (rhs == mainThread)
                    return -1;
                return rhs.getName().compareTo(lhs.getName());
            }
        });
        //遍历所有线程堆栈信息，保存至stackTracesMap中
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            if (entry.getKey() == mainThread || (entry.getKey().getName().startsWith(filterStr == null ? "" : filterStr) && (logThreadsWithoutStackTrace || entry.getValue().length > 0)))
                stackTracesMap.put(entry.getKey(), entry.getValue());
        }

        // 有时主线程在getallstacktrace()中不返回，我们必须记录
        if (!stackTracesMap.containsKey(mainThread)) {
            stackTracesMap.put(mainThread, mainThread.getStackTrace());
        }
        StackTrace.AnrThrowable stat = null;
        for (Map.Entry<Thread, StackTraceElement[]> threadEntry : stackTracesMap.entrySet()) {
            stat = new StackTrace(threadEntry.getKey(), threadEntry.getValue()).new AnrThrowable(stat);
        }
        return new AnrException(stat, duration);
    }

    /**
     * 生成只记录主线程堆栈信息的AnrException
     */
    static AnrException createMainThreadException(long duration) {
        Thread mainThread = Looper.getMainLooper().getThread();
        StackTraceElement[] mainStackTrace = mainThread.getStackTrace();
        return new AnrException(new StackTrace(mainThread, mainStackTrace).new AnrThrowable(null), duration);
    }

    /**
     * 各线程堆栈信息
     */
    private static class StackTrace implements Serializable {
        /**
         * 线程
         */
        private Thread thread;
        /**
         * 堆栈信息
         */
        private StackTraceElement[] stackTraceElements;

        private StackTrace(Thread thread, StackTraceElement[] stackTraceElements) {
            this.thread = thread;
            this.stackTraceElements = stackTraceElements;
        }

        /**
         * 记录堆栈信息中的异常
         */
        private class AnrThrowable extends Throwable {
            private AnrThrowable(AnrThrowable other) {
                super(thread.getName() + "(state=" + thread.getState() + ")", other);
            }

            @Override
            public Throwable fillInStackTrace() {
                setStackTrace(stackTraceElements);
                return this;
            }
        }
    }
}
