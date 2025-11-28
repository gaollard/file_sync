package com.example.test_filesync.service;

import android.content.Context;
import android.os.Bundle;

import cn.jpush.android.service.JCommonService;
import cn.jpush.android.api.JPushInterface;

import com.example.test_filesync.util.LogUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 极光推送通用服务
 * 继承自JCommonService，用于处理极光推送的自定义消息
 */
public class JiService extends JCommonService {
    private static final String TAG = "JiService";

    /**
     * 处理极光推送的自定义消息
     * 注意：此方法可能不是JCommonService的标准方法，根据实际SDK版本调整
     */
    public void onMessage(Context context, Bundle bundle) {
        LogUtils.i(context, TAG, "收到JPush消息");
    }

    /**
     * 解析消息的附加字段
     * @param context 上下文
     * @param extras JSON格式的附加字段
     */
    private void parseMessageExtras(Context context, JSONObject extras) {
        try {
            // TODO: 根据业务需求解析自定义字段
            // 例如：
            // if (extras.has("type")) {
            //     String type = extras.optString("type");
            //     // 根据type处理不同的业务逻辑
            // }
            // if (extras.has("action")) {
            //     String action = extras.optString("action");
            //     // 根据action执行相应的操作
            // }
            LogUtils.d(context, TAG, "解析附加字段: " + extras.toString());
        } catch (Exception e) {
            LogUtils.e(context, TAG, "解析附加字段时发生异常: " + e.getMessage(), e);
        }
    }
}
