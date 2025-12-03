package com.example.test_filesync.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.example.test_filesync.R;
import com.example.test_filesync.util.FloatingWindowHelper;
import com.example.test_filesync.util.LogUtils;

/**
 * 悬浮窗服务
 * 用于在后台管理悬浮窗的显示和隐藏
 */
public class FloatingWindowService extends Service {
    
    private static final String TAG = "FloatingWindowService";
    private static final String CHANNEL_ID = "FloatingWindowServiceChannel";
    private static final int NOTIFICATION_ID = 1002;
    
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.i(this, TAG, "悬浮窗服务已创建");
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.i(this, TAG, "悬浮窗服务已启动");
        
        // 显示前台服务通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ (API 34+) 需要指定前台服务类型
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            );
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }
        
        // 显示悬浮窗
        if (intent != null && intent.getBooleanExtra("show", true)) {
            boolean success = FloatingWindowHelper.showFloatingWindow(this);
            if (!success) {
                LogUtils.w(this, TAG, "显示悬浮窗失败，可能需要权限");
            }
        } else {
            // 隐藏悬浮窗
            // FloatingWindowHelper.hideFloatingWindow(this);
        }
        
        return START_STICKY; // 服务被杀死后自动重启
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.i(this, TAG, "悬浮窗服务已销毁");
        // 隐藏悬浮窗
        FloatingWindowHelper.hideFloatingWindow(this);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // 不需要绑定服务
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "悬浮窗服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于管理悬浮窗显示的服务");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("悬浮窗服务")
                .setContentText("悬浮窗正在运行")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return builder.build();
        } else {
            return builder.build();
        }
    }
    
    /**
     * 启动悬浮窗服务
     * @param context 上下文
     * @param show 是否显示悬浮窗
     */
    public static void startService(android.content.Context context, boolean show) {
        Intent intent = new Intent(context, FloatingWindowService.class);
        intent.putExtra("show", show);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
    
    /**
     * 停止悬浮窗服务
     * @param context 上下文
     */
    public static void stopService(android.content.Context context) {
        Intent intent = new Intent(context, FloatingWindowService.class);
        context.stopService(intent);
    }
}

