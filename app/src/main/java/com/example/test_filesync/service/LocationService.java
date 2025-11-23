package com.example.test_filesync.service;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.example.test_filesync.R;
import com.example.test_filesync.util.LogUtils;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final long LOCATION_UPDATE_INTERVAL = 5000; // 5秒

    private Handler handler = new Handler(Looper.getMainLooper());
    private LocationManager locationManager;
    private LocationListener locationListener;
    private NotificationManager notificationManager;

    // 定时任务
    private Runnable locationTask = new Runnable() {
        @Override
        public void run() {
            getCurrentLocation();
            // 重新调度下一次任务
            handler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            startForegroundService();
            initializeLocationListener();
            startPeriodicLocationTask();
        } catch (Exception e) {
            Toast.makeText(this, "LocationService 初始化错误：" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "onCreate error", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // 服务被关闭后自动重新启动
    }

    // 初始化位置监听器
    private void initializeLocationListener() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // 位置更新时的回调（这里不使用，因为我们使用定时主动获取）
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };
    }

    // 开始定时获取位置任务
    private void startPeriodicLocationTask() {
        handler.post(locationTask);
    }

    // 获取当前位置
    private void getCurrentLocation() {
        try {
            // 检查位置权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "位置权限未授予");
                    return;
                }
            }

            Location location = null;
            

            // 优先使用 GPS 定位
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(this, "使用 GPS 定位", Toast.LENGTH_SHORT).show();
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            // 如果 GPS 不可用，使用网络定位
            if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Toast.makeText(this, "使用网络定位", Toast.LENGTH_SHORT).show();
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            // 如果网络定位也不可用，使用被动定位
            if (location == null && locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                Toast.makeText(this, "使用被动定位", Toast.LENGTH_SHORT).show();
                location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }

            if (location != null) {
                // 打印位置信息
                printLocationInfo(location);
            } else {
                Toast.makeText(this, "无法获取位置信息，可能定位服务未开启或位置数据不可用", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "无法获取位置信息，可能定位服务未开启或位置数据不可用");
            }

        } catch (Exception e) {
            Log.e(TAG, "获取位置信息时出错", e);
        }
    }

    // 打印位置信息
    private void printLocationInfo(Location location) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timeStr = sdf.format(new Date());

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        float accuracy = location.getAccuracy();
        double altitude = location.getAltitude();
        float speed = location.getSpeed();
        long time = location.getTime();

        // 这个位置为何和百度地图不一致？
        // 因为百度地图使用的是高德地图的API，而高德地图的API使用的是百度地图的API，所以位置不一致。
        // 所以需要使用百度地图的API来获取位置信息。
        // 但是百度地图的API需要收费，所以需要使用高德地图的API来获取位置信息。
        // 所以需要使用高德地图的API来获取位置信息。
        // 所以需要使用高德地图的API来获取位置信息。

        String locationInfo = String.format(
            Locale.getDefault(),
            "时间: %s\n" +
            "纬度: %.6f\n" +
            "经度: %.6f\n" +
            "精度: %.2f 米\n" +
            "海拔: %.2f 米\n" +
            "速度: %.2f m/s\n" +
            "定位时间: %s",
            timeStr,
            latitude,
            longitude,
            accuracy,
            altitude,
            speed,
            sdf.format(new Date(time))
        );

        Toast.makeText(this, "位置信息: " + locationInfo, Toast.LENGTH_SHORT).show();


        // 使用 Log 打印
        Log.i(TAG, "========== 位置信息 ==========");
        Log.i(TAG, locationInfo);
        Log.i(TAG, "============================");

        // 也可以更新通知显示位置信息
        updateNotification(locationInfo);
    }

    // 创建通知渠道并启动前台服务
    private void startForegroundService() {
        try {
            // 创建通知通道
            NotificationChannel channel = new NotificationChannel(
                "location_channel",
                "位置服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setSound(null, null); // 禁用通知音
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); // 锁屏可见
            channel.enableVibration(false); // 禁用震动

            notificationManager.createNotificationChannel(channel);

            // 启动前台服务
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    3,
                    createNotification("位置服务运行中"),
                    FOREGROUND_SERVICE_TYPE_LOCATION
                );
            } else {
                startForeground(3, createNotification("位置服务运行中"));
            }

            Toast.makeText(this, "位置服务已启动", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "位置服务已启动");
        } catch (Exception e) {
            Toast.makeText(this, "启动位置服务错误：" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "startForegroundService error", e);
        }
    }

    // 创建通知
    private Notification createNotification(String text) {
        return new NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("位置服务")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_location)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    // 更新通知显示位置信息
    private void updateNotification(String locationInfo) {
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(new Date());
        Notification notification = new NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("位置服务")
            .setContentText("最后更新: " + currentTime)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(locationInfo))
            .setSmallIcon(R.mipmap.ic_location)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
        notificationManager.notify(3, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 移除定时任务
        handler.removeCallbacks(locationTask);

        // 移除位置监听器
        if (locationManager != null && locationListener != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        locationManager.removeUpdates(locationListener);
                    }
                } else {
                    locationManager.removeUpdates(locationListener);
                }
            } catch (Exception e) {
                Log.e(TAG, "移除位置监听器时出错", e);
            }
        }

        Log.i(TAG, "位置服务已停止");
    }

    @Override
    public android.os.IBinder onBind(Intent intent) {
        return null;
    }
}
