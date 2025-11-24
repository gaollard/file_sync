package com.example.test_filesync;
import android.app.Application;
import android.content.Context;
import com.baidu.location.LocationClient;

public class StudentApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    // 百度地图 SDK 隐私合规设置，必须在创建 LocationClient 之前调用
    LocationClient.setAgreePrivacy(true);
  }
}
