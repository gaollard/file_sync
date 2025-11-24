package com.example.test_filesync;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import com.baidu.location.LocationClient;
import com.example.test_filesync.service.LocationService;
import com.example.test_filesync.util.LogUtils;

public class StudentApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    // 百度地图 SDK 隐私合规设置，必须在创建 LocationClient 之前调用
    LocationClient.setAgreePrivacy(true);
    
    // 检查权限后再启动服务
    if (checkRequiredPermissions()) {
      LogUtils.i(this, "StudentApplication", "权限检查通过，启动 LocationService");
      startLocationService();
    } else {
      LogUtils.w(this, "StudentApplication", "缺少必要权限，延迟启动 LocationService");
      // 权限未授予时，服务会在用户授予权限后由其他组件启动
      // 或者可以在 MainActivity 中请求权限后再启动服务
    }
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
}
