package com.example.test_filesync;
import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import androidx.work.Configuration;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import android.content.SharedPreferences;

import com.baidu.location.LocationClient;
import com.example.test_filesync.service.LocationService;
import com.example.test_filesync.service.PingJobService;
import com.example.test_filesync.util.LogUtils;
import com.example.test_filesync.worker.PingWorker;

import java.util.concurrent.TimeUnit;

import cn.jpush.android.api.JPushInterface;

public class StudentApplication extends Application {
  private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
  @Override
  public void onCreate() {
    super.onCreate();

    // 初始化 WorkManager
    initializeWorkManager();

    // 百度地图 SDK 隐私合规设置，必须在创建 LocationClient 之前调用
    // LocationClient.setAgreePrivacy(true);

    // // 检查权限后再启动服务
    // if (checkRequiredPermissions()) {
    //   LogUtils.i(this, "StudentApplication", "权限检查通过，启动 LocationService");
    //   startLocationService();
    // } else {
    //   LogUtils.w(this, "StudentApplication", "缺少必要权限，延迟启动 LocationService");
    //   // 权限未授予时，服务会在用户授予权限后由其他组件启动
    //   // 或者可以在 MainActivity 中请求权限后再启动服务
    // }

    // 初始化极光推送
    initJPush();

    // 调度 PingJobService
//    schedulePingJob();

    // 调度 WorkManager 任务
    schedulePingWork();

    // 注册 SharedPreferences 监听器
    registerSharedPreferencesListener();
  }

  /**
   * 初始化 WorkManager
   */
  private void initializeWorkManager() {
    try {
      Configuration configuration = new Configuration.Builder()
          .setMinimumLoggingLevel(android.util.Log.INFO)
          .build();
      WorkManager.initialize(this, configuration);
      LogUtils.d(this, "StudentApplication", "WorkManager 初始化成功");
    } catch (IllegalStateException e) {
      // WorkManager 已经初始化，忽略异常
      LogUtils.d(this, "StudentApplication", "WorkManager 已经初始化");
    } catch (Exception e) {
      LogUtils.e(this, "StudentApplication", "WorkManager 初始化失败: " + e.getMessage(), e);
    }
  }

  /**
   * 调度 PingWorker 任务
   */
  private void schedulePingWork() {
    try {
      // 每隔15min调度一次 PingWorker 任务
      OneTimeWorkRequest workRequest =
          new OneTimeWorkRequest.Builder(PingWorker.class)
              .setInitialDelay(1, TimeUnit.MINUTES)
              .build();
      WorkManager.getInstance(this.getApplicationContext()).enqueue(workRequest);
      LogUtils.d(this, "StudentApplication", "PingWorker 任务调度成功");
    } catch (Exception e) {
      LogUtils.e(this, "StudentApplication", "调度 PingWorker 任务失败: " + e.getMessage(), e);
    }
  }

  /**
   * 注册 SharedPreferences 监听器
   * 当 SharedPreferences 变化时，自动触发 PingWorker 任务
   */
  private void registerSharedPreferencesListener() {
    try {
      SharedPreferences sp = getSharedPreferences(PingWorker.PING_SP_NAME, Context.MODE_PRIVATE);

      preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
          LogUtils.d(StudentApplication.this, "StudentApplication",
              "SharedPreferences 变化检测到，key: " + key);

          // 当指定的 key 变化时，触发 PingWorker 任务
          // 可以在这里添加过滤逻辑，只监听特定的 key
          if (key != null) {
            // 调度新的 PingWorker 任务
            schedulePingWorkOnPreferenceChange(key);
          }
        }
      };

      sp.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
      LogUtils.d(this, "StudentApplication", "SharedPreferences 监听器注册成功");
    } catch (Exception e) {
      LogUtils.e(this, "StudentApplication", "注册 SharedPreferences 监听器失败: " + e.getMessage(), e);
    }
  }

  /**
   * 当 SharedPreferences 变化时调度 PingWorker 任务
   */
  private void schedulePingWorkOnPreferenceChange(String changedKey) {
    try {
      OneTimeWorkRequest workRequest =
          new OneTimeWorkRequest.Builder(PingWorker.class)
              .build();
      WorkManager.getInstance(this).enqueue(workRequest);
      LogUtils.d(this, "StudentApplication",
          "SharedPreferences 变化触发 PingWorker 任务，变化的 key: " + changedKey);
    } catch (Exception e) {
      LogUtils.e(this, "StudentApplication",
          "SharedPreferences 变化触发任务失败: " + e.getMessage(), e);
    }
  }

  @Override
  public void onTerminate() {
    super.onTerminate();
    // 注销监听器，防止内存泄漏
    if (preferenceChangeListener != null) {
      try {
        SharedPreferences sp = getSharedPreferences(PingWorker.PING_SP_NAME, Context.MODE_PRIVATE);
        sp.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        LogUtils.d(this, "StudentApplication", "SharedPreferences 监听器已注销");
      } catch (Exception e) {
        LogUtils.e(this, "StudentApplication", "注销 SharedPreferences 监听器失败: " + e.getMessage(), e);
      }
    }
  }

  /**
   * 初始化极光推送SDK
   */
  private void initJPush() {
    try {
      // 设置是否开启日志，发布时请关闭日志
      JPushInterface.setDebugMode(true);

      // 初始化极光推送
      JPushInterface.init(this);

      // 获取注册ID
      String registrationId = JPushInterface.getRegistrationID(this);
      LogUtils.i(this, "StudentApplication", "极光推送初始化成功，RegistrationId: " + registrationId);

      // 设置推送通知栏样式（可选）
      // setPushNotificationStyle();
    } catch (Exception e) {
      LogUtils.e(this, "StudentApplication", "极光推送初始化失败: " + e.getMessage(), e);
    }
  }

  /**
   * 设置推送通知栏样式（可选）
   */
  private void setPushNotificationStyle() {
    // 可以自定义通知栏样式
    // 具体实现根据需求来定制
  }

  /**
   * 检查启动 LocationService 所需的权限
   * @return true 如果所有必要权限都已授予
   */
  private boolean checkRequiredPermissions() {
    // 检查位置权限
    boolean hasLocationPermission = false;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      int fineLocation = ContextCompat.checkSelfPermission(this,
          android.Manifest.permission.ACCESS_FINE_LOCATION);
      int coarseLocation = ContextCompat.checkSelfPermission(this,
          android.Manifest.permission.ACCESS_COARSE_LOCATION);
      hasLocationPermission = (fineLocation == PackageManager.PERMISSION_GRANTED) ||
          (coarseLocation == PackageManager.PERMISSION_GRANTED);

      if (!hasLocationPermission) {
        LogUtils.w(this, "StudentApplication", "位置权限未授予");
      }
    } else {
      // Android 6.0 以下版本，权限在安装时自动授予
      hasLocationPermission = true;
    }

    // 检查前台服务权限（Android 9+）
    boolean hasForegroundServicePermission = true;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      int foregroundService = ContextCompat.checkSelfPermission(this,
          android.Manifest.permission.FOREGROUND_SERVICE);
      hasForegroundServicePermission = (foregroundService == PackageManager.PERMISSION_GRANTED);

      if (!hasForegroundServicePermission) {
        LogUtils.w(this, "StudentApplication", "FOREGROUND_SERVICE 权限未授予");
      }
    }

    // 检查前台服务位置类型权限（Android 14+）
    boolean hasForegroundServiceLocationPermission = true;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      int foregroundServiceLocation = ContextCompat.checkSelfPermission(this,
          android.Manifest.permission.FOREGROUND_SERVICE_LOCATION);
      hasForegroundServiceLocationPermission =
          (foregroundServiceLocation == PackageManager.PERMISSION_GRANTED);

      if (!hasForegroundServiceLocationPermission) {
        LogUtils.w(this, "StudentApplication", "FOREGROUND_SERVICE_LOCATION 权限未授予");
      }
    }

    boolean allPermissionsGranted = hasLocationPermission &&
        hasForegroundServicePermission &&
        hasForegroundServiceLocationPermission;

    if (!allPermissionsGranted) {
      LogUtils.w(this, "StudentApplication",
          "部分权限未授予 - 位置:" + hasLocationPermission +
          ", 前台服务:" + hasForegroundServicePermission +
          ", 前台服务位置:" + hasForegroundServiceLocationPermission);
    }

    return allPermissionsGranted;
  }

  private void startLocationService() {
    try {
      Intent serviceIntent = new Intent(this, LocationService.class);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(serviceIntent);
      } else {
        startService(serviceIntent);
      }
      LogUtils.i(this, "StudentApplication", "LocationService 已在应用启动时启动");
    } catch (Exception e) {
      LogUtils.e(this, "StudentApplication", "启动 LocationService 失败: " + e.getMessage(), e);
    }
  }

  /**
   * 调度 PingJobService
   */
  private void schedulePingJob() {
    try {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        LogUtils.w(this, "StudentApplication", "JobScheduler 需要 Android 5.0+，当前版本不支持");
        return;
      }

      JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
      if (jobScheduler == null) {
        LogUtils.e(this, "StudentApplication", "无法获取 JobScheduler 服务");
        return;
      }

      // 先取消之前的任务，避免重复调度
      jobScheduler.cancel(1);
      LogUtils.d(this, "StudentApplication", "已取消之前的任务");

      ComponentName componentName = new ComponentName(this, PingJobService.class);
      JobInfo.Builder jobBuilder = new JobInfo.Builder(1, componentName)
          .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE) // 不需要网络
          .setRequiresCharging(false) // 不需要充电
          .setRequiresDeviceIdle(false) // 不需要设备空闲
          .setPersisted(true); // 持久化，应用退出后仍可执行

      // 设置立即执行（延迟0毫秒）
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        jobBuilder.setMinimumLatency(0); // 最小延迟0毫秒
        jobBuilder.setOverrideDeadline(1000); // 最晚1秒内执行
      }

      JobInfo jobInfo = jobBuilder.build();
      int result = jobScheduler.schedule(jobInfo);

      if (result == JobScheduler.RESULT_SUCCESS) {
        LogUtils.i(this, "StudentApplication", "PingJobService 调度成功");
      } else {
        LogUtils.e(this, "StudentApplication", "PingJobService 调度失败，返回码: " + result);
      }
    } catch (Exception e) {
      LogUtils.e(this, "StudentApplication", "调度 PingJobService 失败: " + e.getMessage(), e);
    }
  }
}
