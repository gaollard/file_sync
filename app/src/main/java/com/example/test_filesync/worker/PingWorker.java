package com.example.test_filesync.worker;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.test_filesync.util.LogUtils;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

// 监听 SharedPreferences 变动，如果变动，则执行后台任务
public class PingWorker extends Worker {
    private static final String TAG = "PingWorker";
    private static final long PING_INTERVAL_MS = 5000; // 5秒间隔
    public static final String PING_SP_NAME = "ping"; // 改为 public，供外部访问
    private static final String KEY_LAST_PING_COUNT = "lastPingCount"; // 上次的 pingCount 值

    public PingWorker(Context context, WorkerParameters workerParams) {
      super(context, workerParams);
    }

      /**
   * 调度 PingWorker 任务
   */
  private void schedulePingWork() {
    try {
      OneTimeWorkRequest workRequest =
          new OneTimeWorkRequest.Builder(PingWorker.class)
              .setInitialDelay(1, TimeUnit.MINUTES)
              .build();
      WorkManager.getInstance(this.getApplicationContext()).enqueue(workRequest);
      LogUtils.d(this.getApplicationContext(), "StudentApplication", "PingWorker 任务调度成功");
    } catch (Exception e) {
      LogUtils.e(this.getApplicationContext(), "StudentApplication", "调度 PingWorker 任务失败: " + e.getMessage(), e);
    }
  }

  @Override
  public Result doWork() {
    Context context = getApplicationContext();

    // 获取 sp
    SharedPreferences sp = context.getSharedPreferences(PING_SP_NAME, Context.MODE_PRIVATE);
    int pingCount = sp.getInt("pingCount", 0);

    try {
      Thread.sleep(5000); // 模拟耗时操作

      pingCount++;

      // 更新上次的值
      sp.edit().putInt("pingCount", pingCount).apply();

      LogUtils.d(context, TAG, "后台任务执行完成，总计数: " + pingCount);

      // 调度新的 PingWorker 任务
      schedulePingWork();

      // 返回成功结果
      return Result.success();

    } catch (InterruptedException e) {
      LogUtils.e(context, TAG, "任务被中断", e);
      return Result.failure();
    } catch (Exception e) {
      LogUtils.e(context, TAG, "任务执行失败", e);
      return Result.failure();
    }
  }
}
