package com.jack.testanr;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.jack.anrmonitor.AnrException;
import com.jack.anrmonitor.AnrMonitor;
import com.jack.anrmonitor.AnrMonitorListener;
import com.jack.monitor.Monitor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * @author jack
 * @since 2020/5/9 13:29
 */
public class App extends Application {
    private static final String TAG = "App";
    AnrMonitor anrMonitor;
    private File logFile = new File(Environment.getExternalStorageDirectory(), "AnrLog.txt");
    /**
     * 发生anr的响应时长
     */
    int duration = 6;

    final AnrMonitorListener.AnrListener silentListener = new AnrMonitorListener.AnrListener() {
        @Override
        public void onAppNotResponding(AnrException anrException) {
            Log.e(TAG, "", anrException);
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        anrMonitor = Monitor.install().startAnrCustom(this, 2000);
        anrMonitor
                .setAnrListener(new AnrMonitorListener.AnrListener() {
                    @Override
                    public void onAppNotResponding(AnrException anrException) {
                        //把错误信息保存到本地
//                        handlelException(error);
                        throw anrException;
                    }
                })
                .setAnrInterceptor(new AnrMonitorListener.AnrInterceptor() {
                    @Override
                    public long intercept(long duration, AnrException anrException) {
                        long ret = App.this.duration * 1000 - duration;
                        if (ret > 0) {
                            Log.w(TAG, "主线程被阻塞(" + duration + " ms), 过" + ret + " ms，仍然被阻塞会造成ANR", anrException);
                        }
                        //震动提醒
                        anrMonitor.vibrator();
                        return ret;
                    }
                })
                .setInterruptionListener(new AnrMonitorListener.InterruptionListener() {
                    @Override
                    public void onInterrupted(InterruptedException exception) {
                        Log.w(TAG, "", exception);
                    }
                });

        anrMonitor.start();
        anrMonitor.setIgnoreDebugger(true);
//快捷启动Monitor
//        Monitor.install().start(this);

//        Monitor.install().startAnrCustom(this,2000)
//                //阻塞时是否只记录主线程堆栈信息
//                .setReportMainThreadOnly()
//                //阻塞时根据线程名关键字过滤堆栈信息
//                .setReportThreadNameFilter("Main")
//                //阻塞时记录所有线程堆栈信息
//                .setReportAllThreads()
//                //是否忽略Debug模式下的阻塞
//                .setIgnoreDebugger()
//                //阻塞时是否记录不含堆栈信息的线程
//                .setLogThreadsWithoutStackTrace()
//                //发生阻塞时拦截监听
//                .setAnrInterceptor()
//                //发生阻塞时的监听
//                .setAnrListener()
//                //最后必须调用
//                .start();

//        Monitor.install().startFpsCustom()
//                //用户自定义帧率监控回调，返回前一个垂直同步帧时间，当前垂直同步帧时间，当前时间和以前时间之间被丢弃帧的数量
//                .addFrameDataCallback()
//                //帧率显示黄色预警的丢帧比例
//                .yellowFlagPercentage()
//                //帧率显示红色预警的丢帧比例
//                .redFlagPercentage()
//                //帧率显示器方位
//                .startingGravity()
//                //帧率显示器在X轴的坐标
//                .startingXPosition()
//                //帧率显示器在Y轴的坐标
//                .startingYPosition()
//                //帧率显示器隐藏
//                .hide()
//                //最后必须调用（帧率显示器显示）
//                .show(this);

    }


    /**
     * 记录异常信息
     */
    private boolean handlelException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        PrintWriter pw = null;
        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            pw = new PrintWriter(logFile);

            // 收集手机及错误信息
            collectInfoToSDCard(pw, ex);
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }


    /**
     * 将异常日志转换为字符串
     */
    public static String getException(Throwable throwable) {
        Writer writer = null;
        PrintWriter printWriter = null;
        try {
            writer = new StringWriter();
            printWriter = new PrintWriter(writer);
            throwable.printStackTrace(printWriter);
            return writer.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (printWriter != null)
                printWriter.close();
        }
        return null;
    }

    /**
     * 收集记录错误信息
     *
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    @SuppressLint("SimpleDateFormat")
    private void collectInfoToSDCard(PrintWriter pw, Throwable ex) throws PackageManager.NameNotFoundException, IllegalAccessException, IllegalArgumentException {
        PackageManager pm = this.getPackageManager();
        PackageInfo mPackageInfo = pm.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);

        pw.println("time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())); // 记录错误发生的时间
        pw.println("versionCode: " + mPackageInfo.versionCode); // 版本号
        pw.println("versionName: " + mPackageInfo.versionName); // 版本名称

        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            pw.print(field.getName() + " : ");
            pw.println(field.get(null).toString());
        }
        ex.printStackTrace(pw);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Monitor.install().stop(this);
    }
}
