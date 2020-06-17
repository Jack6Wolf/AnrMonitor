package com.jack.fpsmonitor;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 帧率检测计算类
 *
 * @author jack
 * @since 2020/6/17 11:21
 */
public class Calculation {
    /**
     * 悬浮窗显示的类型
     */
    public enum Metric {
        /**
         * 正常的不丢帧，绿色显示
         */
        GOOD,
        /**
         * 中等的丢帧，黄色显示
         */
        MEDIUM,
        /**
         * 丢帧非常严重。红色显示
         */
        BAD
    }


    /**
     * 返回固定采样里丢帧数量的集合
     */
    public static List<Integer> getDroppedSet(FpsConfig fpsConfig, List<Long> dataSet) {
        List<Integer> droppedSet = new ArrayList<>();
        long start = -1;
        for (Long value : dataSet) {
            if (start == -1) {
                start = value;
                continue;
            }

            int droppedCount = droppedCount(start, value, fpsConfig.deviceRefreshRateInMs);
            if (droppedCount > 0) {
                droppedSet.add(droppedCount);
            }
            start = value;
        }
        return droppedSet;
    }

    /**
     * 返回丢失的帧数
     */
    public static int droppedCount(long start, long end, float devRefreshRate) {
        int count = 0;
        long diffNs = end - start;
        //纳秒转换为毫秒
        long diffMs = TimeUnit.MILLISECONDS.convert(diffNs, TimeUnit.NANOSECONDS);
        //16.6ms
        long dev = Math.round(devRefreshRate);
        if (diffMs > dev) {
            long droppedCount = (diffMs / dev);
            count = (int) droppedCount;
        }

        return count;
    }

    /**
     * 计算帧率，当前悬浮窗的类型
     */
    public static AbstractMap.SimpleEntry<Metric, Long> calculateMetric(FpsConfig fpsConfig,
                                                                        List<Long> dataSet, List<Integer> droppedSet) {
        long timeInNS = dataSet.get(dataSet.size() - 1) - dataSet.get(0);
        //获取dataSet尾部到头部时间间隔理应显示的帧数
        long size = getNumberOfFramesInSet(timeInNS, fpsConfig);

        // 总丢帧数
        int dropped = 0;

        for (Integer k : droppedSet) {
            dropped += k;
        }
        //基数与60做对比
        float multiplier = fpsConfig.refreshRate / size;
        //丢帧的每秒帧数
        float answer = multiplier * (size - dropped);
        long realAnswer = Math.round(answer);

        //计算需要显示 Metric类型
        //丢帧比例
        float percentOver = (float) dropped / (float) size;
        Metric metric = Metric.GOOD;
        if (percentOver >= fpsConfig.redFlagPercentage) {
            metric = Metric.BAD;
        } else if (percentOver >= fpsConfig.yellowFlagPercentage) {
            metric = Metric.MEDIUM;
        }

        return new AbstractMap.SimpleEntry<>(metric, realAnswer);
    }

    /**
     * 获取该时间段理论应该显示的帧数
     */
    protected static long getNumberOfFramesInSet(long realSampleLengthNs, FpsConfig fpsConfig) {
        float realSampleLengthMs = TimeUnit.MILLISECONDS.convert(realSampleLengthNs, TimeUnit.NANOSECONDS);
        float size = realSampleLengthMs / fpsConfig.deviceRefreshRateInMs;
        return Math.round(size);
    }

}
