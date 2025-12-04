package com.example.test_filesync.receiver;

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

        // 添加一些保活策略
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            // 设备启动完成，重新调度 PingJobService
            LogUtils.i(context, TAG, "设备启动完成，重新调度 PingJobService");
            schedulePingJob(context);
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
                   Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            // 应用更新后也自动启动服务
            LogUtils.i(context, TAG, "应用已更新，重新调度 PingJobService");
            schedulePingJob(context);
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

    /**
     * 调度 PingJobService
     */
    private void schedulePingJob(Context context) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                LogUtils.w(context, TAG, "JobScheduler 需要 Android 5.0+，当前版本不支持");
                return;
            }

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler == null) {
                LogUtils.e(context, TAG, "无法获取 JobScheduler 服务");
                return;
            }

            ComponentName componentName = new ComponentName(context, PingJobService.class);
            JobInfo.Builder jobBuilder = new JobInfo.Builder(1, componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false)
                    .setPersisted(true); // 持久化，应用退出后仍可执行

            // 设置立即执行
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                jobBuilder.setMinimumLatency(0);
                jobBuilder.setOverrideDeadline(1000);
            }

            JobInfo jobInfo = jobBuilder.build();
            int result = jobScheduler.schedule(jobInfo);

            if (result == JobScheduler.RESULT_SUCCESS) {
                LogUtils.i(context, TAG, "PingJobService 调度成功");
            } else {
                LogUtils.e(context, TAG, "PingJobService 调度失败，返回码: " + result);
            }
        } catch (Exception e) {
            LogUtils.e(context, TAG, "调度 PingJobService 失败: " + e.getMessage(), e);
        }
    }
}
