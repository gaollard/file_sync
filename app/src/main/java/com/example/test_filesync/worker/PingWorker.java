package com.example.test_filesync.worker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.example.test_filesync.service.LocationService;
import com.example.test_filesync.util.LogUtils;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

// 监听 SharedPreferences 变动，如果变动，则执行后台任务
public class PingWorker extends Worker implements SharedPreferences.OnSharedPreferenceChangeListener {

  private static final String TAG = "PingWorker";
  private static final long PING_INTERVAL_MS = 5000; // 5秒间隔
  public static final String PING_SP_NAME = "ping"; // 改为 public，供外部访问
  private static final String KEY_LAST_PING_COUNT = "lastPingCount"; // 上次的 pingCount 值

  private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    LogUtils.d(this.getApplicationContext(), "WorkManager", "SharedPreferences 变化检测到，key: " + key);
  }

  public PingWorker(Context context, WorkerParameters workerParams) {
    super(context, workerParams);
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

      // 判断 LocationService 是否开启，如果未开启，则开启 LocationService
      if (!LocationService.isRunning) {
        LocationService.isRunning = true;
        Intent intent = new Intent(context, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent);
        } else {
          context.startService(intent);
        }
      }

      // 更新上次的值
      sp.edit().putInt("pingCount", pingCount).apply();

      LogUtils.d(context, TAG, "后台任务执行完成，总计数: " + pingCount);

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
