package com.example.test_filesync.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.util.Log;

import com.example.test_filesync.util.LogUtils;

public class PingJobService extends JobService {
    private static final String TAG = "PingJobService";
    private boolean jobCancelled = false;

    @Override
    public boolean onStartJob(JobParameters params) {
        LogUtils.d(this, TAG, "Job started");
        Context context = this;
        // 在后台线程执行任务
        new Thread(() -> {
            for (int i = 0; i < 10 && !jobCancelled; i++) {
                LogUtils.d(context, TAG, "Running job iteration: " + i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Job interrupted", e);
                }
            }

            LogUtils.d(context, TAG, "Job finished");
            jobFinished(params, false); // 任务完成，不需要重试
        }).start();

        return true; // 返回true表示任务在后台线程执行
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job cancelled before completion");
        jobCancelled = true;
        return true; // 返回true表示需要重新调度任务
    }
}
