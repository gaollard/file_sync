package com.example.test_filesync.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.test_filesync.MainActivity;
import com.example.test_filesync.service.LocationService;
import com.example.test_filesync.util.LogUtils;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        LogUtils.i(context, TAG, "BootReceiver 被触发，action: " + action);

//        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
//            "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
//            LogUtils.i(context, TAG, "设备启动完成，准备启动 LocationService");
//            // 启动 LocationService
//            Intent serviceIntent = new Intent(context, LocationService.class);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(serviceIntent);
//            } else {
//                context.startService(serviceIntent);
//            }
//            LogUtils.i(context, TAG, "LocationService 已启动");
//        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
//                   Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
//            // 应用更新后也自动启动服务
//            LogUtils.i(context, TAG, "应用已更新，准备启动 LocationService");
//            Intent serviceIntent = new Intent(context, LocationService.class);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(serviceIntent);
//            } else {
//                context.startService(serviceIntent);
//            }
//            LogUtils.i(context, TAG, "LocationService 已启动");
//        }
    }
}
