package com.jack.fpsmonitor.ui;

import android.animation.Animator;
import android.app.Application;
import android.app.Service;
import android.graphics.PixelFormat;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.jack.anrmonitor.R;
import com.jack.fpsmonitor.Calculation;
import com.jack.fpsmonitor.FpsConfig;

import java.util.AbstractMap;
import java.util.List;

/**
 * 悬浮窗
 *
 * @author jack
 * @since 2020/6/17
 */
public class TinyCoach {
    private FpsConfig fpsConfig;
    private TextView meterView;
    private final WindowManager windowManager;
    private int shortAnimationDuration = 200, longAnimationDuration = 700;

    /**
     * 监听双击，这样我们就可以隐藏悬浮窗
     */
    private GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // 隐藏但不删除视图
            hide(false);
            return super.onDoubleTap(e);
        }
    };

    public TinyCoach(Application context, FpsConfig config) {

        fpsConfig = config;

        meterView = (TextView) LayoutInflater.from(context).inflate(R.layout.meter_view, null);

        //设置初始fps值
        meterView.setText(String.valueOf((int) fpsConfig.refreshRate));

        // 获取窗口管理器并向窗口添加视图
        windowManager = (WindowManager) meterView.getContext().getSystemService(Service.WINDOW_SERVICE);

        int minWidth = meterView.getLineHeight()
                + meterView.getPaddingTop()
                + meterView.getPaddingBottom()
                + (int) meterView.getPaint().getFontMetrics().bottom;
        meterView.setMinWidth(minWidth);

        addViewToWindow(meterView);
    }

    /**
     * 向windowManager添加view
     */
    private void addViewToWindow(View view) {

        int permissionFlag = PermissionCompat.getFlag();

        WindowManager.LayoutParams paramsF = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                permissionFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        // 配置开始坐标
        if (fpsConfig.xOrYSpecified) {
            paramsF.x = fpsConfig.startingXPosition;
            paramsF.y = fpsConfig.startingYPosition;
            paramsF.gravity = FpsConfig.DEFAULT_GRAVITY;
        } else if (fpsConfig.gravitySpecified) {
            paramsF.x = 0;
            paramsF.y = 0;
            paramsF.gravity = fpsConfig.startingGravity;
        } else {
            paramsF.gravity = FpsConfig.DEFAULT_GRAVITY;
            paramsF.x = fpsConfig.startingXPosition;
            paramsF.y = fpsConfig.startingYPosition;
        }

        // 向窗口添加视图
        windowManager.addView(view, paramsF);

        // 创建手势检测器来监听双击
        GestureDetector gestureDetector = new GestureDetector(view.getContext(), simpleOnGestureListener);

        // 增加接触侦听器
        view.setOnTouchListener(new DancerTouchListener(paramsF, windowManager, gestureDetector));

        // 禁用触觉反馈
        view.setHapticFeedbackEnabled(false);

        show();
    }

    /**
     * 展示数据
     *
     * @param dataSet 采样间隔里的每一帧显示当前时间集合
     */
    public void showData(FpsConfig fpsConfig, List<Long> dataSet) {

        List<Integer> droppedSet = Calculation.getDroppedSet(fpsConfig, dataSet);
        AbstractMap.SimpleEntry<Calculation.Metric, Long> answer = Calculation.calculateMetric(fpsConfig, dataSet, droppedSet);

        if (answer.getKey() == Calculation.Metric.BAD) {
            meterView.setBackgroundResource(R.drawable.fpsmeterring_bad);
        } else if (answer.getKey() == Calculation.Metric.MEDIUM) {
            meterView.setBackgroundResource(R.drawable.fpsmeterring_medium);
        } else {
            meterView.setBackgroundResource(R.drawable.fpsmeterring_good);
        }

        meterView.setText(String.valueOf(answer.getValue()));
    }

    /**
     * 移除悬浮窗
     */
    public void destroy() {
        meterView.setOnTouchListener(null);
        hide(true);
    }

    /**
     * 显示 动画
     */
    public void show() {
        meterView.setAlpha(0f);
        meterView.setVisibility(View.VISIBLE);
        meterView.animate()
                .alpha(1f)
                .setDuration(longAnimationDuration)
                .setListener(null);
    }

    /**
     * 关闭 隐藏
     */
    public void hide(final boolean remove) {
        meterView.animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        meterView.setVisibility(View.GONE);
                        if (remove) {
                            windowManager.removeView(meterView);
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });

    }
}
