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
        // 如果父类有onMessage方法，取消下面的注释
        // super.onMessage(context, bundle);

        if (context == null || bundle == null) {
            LogUtils.w(context, TAG, "收到JPush消息，但Context或Bundle为空");
            return;
        }

        try {
            // 获取消息内容
            String message = bundle.getString(JPushInterface.EXTRA_MESSAGE);
            String extras = bundle.getString(JPushInterface.EXTRA_EXTRA);
            String messageId = bundle.getString(JPushInterface.EXTRA_MSG_ID);

            LogUtils.i(context, TAG, "收到JPush自定义消息 - MessageId: " + messageId + ", Message: " + message);

            // 处理附加字段
            if (extras != null && !extras.isEmpty()) {
                LogUtils.d(context, TAG, "自定义消息附加字段: " + extras);
                try {
                    JSONObject jsonObject = new JSONObject(extras);
                    parseMessageExtras(context, jsonObject);
                } catch (JSONException e) {
                    LogUtils.e(context, TAG, "解析自定义消息附加字段失败: " + e.getMessage(), e);
                }
            }

            // TODO: 在这里添加自定义消息的业务处理逻辑
            // 例如：根据消息类型执行不同的操作、更新本地数据等

        } catch (Exception e) {
            LogUtils.e(context, TAG, "处理JPush消息时发生异常: " + e.getMessage(), e);
        }
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
