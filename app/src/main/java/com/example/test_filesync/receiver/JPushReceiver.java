package com.example.test_filesync.receiver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.test_filesync.util.LogUtils;

import cn.jpush.android.api.CustomMessage;
import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.api.NotificationMessage;
import cn.jpush.android.service.JPushMessageReceiver;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 极光推送消息接收器
 * 用于接收和处理极光推送的各种消息类型
 */
public class JPushReceiver extends JPushMessageReceiver {
    private static final String TAG = "JPushReceiver";

  @Override
  public void onMessage(Context context, CustomMessage customMessage) {
    super.onMessage(context, customMessage);
    LogUtils.i(context, "new_msg" + customMessage.title + customMessage.message);
  }


}
