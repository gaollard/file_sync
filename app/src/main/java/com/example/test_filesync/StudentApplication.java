package com.example.test_filesync;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.baidu.location.LocationClient;
import com.example.test_filesync.service.LocationService;
import com.example.test_filesync.util.LogUtils;

public class StudentApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    // 百度地图 SDK 隐私合规设置，必须在创建 LocationClient 之前调用
    LocationClient.setAgreePrivacy(true);
    
    // 应用启动时自动启动 LocationService，确保服务持续运行
    startLocationService();
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
}
