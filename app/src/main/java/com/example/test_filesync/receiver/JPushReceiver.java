package com.example.test_filesync.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.test_filesync.util.LogUtils;
import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.api.NotificationMessage;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 极光推送消息接收器
 * 用于接收和处理极光推送的各种消息类型
 */
public class JPushReceiver extends BroadcastReceiver {
    private static final String TAG = "JPushReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        Bundle bundle = intent.getExtras();

        if (bundle == null) {
            LogUtils.w(context, TAG, "收到JPush消息，但Bundle为空");
            return;
        }

        LogUtils.i(context, TAG, "收到JPush消息，action: " + action);

        try {
            // 处理不同类型的消息
            if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(action)) {
                // 自定义消息
                handleCustomMessage(context, bundle);
            } else if (JPushInterface.ACTION_NOTIFICATION_RECEIVED.equals(action)) {
                // 通知消息收到
                handleNotificationReceived(context, bundle);
            } else if (JPushInterface.ACTION_NOTIFICATION_OPENED.equals(action)) {
                // 通知消息被点击
                handleNotificationOpened(context, bundle);
            } else if (JPushInterface.ACTION_REGISTRATION_ID.equals(action)) {
                // 注册ID更新
                handleRegistrationId(context, bundle);
            } else if (JPushInterface.ACTION_CONNECTION_CHANGE.equals(action)) {
                // 连接状态变化
                handleConnectionChange(context, bundle);
            } else {
                LogUtils.d(context, TAG, "未处理的消息类型: " + action);
            }
        } catch (Exception e) {
            LogUtils.e(context, TAG, "处理JPush消息时发生异常: " + e.getMessage(), e);
        }
    }

    /**
     * 处理自定义消息
     */
    private void handleCustomMessage(Context context, Bundle bundle) {
        String message = bundle.getString(JPushInterface.EXTRA_MESSAGE);
        String extras = bundle.getString(JPushInterface.EXTRA_EXTRA);
        String messageId = bundle.getString(JPushInterface.EXTRA_MSG_ID);

        LogUtils.i(context, TAG, "收到自定义消息 - MessageId: " + messageId + ", Message: " + message);
        
        if (extras != null && !extras.isEmpty()) {
            LogUtils.d(context, TAG, "自定义消息附加字段: " + extras);
            try {
                JSONObject jsonObject = new JSONObject(extras);
                // 可以在这里解析自定义字段并处理业务逻辑
                parseCustomExtras(context, jsonObject);
            } catch (JSONException e) {
                LogUtils.e(context, TAG, "解析自定义消息附加字段失败: " + e.getMessage(), e);
            }
        }

        // TODO: 在这里处理自定义消息的业务逻辑
        // 例如：更新UI、保存数据、触发特定操作等
    }

    /**
     * 处理通知消息收到
     */
    private void handleNotificationReceived(Context context, Bundle bundle) {
        String title = bundle.getString(JPushInterface.EXTRA_NOTIFICATION_TITLE);
        String content = bundle.getString(JPushInterface.EXTRA_ALERT);
        String extras = bundle.getString(JPushInterface.EXTRA_EXTRA);
        String messageId = bundle.getString(JPushInterface.EXTRA_MSG_ID);
        int notificationId = bundle.getInt(JPushInterface.EXTRA_NOTIFICATION_ID, 0);

        LogUtils.i(context, TAG, "收到通知消息 - MessageId: " + messageId 
                + ", NotificationId: " + notificationId 
                + ", Title: " + title 
                + ", Content: " + content);

        if (extras != null && !extras.isEmpty()) {
            LogUtils.d(context, TAG, "通知消息附加字段: " + extras);
            try {
                JSONObject jsonObject = new JSONObject(extras);
                parseNotificationExtras(context, jsonObject);
            } catch (JSONException e) {
                LogUtils.e(context, TAG, "解析通知消息附加字段失败: " + e.getMessage(), e);
            }
        }

        // TODO: 在这里处理通知消息收到的业务逻辑
        // 例如：更新应用角标、统计消息到达率等
    }

    /**
     * 处理通知消息被点击
     */
    private void handleNotificationOpened(Context context, Bundle bundle) {
        String title = bundle.getString(JPushInterface.EXTRA_NOTIFICATION_TITLE);
        String content = bundle.getString(JPushInterface.EXTRA_ALERT);
        String extras = bundle.getString(JPushInterface.EXTRA_EXTRA);
        String messageId = bundle.getString(JPushInterface.EXTRA_MSG_ID);

        LogUtils.i(context, TAG, "通知消息被点击 - MessageId: " + messageId 
                + ", Title: " + title 
                + ", Content: " + content);

        if (extras != null && !extras.isEmpty()) {
            LogUtils.d(context, TAG, "通知消息附加字段: " + extras);
            try {
                JSONObject jsonObject = new JSONObject(extras);
                parseNotificationExtras(context, jsonObject);
                
                // 根据附加字段跳转到指定页面
                handleNotificationClick(context, jsonObject);
            } catch (JSONException e) {
                LogUtils.e(context, TAG, "解析通知消息附加字段失败: " + e.getMessage(), e);
            }
        } else {
            // 没有附加字段时，默认跳转到主页面
            // TODO: 跳转到应用主页面
        }

        // TODO: 在这里处理通知点击的业务逻辑
        // 例如：跳转到指定页面、打开特定功能等
    }

    /**
     * 处理注册ID更新
     */
    private void handleRegistrationId(Context context, Bundle bundle) {
        String registrationId = bundle.getString(JPushInterface.EXTRA_REGISTRATION_ID);
        LogUtils.i(context, TAG, "RegistrationId更新: " + registrationId);
        
        // TODO: 在这里处理RegistrationId更新
        // 例如：将RegistrationId上传到服务器
    }

    /**
     * 处理连接状态变化
     */
    private void handleConnectionChange(Context context, Bundle bundle) {
        boolean connected = bundle.getBoolean(JPushInterface.EXTRA_CONNECTION_CHANGE, false);
        LogUtils.i(context, TAG, "连接状态变化: " + (connected ? "已连接" : "已断开"));
        
        // TODO: 在这里处理连接状态变化
        // 例如：更新UI状态、重试连接等
    }

    /**
     * 解析自定义消息的附加字段
     */
    private void parseCustomExtras(Context context, JSONObject extras) {
        // TODO: 根据业务需求解析自定义字段
        // 例如：
        // if (extras.has("type")) {
        //     String type = extras.optString("type");
        //     // 根据type处理不同的业务逻辑
        // }
    }

    /**
     * 解析通知消息的附加字段
     */
    private void parseNotificationExtras(Context context, JSONObject extras) {
        // TODO: 根据业务需求解析通知附加字段
        // 例如：
        // if (extras.has("page")) {
        //     String page = extras.optString("page");
        //     // 根据page跳转到不同页面
        // }
    }

    /**
     * 处理通知点击事件
     */
    private void handleNotificationClick(Context context, JSONObject extras) {
        // TODO: 根据附加字段跳转到指定页面
        // 例如：
        // if (extras.has("page")) {
        //     String page = extras.optString("page");
        //     Intent intent = new Intent(context, TargetActivity.class);
        //     intent.putExtra("page", page);
        //     intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //     context.startActivity(intent);
        // }
    }
}