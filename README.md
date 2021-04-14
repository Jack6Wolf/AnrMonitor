
# 卡顿监控——AnrMonitor
# 实现原理
> 参考Android WatchDog机制（com.android.server.WatchDog.java）

创建一个单独线程向主线程发送一个变量置0的操作，

自我休眠自定义ANR的阈值，休眠过后判断变量是否置0完成，如果未完成则告警。

## 原理图




## 核心代码
AnrMonitor本身就是一个线程。
```java
public class AnrMonitor extends Thread {
        ....省略部分代码...   

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

        ....省略部分代码...

}
```

根据上述代码，该线程

1. 先判断是否继续监控（针对停止监控）

2. tick+N(检测周期)

3. 给主线程发送一个tick置0操作的message

4. sleep Nms

5. 休眠后检测tick是否置0

6. 不为0，则生成AnrException,触发卡顿

7. 重置，进行下一轮的监控





## UI性能监控——FpsMonitor
### 实现原理
 

利用Android系统的Choreographer

在 Android API 16 中提供了 Choreographer 这个类，它会在每一帧绘制之前通过FrameCallback接口的方式回调给上层，并且提供了当前帧开始绘制的时间(单位：纳秒)。

关于 Choregrapher 工作原理如下：

 

我们可以给 Choregrapher 注册一个 FrameCallback 回调，那么系统在每一帧开始绘制的时候，会通过 FrameCallback#doFrame(...) 回调出来。之后，在这个回调中计算对应的 fps 即可。

### 核心代码
```java
public class FpsFrameCallback implements Choreographer.FrameCallback{
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

}
```
 

1. 首先判断FpsMonitor监控是否停用。

2. 是否设置了用户自定义FrameCallback回调，如果设置了，从绘制帧率时间集合中取出最后一帧时间作为上一帧时间，计算当前绘制帧时间之间的丢帧数回调。

3. 关键步骤，根据时间采样段将集合中的数据计算，再通过悬浮窗显示帧率

4. 添加当前绘帧时间到集合

5. 下一个帧回调注册

 

其中第3步我们分析一下，
```java
 //时间差值超过我们设置的数值时
if (isFinishedWithSample(frameTimeNanos)) {
         collectSampleAndSend(frameTimeNanos);
}
```

isFinishedWithSample规定了多久时间采一次样，间隔一定时间更新悬浮窗显示的当前帧率，因为每绘制一帧doFrame（）都会回调一次，所以

你不可能每次都计算或显示一次帧率，因为对于60fps的设备来说，正常情况下每16ms就会回调一次。显然这是不合理的。

而当时间差值超过我们规定的采样时间时，我们便会处理数据并显示帧率collectSampleAndSend（）。
```java
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
```
可以看到，首先会将集合，保存到showData中，然后清空集合，继续下一轮的收集。
```java
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
```

而当前showData通过Calculation.calculateMetric的计算获得当前的帧率，再通过悬浮窗TinyCoach展示出来。
