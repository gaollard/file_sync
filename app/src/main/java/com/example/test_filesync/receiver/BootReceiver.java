package com.example.test_filesync.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.test_filesync.MainActivity;
import com.example.test_filesync.util.LogUtils;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
          LogUtils.i(context, TAG, "设备启动完成");
        }
    }
}
