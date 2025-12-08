package com.example.test_filesync.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.test_filesync.util.LogUtils;

public class MyHonorMsgReceiver extends BroadcastReceiver {
    private static final String TAG = "MyHonorMsgReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtils.i(context, TAG, "onReceive: " + action);
    }
}
