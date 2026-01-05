package com.example.test_filesync.receiver;

import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.test_filesync.service.LocationService;
import com.example.test_filesync.service.PingJobService;
import com.example.test_filesync.util.LogUtils;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        LogUtils.i(context, TAG, "BootReceiver 被触发，action: " + action);

        // 判断 LocationService 是否开启
        // 如果未开启，则开启 LocationService
        if (!LocationService.isRunning) {
            LogUtils.i(context, TAG, "LocationService 未开启，开启 LocationService");
            LocationService.isRunning = true;
            Intent locationServiceIntent = new Intent(context, LocationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(locationServiceIntent);
            } else {
                context.startService(locationServiceIntent);
            }
        } else {
            LogUtils.i(context, TAG, "LocationService 已开启");
        }

        // 添加一些保活策略
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            // 设备启动完成，重新调度 PingJobService
            LogUtils.i(context, TAG, "设备启动完成，重新调度 PingJobService");
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
                   Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            // 应用更新后也自动启动服务
            LogUtils.i(context, TAG, "应用已更新，重新调度 PingJobService");
        }

        // 熄屏唤醒
        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            LogUtils.i(context, TAG, "熄屏唤醒，");
        }

        // 亮屏唤醒
        if (Intent.ACTION_SCREEN_ON.equals(action)) {
            LogUtils.i(context, TAG, "亮屏唤醒，");
        }

        // 屏幕解锁
        if (Intent.ACTION_USER_PRESENT.equals(action)) {
            LogUtils.i(context, TAG, "屏幕解锁，");
        }
    }
}
