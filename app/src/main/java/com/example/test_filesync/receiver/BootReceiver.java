package com.example.test_filesync.receiver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.test_filesync.service.LocationService;
import com.example.test_filesync.util.LogUtils;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        LogUtils.i(context, TAG, "BootReceiver 被触发，action: " + action);

        // 判断 LocationService 是否开启
        // 如果未开启，则开启 LocationService
        // if (!LocationService.isRunning) {
        // LogUtils.i(context, TAG, "LocationService 未开启，开启 LocationService");
        // LocationService.isRunning = true;
        // Intent locationServiceIntent = new Intent(context, LocationService.class);
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // context.startForegroundService(locationServiceIntent);
        // } else {
        // context.startService(locationServiceIntent);
        // }
        // } else {
        // LogUtils.i(context, TAG, "LocationService 已开启");
        // }

        // 添加一些保活策略
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            LogUtils.i(context, TAG, "设备启动完成");
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
                Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            LogUtils.i(context, TAG, "应用已更新");
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
