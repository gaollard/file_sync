package com.example.test_filesync.service;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.example.test_filesync.R;
import com.example.test_filesync.util.AppInfo;
import com.example.test_filesync.util.LogUtils;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final long LOCATION_UPDATE_INTERVAL = 60000; // 1分钟

    private Handler handler = new Handler(Looper.getMainLooper());
    private LocationClient locationClient;
    private NotificationManager notificationManager;
    private boolean isLocationClientStarted = false;
    private int startCheckRetryCount = 0;
    private static final int MAX_START_CHECK_RETRY = 10; // 最大重试次数

    // 百度定位监听器
    private BDAbstractLocationListener locationListener = new BDAbstractLocationListener() {
        // onReceiveLocation 什么时候被调用？
        // 1. 当定位成功时，会调用 onReceiveLocation 方法
        // 2. 当定位失败时，会调用 onReceiveLocation 方法
        // 3. 当定位结果发生变化时，会调用 onReceiveLocation 方法
        // 4. 当定位客户端启动时，会调用 onReceiveLocation 方法
        // 5. 当定位客户端停止时，会调用 onReceiveLocation 方法
        // 6. 当定位客户端被销毁时，会调用 onReceiveLocation 方法
        // 7. 当定位客户端被重新启动时，会调用 onReceiveLocation 方法
        // 8. 当定位客户端被重新启动时，会调用 onReceiveLocation 方法
        // 9. 当定位客户端被重新启动时，会调用 onReceiveLocation 方法
        @Override
        public void onReceiveLocation(BDLocation location) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(new Date());
            LogUtils.i(LocationService.this, String.format(Locale.getDefault(),
                "[%s] onReceiveLocation 被触发，定位类型: %d", timestamp,
                location != null ? location.getLocType() : -1));
            if (location != null) {
                int locType = location.getLocType();
                // 检查定位类型，判断是否为错误
                // 61: GPS定位结果 (TypeGpsLocation)
                // 66: 离线定位结果 (TypeOffLineLocation)
                // 161: 网络定位结果 (TypeNetWorkLocation)
                // 其他值通常表示错误
                if (locType == 61 || locType == 66 || locType == 161 ||
                    locType == BDLocation.TypeGpsLocation ||
                    locType == BDLocation.TypeNetWorkLocation ||
                    locType == BDLocation.TypeOffLineLocation ||
                    locType == BDLocation.TypeCacheLocation) {
                    // 定位成功
                    printLocationInfo(location);
                } else {
                    // 定位失败，记录错误信息
                    String locTypeDesc = location.getLocTypeDescription();
                    String errorMsg = String.format(Locale.getDefault(),
                        "定位失败，错误代码: %d (%s)", locType, locTypeDesc);
                    LogUtils.e(LocationService.this, errorMsg);

                    // 特别处理 API Key 校验失败的错误 (错误码 167)
                    if (locType == 167 ||
                        (locTypeDesc != null && locTypeDesc.contains("TypeServerCheckKeyError"))) {
                        String detailedError = "API Key 校验失败，请检查：\n" +
                            "1. AndroidManifest.xml 中的 API Key 是否正确\n" +
                            "2. 百度开发者控制台中该 API Key 是否已启用定位服务\n" +
                            "3. 应用的包名是否与百度控制台配置的包名一致\n" +
                            "4. 应用的 SHA1 签名是否与百度控制台配置的签名一致\n" +
                            "5. API Key 是否已正确绑定到当前应用";
                        LogUtils.e(LocationService.this, detailedError);
                    }
                    // 特别处理网络定位解密失败的错误
                    else if (locType == 62 || locType == 63 || locType == 67 || locType == 68) {
                        String detailedError = "网络定位失败，可能原因：\n" +
                            "1. API Key 无效或未启用网络定位服务\n" +
                            "2. 网络连接问题\n" +
                            "3. 请求参数解密失败";
                        LogUtils.e(LocationService.this, detailedError);
                    }
                }
            } else {
                LogUtils.w(LocationService.this, "无法获取位置信息");
            }
        }
    };

    // onCreate 什么时候被调用？
    // 1. 当服务被创建时，会调用 onCreate 方法
    // 2. 当服务被重启时，会调用 onCreate 方法
    // 3. 当服务被系统重启时，会调用 onCreate 方法
    // 4. 当服务被用户手动启动时，会调用 onCreate 方法
    // 5. 当服务被其他组件启动时，会调用 onCreate 方法
    // 6. 当服务被系统服务启动时，会调用 onCreate 方法
    // 7. 当服务被其他应用启动时，会调用 onCreate 方法
    // onStartCommand 和 onCreate 的区别是什么？
    // 1. onStartCommand 是当服务被启动时，会调用 onStartCommand 方法
    // 2. onCreate 是当服务被创建时，会调用 onCreate 方法
    // 3. onStartCommand 是当服务被重启时，会调用 onStartCommand 方法
    // 4. onCreate 是当服务被系统重启时，会调用 onCreate 方法
    // 5. onStartCommand 是当服务被用户手动启动时，会调用 onStartCommand 方法
    // 6. onCreate 是当服务被其他组件启动时，会调用 onCreate 方法
    // 7. onStartCommand 是当服务被系统服务启动时，会调用 onStartCommand 方法
    @Override
    public void onCreate() {
        LogUtils.i(this, "LocationService onCreate 被触发");
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        try {
            startForegroundService();
            initializeBaiduLocation();
        } catch (Exception e) {
            Toast.makeText(this, "LocationService 初始化错误：" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            LogUtils.e(this, "LocationService 初始化错误：" + e.getLocalizedMessage(), e);
        }
    }

    // onStartCommand 什么时候被调用？
    // 1. 当服务被启动时，会调用 onStartCommand 方法
    // 2. 当服务被重启时，会调用 onStartCommand 方法
    // 3. 当服务被系统重启时，会调用 onStartCommand 方法
    // 4. 当服务被用户手动启动时，会调用 onStartCommand 方法
    // 5. 当服务被其他组件启动时，会调用 onStartCommand 方法
    // 6. 当服务被系统服务启动时，会调用 onStartCommand 方法
    // 7. 当服务被其他应用启动时，会调用 onStartCommand 方法
    // 8. 当服务被其他应用启动时，会调用 onStartCommand 方法
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.i(this, "LocationService onStartCommand 被触发");
        // 确保服务作为前台服务运行（重要：当服务被系统重启时，onCreate 可能不会再次调用）
        // 但 onStartCommand 会被调用，所以需要在这里也确保前台服务已启动
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        // 如果前台服务未启动，则启动它
        try {
            // 检查通知渠道是否存在，如果不存在则创建
            boolean needStartForeground = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = notificationManager.getNotificationChannel("location_channel");
                if (channel == null) {
                    // 通知渠道不存在，需要完整初始化
                    startForegroundService();
                } else {
                    // 通知渠道已存在，但需要确保前台服务正在运行
                    // 如果服务被重启，需要重新调用 startForeground
                    needStartForeground = true;
                }
            } else {
                // Android 8.0 以下版本，直接启动前台服务
                needStartForeground = true;
            }

            // 如果需要启动前台服务（通知渠道已存在或 Android 8.0 以下）
            if (needStartForeground) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        3,
                        createNotification("位置服务运行中"),
                        FOREGROUND_SERVICE_TYPE_LOCATION
                    );
                } else {
                    startForeground(3, createNotification("位置服务运行中"));
                }
            }

            // 如果定位客户端未初始化或未启动，则重新初始化
            if (locationClient == null || !isLocationClientStarted) {
                initializeBaiduLocation();
            }
        } catch (Exception e) {
            LogUtils.e(this, "onStartCommand 中启动前台服务失败：" + e.getLocalizedMessage(), e);
        }

        return START_STICKY; // 服务被关闭后自动重新启动
    }

    private void initializeBaiduLocation() {
        try {
            // 检查位置权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    LogUtils.e(this, "位置权限未授予，无法使用定位服务");
                    Toast.makeText(this, "位置权限未授予，无法使用定位服务", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            String sha1 = AppInfo.getSHA1Signature(this);
            if (sha1 != null) {
                LogUtils.i(this, "SHA1 签名: " + sha1);
                LogUtils.i(this, "请将以上信息配置到百度开发者控制台");
            } else {
                LogUtils.w(this, "无法获取 SHA1 签名，请使用命令行工具获取");
            }

            // 初始化定位客户端
            locationClient = new LocationClient(getApplicationContext());
            locationClient.registerLocationListener(locationListener);

            // 配置定位参数
            LocationClientOption option = new LocationClientOption();
            // 设置定位模式
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy); // 高精度模式
            // 设置返回的定位结果坐标系类型
            option.setCoorType("bd09ll"); // 返回百度经纬度坐标系
            // 设置是否需要地址信息
            option.setIsNeedAddress(true);
            // 设置是否需要返回位置的语义化信息
            option.setIsNeedLocationDescribe(true);
            // 设置是否需要返回位置的POI信息
            option.setIsNeedLocationPoiList(true);
            // 关键：设置为持续定位模式（false表示持续定位，true表示单次定位）
            option.setOnceLocation(false);
            // 设置定位间隔，单位毫秒
            // setScanSpan(0) 表示单次定位，>0 表示定时定位
            option.setScanSpan((int) LOCATION_UPDATE_INTERVAL);
            // 设置是否打开GPS
            option.setOpenGps(true);
            // 设置是否使用默认定位结果
            option.setIgnoreKillProcess(false);
            // 设置是否需要设备方向结果
            option.setNeedDeviceDirect(false);
            // 设置是否当GPS有效时按照1S/1次频率输出GPS结果
            option.setLocationNotify(true);
            // 设置是否允许模拟位置
            option.setEnableSimulateGps(false);

            LocationClient.setAgreePrivacy(true);

            // 应用定位参数
            locationClient.setLocOption(option);

            // 启动定位（异步操作，需要等待启动完成）
            locationClient.start();

            // 延迟检查启动状态，给定位客户端一些启动时间
            startCheckRetryCount = 0;

            // 500 毫秒后执行检查定位客户端启动状态的任务
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (locationClient != null) {
                        isLocationClientStarted = locationClient.isStarted();
                        if (isLocationClientStarted) {
                            Toast.makeText(LocationService.this, "百度定位服务已启动", Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "百度定位服务已启动，isStarted: " + isLocationClientStarted);
                        } else {
                            startCheckRetryCount++;
                            if (startCheckRetryCount < MAX_START_CHECK_RETRY) {
                                Log.w(TAG, "定位客户端启动中，重试检查 (" + startCheckRetryCount + "/" + MAX_START_CHECK_RETRY + ")");
                                // 如果还未启动，再等待一段时间后重试检查
                                handler.postDelayed(this, 1000);
                                Toast.makeText(LocationService.this, "定位客户端启动中，重试检查 (" + startCheckRetryCount + "/" + MAX_START_CHECK_RETRY + ")", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LocationService.this, "定位客户端启动超时，已达到最大重试次数", Toast.LENGTH_SHORT).show();
                                LogUtils.e(LocationService.this, "定位客户端启动超时，已达到最大重试次数");
                            }
                        }
                    }
                }
            }, 1000);
            LogUtils.i(this, "正在启动百度定位服务...");
        } catch (Exception e) {
            Toast.makeText(this, "初始化百度定位失败：" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            LogUtils.e(this, "初始化百度定位失败：" + e.getLocalizedMessage(), e);
        }
    }

    // 打印位置信息
    private void printLocationInfo(BDLocation location) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timeStr = sdf.format(new Date());

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        float radius = location.getRadius(); // 定位精度
        double altitude = location.getAltitude();
        float speed = location.getSpeed();
        int locType = location.getLocType(); // 定位类型
        String locTypeDescription = location.getLocTypeDescription(); // 定位类型描述
        String address = location.getAddrStr(); // 地址信息
        String locationDescribe = location.getLocationDescribe(); // 位置语义化信息

        // 构建位置信息字符串
        StringBuilder locationInfo = new StringBuilder();
        locationInfo.append(String.format(Locale.getDefault(), "时间: %s\n", timeStr));
        locationInfo.append(String.format(Locale.getDefault(), "纬度: %.6f\n", latitude));
        locationInfo.append(String.format(Locale.getDefault(), "经度: %.6f\n", longitude));
        locationInfo.append(String.format(Locale.getDefault(), "精度: %.2f 米\n", radius));
        locationInfo.append(String.format(Locale.getDefault(), "海拔: %.2f 米\n", altitude));
        locationInfo.append(String.format(Locale.getDefault(), "速度: %.2f m/s\n", speed));
        locationInfo.append(String.format(Locale.getDefault(), "定位类型: %d (%s)\n", locType, locTypeDescription));
        if (address != null && !address.isEmpty()) {
            locationInfo.append(String.format(Locale.getDefault(), "地址: %s\n", address));
        }
        if (locationDescribe != null && !locationDescribe.isEmpty()) {
            locationInfo.append(String.format(Locale.getDefault(), "位置描述: %s\n", locationDescribe));
        }

        String locationInfoStr = locationInfo.toString();

        LogUtils.i(this, "位置信息: " + locationInfoStr);

        // 更新通知显示位置信息
        updateNotification(locationInfoStr);
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
        LogUtils.i(this, "LocationService onDestroy 被触发");

        super.onDestroy();

        // 移除所有待执行的任务
        handler.removeCallbacksAndMessages(null);

        // 停止百度定位服务
        if (locationClient != null) {
            try {
                locationClient.stop();
                locationClient.unRegisterLocationListener(locationListener);
                locationClient = null;
                isLocationClientStarted = false;
            } catch (Exception e) {
                LogUtils.e(this, "停止百度定位服务时出错", e);
            }
        }
    }

    @Override
    public android.os.IBinder onBind(Intent intent) {
        return null;
    }
}
