package com.example.test_filesync.service;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.example.test_filesync.util.LogUtils;

public class PingJobService extends JobService {
    private static final String TAG = "PingJobService";
    private boolean jobCancelled = false;

    @Override
    public boolean onStartJob(JobParameters params) {
        jobCancelled = false;
        LogUtils.d(this, TAG, "Job started");
        Context context = this;
        // 在后台线程执行任务
        new Thread(() -> {
            int count = 0;
            while (!jobCancelled) {
                LogUtils.d(context, TAG, "Number: " + count);
                count++;
                try {
                    Thread.sleep(5000); // 每隔5秒打印一次
                } catch (InterruptedException e) {
                    Log.e(TAG, "Job interrupted", e);
                    break;
                }
            }
            LogUtils.d(context, TAG, "Job stopped");
            // 如果任务被取消，不调用 jobFinished，让系统知道任务未完成
            // 这样 onStopJob 会被调用，我们可以在那里重新调度
        }).start();

        return true; // 返回true表示任务在后台线程执行
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        LogUtils.d(this, TAG, "Job cancelled before completion, rescheduling...");
        jobCancelled = true;
        // 任务被停止时，立即重新调度以确保任务继续运行
        rescheduleJob(this);
        return false; // 返回false，因为我们手动重新调度了
    }

    /**
     * 重新调度任务
     */
    private void rescheduleJob(Context context) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
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
                LogUtils.d(context, TAG, "任务重新调度成功");
            } else {
                LogUtils.e(context, TAG, "任务重新调度失败，返回码: " + result);
            }
        } catch (Exception e) {
            LogUtils.e(context, TAG, "重新调度任务失败: " + e.getMessage(), e);
        }
    }
}
