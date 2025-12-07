package com.example.test_filesync.receiver;

import com.example.test_filesync.util.LogUtils;
import com.hihonor.push.sdk.HonorMessageService;
import com.hihonor.push.sdk.HonorPushDataMsg;

public class MyHonorMsgService extends HonorMessageService {
    private static final String TAG = "MyHonorMsgService";

    //Token发生变化时，会以onNewToken方法返回
    @Override
    public void onNewToken(String pushToken) {
        // TODO: 处理新token。
      LogUtils.i(getApplication(), TAG, "onNewToken: " + pushToken);
    }

    @Override
    public void onMessageReceived(HonorPushDataMsg msg) {
        // TODO: 处理收到的透传消息
        LogUtils.i(getApplication(), TAG, "onMessageReceived: " + msg.toString());
    }
}
