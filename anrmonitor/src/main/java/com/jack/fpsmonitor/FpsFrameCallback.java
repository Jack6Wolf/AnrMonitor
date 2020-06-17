package com.jack.fpsmonitor;

import android.annotation.SuppressLint;
import android.view.Choreographer;

import com.jack.fpsmonitor.ui.TinyCoach;

import java.util.ArrayList;
import java.util.List;

/**
 * Choreographer.FrameCallback
 *
 * @author jack
 * @since 2020/6/17
 */
@SuppressLint("NewApi")
public class FpsFrameCallback implements Choreographer.FrameCallback {
    private FpsConfig fpsConfig;
    private TinyCoach tinyCoach;
    /**
     * 保存样本集的帧时间
     */
    private List<Long> dataSet;
    private boolean enabled = true;
    private long startSampleTimeInNs = 0;

    public FpsFrameCallback(FpsConfig fpsConfig, TinyCoach tinyCoach) {
        this.fpsConfig = fpsConfig;
        this.tinyCoach = tinyCoach;
        dataSet = new ArrayList<>();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        //如果没有启用，我们现在退出，不注册回调
        if (!enabled) {
            destroy();
            return;
        }

        //初试时间
        if (startSampleTimeInNs == 0) {
            startSampleTimeInNs = frameTimeNanos;
        }
        // 仅为回调调用....
        else if (fpsConfig.frameDataCallback != null) {
            //取出最后一次时间作为开始时间
            long start = dataSet.get(dataSet.size() - 1);
            //计算丢失的帧数
            int droppedCount = Calculation.droppedCount(start, frameTimeNanos, fpsConfig.deviceRefreshRateInMs);
            fpsConfig.frameDataCallback.doFrame(start, frameTimeNanos, droppedCount);
        }

        //时间差值超过我们设置的数值时：736ms
        if (isFinishedWithSample(frameTimeNanos)) {
            collectSampleAndSend(frameTimeNanos);
        }

        //添加当前帧时间到data
        dataSet.add(frameTimeNanos);

        //我们需要为下一个帧回调注册（必须的）
        Choreographer.getInstance().postFrameCallback(this);
    }

    /**
     * 上报数据开始采集数据上报悬浮窗
     *
     * @param frameTimeNanos
     */
    private void collectSampleAndSend(long frameTimeNanos) {
        //只有时间差值大于736ms内
        List<Long> dataSetCopy = new ArrayList<>();
        dataSetCopy.addAll(dataSet);

        //向悬浮窗推送数据
        tinyCoach.showData(fpsConfig, dataSetCopy);

        // clear data
        dataSet.clear();

        //重置样本定时器到最后一帧
        startSampleTimeInNs = frameTimeNanos;
    }

    /**
     * 当样丢帧超过规定值时
     */
    private boolean isFinishedWithSample(long frameTimeNanos) {
        return frameTimeNanos - startSampleTimeInNs > fpsConfig.getSampleTimeInNs();
    }

    private void destroy() {
        dataSet.clear();
        fpsConfig = null;
        tinyCoach = null;
    }

}
