package com.example.test_filesync.receiver;

import com.example.test_filesync.util.LogUtils;
import com.hihonor.push.sdk.HonorMessageService;
import com.hihonor.push.sdk.HonorPushDataMsg;

public class MyHonorMsgService extends HonorMessageService {
    private static final String TAG = "MyHonorMsgService";

    //Token发生变化时，会以onNewToken方法返回
    @Override
    public void onNewToken(String pushToken) {
      LogUtils.i(getApplication(), TAG, "onNewToken: " + pushToken);
    }

    @Override
    public void onMessageReceived(HonorPushDataMsg msg) {
        // 打印荣耀推送消息 这里为什么没执行？
        LogUtils.i(getApplication(), TAG, "onMessageReceived: " + msg.toString());
    }
}
