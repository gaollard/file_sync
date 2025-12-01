package com.example.test_filesync.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.test_filesync.util.LogUtils;

import org.json.JSONObject;

import cn.jpush.android.api.JPushInterface;

public class CustomPushReceiver extends BroadcastReceiver {
  private static final String TAG = "MyReceiver";

  private NotificationManager nm;

  @Override
  public void onReceive(Context context, Intent intent) {
    if (null == nm) {
      nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    Bundle bundle = intent.getExtras();
    LogUtils.d(context, "onReceive - " + intent.getAction() + ", extras: ");

    if (JPushInterface.ACTION_REGISTRATION_ID.equals(intent.getAction())) {
      LogUtils.d(context, "JPush 用户注册成功");

    } else if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
      LogUtils.d(context, "接受到推送下来的自定义消息");

    } else if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(intent.getAction())) {
      LogUtils.d(context, "接受到推送下来的通知");

      receivingNotification(context,bundle);

    } else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(intent.getAction())) {
      LogUtils.d(context, "用户点击打开了通知");
      openNotification(context,bundle);
    } else {
      LogUtils.d(context, "Unhandled intent - " + intent.getAction());
    }
  }

  private void receivingNotification(Context context, Bundle bundle){
    String title = bundle.getString(JPushInterface.EXTRA_NOTIFICATION_TITLE);
    LogUtils.d(context, " title : " + title);
    String message = bundle.getString(JPushInterface.EXTRA_ALERT);
    LogUtils.d(context, "message : " + message);
    String extras = bundle.getString(JPushInterface.EXTRA_EXTRA);
    LogUtils.d(context, "extras : " + extras);
  }

  private void openNotification(Context context, Bundle bundle){
    String extras = bundle.getString(JPushInterface.EXTRA_EXTRA);
    String myValue = "";
    try {
      JSONObject extrasJson = new JSONObject(extras);
      myValue = extrasJson.optString("myKey");
    } catch (Exception e) {
      LogUtils.w(context, "Unexpected: extras is not a valid json");
      return;
    }
//    if (TYPE_THIS.equals(myValue)) {
//      Intent mIntent = new Intent(context, ThisActivity.class);
//      mIntent.putExtras(bundle);
//      mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//      context.startActivity(mIntent);
//    } else if (TYPE_ANOTHER.equals(myValue)){
//      Intent mIntent = new Intent(context, AnotherActivity.class);
//      mIntent.putExtras(bundle);
//      mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//      context.startActivity(mIntent);
//    }
  }
}
