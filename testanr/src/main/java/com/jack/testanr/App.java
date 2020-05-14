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

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * @author jack
 * @since 2020/5/9 13:29
 */
public class App extends Application {
    private static final String TAG = "App";
    AnrMonitor anrMonitor = new AnrMonitor(2000);
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
                            Log.w(TAG, "主线程被阻塞(" + duration + " ms), 过" + ret + " ms，仍然被阻塞会造成ANR",anrException);
                        }
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

}
