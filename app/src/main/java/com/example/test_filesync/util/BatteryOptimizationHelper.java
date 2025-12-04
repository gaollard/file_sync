package com.example.test_filesync.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

/**
 * 电池优化工具类
 * 用于检查和请求关闭电池优化，确保应用在后台正常运行
 */
public class BatteryOptimizationHelper {
    
    private static final String TAG = "BatteryOptimizationHelper";
    
    /**
     * 检查是否已关闭电池优化
     * @param context 上下文
     * @return true 如果已关闭电池优化（应用在电池优化白名单中）
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        }
        return true; // Android 6.0以下不需要电池优化设置
    }
    
    /**
     * 请求关闭电池优化
     * @param activity Activity上下文
     * @param requestCode 请求码
     */
    public static void requestIgnoreBatteryOptimizations(Activity activity, int requestCode) {
        if (isIgnoringBatteryOptimizations(activity)) {
            LogUtils.i(activity, TAG, "电池优化已关闭");
            Toast.makeText(activity, "电池优化已关闭", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, requestCode);
                LogUtils.i(activity, TAG, "请求关闭电池优化");
            } catch (Exception e) {
                LogUtils.e(activity, TAG, "请求关闭电池优化失败: " + e.getMessage(), e);
                // 如果直接请求失败，跳转到电池优化设置页面
                openBatteryOptimizationSettings(activity);
            }
        }
    }
    
    /**
     * 打开电池优化设置页面
     * @param context 上下文
     */
    public static void openBatteryOptimizationSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                if (context instanceof Activity) {
                    context.startActivity(intent);
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
                LogUtils.i(context, TAG, "打开电池优化设置页面");
            } catch (Exception e) {
                LogUtils.e(context, TAG, "打开电池优化设置页面失败: " + e.getMessage(), e);
                // 如果失败，打开应用设置页面
                openAppSettings(context);
            }
        }
    }
    
    /**
     * 打开应用设置页面
     * @param context 上下文
     */
    private static void openAppSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            LogUtils.i(context, TAG, "打开应用设置页面");
        } catch (Exception e) {
            LogUtils.e(context, TAG, "打开应用设置页面失败: " + e.getMessage(), e);
            Toast.makeText(context, "无法打开设置页面，请手动在设置中关闭电池优化", Toast.LENGTH_LONG).show();
        }
    }
}

