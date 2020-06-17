package com.jack.testanr;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AnrActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final Object mutex = new Object();

    private static void Sleep() {
        try {
            Thread.sleep(8 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void InfiniteLoop() {
        int i = 0;
        //noinspection InfiniteLoopStatement
        while (true) {
            i++;
        }
    }

    public class LockerThread extends Thread {

        LockerThread() {
            setName("APP: Locker");
        }

        @Override
        public void run() {
            synchronized (mutex) {
                //noinspection InfiniteLoopStatement
                while (true)
                    Sleep();
            }
        }
    }

    private void DeadLock() {
        new LockerThread().start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (mutex) {
                    Log.e("ANR-Failed", "在此消息之前应该有一个死锁！");
                }
            }
        }, 1000);
    }

    private int mode = 0;
    private boolean crash = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final App application = (App) getApplication();

        final Button minAnrDurationButton = (Button) findViewById(R.id.minAnrDuration);
        minAnrDurationButton.setText(application.duration + " 秒");
        minAnrDurationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                application.duration = application.duration % 6 + 2;
                minAnrDurationButton.setText(application.duration + " 秒");
            }
        });

        final Button reportModeButton = (Button) findViewById(R.id.reportMode);
        reportModeButton.setText("所有线程");
        reportModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mode = (mode + 1) % 3;
                switch (mode) {
                    case 0:
                        reportModeButton.setText("所有线程");
                        application.anrMonitor.setReportAllThreads();
                        break;
                    case 1:
                        reportModeButton.setText("主线程");
                        application.anrMonitor.setReportMainThreadOnly();
                        break;
                    case 2:
                        reportModeButton.setText("自主过滤");
                        application.anrMonitor.setReportThreadNameFilter("FinalizerWatchdogDaemon");
                        break;
                }
            }
        });

        final Button behaviourButton = (Button) findViewById(R.id.behaviour);
        behaviourButton.setText("崩溃");
        behaviourButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                crash = !crash;
                if (crash) {
                    behaviourButton.setText("崩溃");
                    application.anrMonitor.setAnrListener(null);
                } else {
                    behaviourButton.setText("打印");
                    application.anrMonitor.setAnrListener(application.silentListener);
                }
            }
        });

        findViewById(R.id.threadSleep).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "Sleep!");
                Sleep();
            }
        });

        findViewById(R.id.infiniteLoop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "InfiniteLoop!");
                InfiniteLoop();
            }
        });

        findViewById(R.id.deadlock).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "DeadLock!");
                DeadLock();
            }
        });

        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.anrMonitor.stopAnrMonitor();
            }
        });
    }
}
