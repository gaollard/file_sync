package com.example.test_filesync.worker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.example.test_filesync.api.ApiCallback;
import com.example.test_filesync.api.ApiConfig;
import com.example.test_filesync.api.dto.UserInfo;
import com.example.test_filesync.service.LocationService;
import com.example.test_filesync.util.HttpUtil;
import com.example.test_filesync.util.LogUtils;
import com.example.test_filesync.util.PullConfig;
import com.google.gson.Gson;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class PingWorker extends Worker {

  private static final String TAG = "PingWorker";
  private static final long PING_INTERVAL_MS = 5000; // 5秒间隔
  public static final String PING_SP_NAME = "ping"; // 改为 public，供外部访问
  private static final String KEY_LAST_PING_COUNT = "lastPingCount"; // 上次的 pingCount 值

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
      PullConfig.pullConfig(context);
      pingCount++;
      // 更新上次的值
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
        sp.edit().putInt("pingCount", pingCount).apply();
      }
      LogUtils.d(context, TAG, "后台任务执行完成，总计数: " + pingCount);
      // 返回成功结果
      return Result.success();
    } catch (Exception e) {
      LogUtils.e(context, TAG, "任务执行失败", e);
      return Result.failure();
    }
  }
}
