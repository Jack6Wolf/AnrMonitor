package com.jack.fpsmonitor;

/**
 * 用户自定义回调的FrameCallback
 *
 * @author jack
 * @since 2020/6/17
 */
public interface FrameDataCallback {
    /**
     * 回调中的每个doFrame()都要调用它要非常明智地使用它。
     * 从这里同步日志记录不是一个好主意，因为每隔16-32ms就会调用一次doFrame。
     *
     * @param previousFrameNS 前一个垂直同步帧时间
     * @param currentFrameNS  当前垂直同步帧时间
     * @param droppedFrames   当前时间和以前时间之间被丢弃帧的数量
     */
    void doFrame(long previousFrameNS, long currentFrameNS, int droppedFrames);
}
