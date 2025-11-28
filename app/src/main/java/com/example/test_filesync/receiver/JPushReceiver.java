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

import java.util.Set;

/**
 * 极光推送消息接收器
 * 用于接收和处理极光推送的各种消息类型
 */
public class JPushReceiver extends JPushMessageReceiver {
    private static final String TAG = "JPushReceiver";

    /**
     * 接收自定义消息
     * 自定义消息不会在通知栏显示，需要应用自己处理
     */
    @Override
    public void onMessage(Context context, CustomMessage customMessage) {
        // super.onMessage(context, customMessage);
        LogUtils.i(context, TAG, "收到自定义消息 - Title: " + customMessage.title + ", Message: " + customMessage.message);

        // 处理附加字段
        if (customMessage.extra != null && !customMessage.extra.isEmpty()) {
            LogUtils.d(context, TAG, "自定义消息附加字段: " + customMessage.extra);
            try {
                JSONObject jsonObject = new JSONObject(customMessage.extra);
                LogUtils.d(context, TAG, "解析附加字段: " + jsonObject.toString());
            } catch (JSONException e) {
                LogUtils.e(context, TAG, "解析自定义消息附加字段失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 通知消息到达时回调
     * 当通知消息到达设备时，会回调此方法
     */
    @Override
    public void onNotifyMessageArrived(Context context, NotificationMessage notificationMessage) {
        super.onNotifyMessageArrived(context, notificationMessage);
        LogUtils.i(context, TAG, "通知消息到达 - Title: " + notificationMessage.notificationTitle
                + ", Content: " + notificationMessage.notificationContent);

        // 处理通知消息的附加字段
        if (notificationMessage.notificationExtras != null && !notificationMessage.notificationExtras.isEmpty()) {
            LogUtils.d(context, TAG, "通知消息附加字段: " + notificationMessage.notificationExtras);
        }
    }

    /**
     * 通知消息被点击时回调
     * 当用户点击通知栏中的通知时，会回调此方法
     */
    @Override
    public void onNotifyMessageOpened(Context context, NotificationMessage notificationMessage) {
        super.onNotifyMessageOpened(context, notificationMessage);
        LogUtils.i(context, TAG, "通知消息被点击 - Title: " + notificationMessage.notificationTitle
                + ", Content: " + notificationMessage.notificationContent);

        // 处理通知点击事件，可以跳转到指定页面
        // TODO: 根据业务需求处理通知点击逻辑
    }

    /**
     * 通知消息被清除时回调
     */
    @Override
    public void onNotifyMessageDismiss(Context context, NotificationMessage notificationMessage) {
        super.onNotifyMessageDismiss(context, notificationMessage);
        LogUtils.d(context, TAG, "通知消息被清除 - Title: " + notificationMessage.notificationTitle);
    }

    /**
     * 注册成功回调
     * 当应用成功注册到极光推送服务器时，会回调此方法
     */
    @Override
    public void onRegister(Context context, String registrationId) {
        super.onRegister(context, registrationId);
        LogUtils.i(context, TAG, "极光推送注册成功 - RegistrationId: " + registrationId);
    }

    /**
     * 连接状态变化回调
     * 当与极光推送服务器的连接状态发生变化时，会回调此方法
     */
    @Override
    public void onConnected(Context context, boolean isConnected) {
        super.onConnected(context, isConnected);
        LogUtils.i(context, TAG, "极光推送连接状态变化 - isConnected: " + isConnected);
    }
}
