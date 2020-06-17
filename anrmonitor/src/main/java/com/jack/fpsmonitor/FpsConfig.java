package com.jack.fpsmonitor;

import android.view.Gravity;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * FPS配置参数
 *
 * @author jack
 * @since 2020/6/17
 */
public class FpsConfig implements Serializable {
    public static int DEFAULT_GRAVITY = Gravity.TOP | Gravity.START;

    /**
     * BAD：丢帧比例
     */
    public float redFlagPercentage = 0.2f;
    /**
     * MEDIUM：丢帧比例
     */
    public float yellowFlagPercentage = 0.05f;
    /**
     * 设置帧率 ，默认：60fps
     */
    public float refreshRate = 60;
    /**
     * 设置帧速， 默认：16.6ms
     */
    public float deviceRefreshRateInMs = 16.6f;

    /**
     * 起始展示位置
     */
    public int startingXPosition = 500;
    public int startingYPosition = 200;
    public int startingGravity = DEFAULT_GRAVITY;
    public boolean xOrYSpecified = false;
    public boolean gravitySpecified = false;

    /**
     * 面对用户提供框架信息的回调
     */
    public FrameDataCallback frameDataCallback = null;

    /**
     * 采样间隔时间，即多久更新悬浮窗显示数据
     */
    public final long sampleTimeInMs = 736;//928;//736; // default sample time

    protected FpsConfig() {
    }

    /**
     * 毫秒转换成纳秒
     */
    public long getSampleTimeInNs() {
        return TimeUnit.NANOSECONDS.convert(sampleTimeInMs, TimeUnit.MILLISECONDS);
    }

    public long getDeviceRefreshRateInNs() {
        float value = deviceRefreshRateInMs * 1000000f;
        return (long) value;
    }
}
