package com.example.test_filesync.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.Set;

import cn.jpush.android.api.JPushInterface;

/**
 * 极光推送工具类
 * 提供便捷的推送功能封装
 */
public class JPushUtil {
    private static final String TAG = "JPushUtil";

    /**
     * 设置别名
     * @param context 上下文
     * @param alias 别名
     */
    public static void setAlias(Context context, String alias) {
        if (context == null || alias == null || alias.isEmpty()) {
            LogUtils.w(context, TAG, "设置别名失败：参数无效");
            return;
        }
        try {
            JPushInterface.setAlias(context, 0, alias);
            LogUtils.i(context, TAG, "设置别名: " + alias);
        } catch (Exception e) {
            LogUtils.e(context, TAG, "设置别名失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除别名
     * @param context 上下文
     */
    public static void deleteAlias(Context context) {
        if (context == null) {
            return;
        }
        try {
            JPushInterface.deleteAlias(context, 0);
            LogUtils.i(context, TAG, "删除别名");
        } catch (Exception e) {
            LogUtils.e(context, TAG, "删除别名失败: " + e.getMessage(), e);
        }
    }

    /**
     * 设置标签
     * @param context 上下文
     * @param tags 标签集合
     */
    public static void setTags(Context context, Set<String> tags) {
        if (context == null || tags == null || tags.isEmpty()) {
            LogUtils.w(context, TAG, "设置标签失败：参数无效");
            return;
        }
        try {
            JPushInterface.setTags(context, 0, tags);
            LogUtils.i(context, TAG, "设置标签: " + tags.toString());
        } catch (Exception e) {
            LogUtils.e(context, TAG, "设置标签失败: " + e.getMessage(), e);
        }
    }

    /**
     * 添加标签
     * @param context 上下文
     * @param tags 标签集合
     */
    public static void addTags(Context context, Set<String> tags) {
        if (context == null || tags == null || tags.isEmpty()) {
            LogUtils.w(context, TAG, "添加标签失败：参数无效");
            return;
        }
        try {
            JPushInterface.addTags(context, 0, tags);
            LogUtils.i(context, TAG, "添加标签: " + tags.toString());
        } catch (Exception e) {
            LogUtils.e(context, TAG, "添加标签失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除标签
     * @param context 上下文
     * @param tags 标签集合
     */
    public static void deleteTags(Context context, Set<String> tags) {
        if (context == null || tags == null || tags.isEmpty()) {
            LogUtils.w(context, TAG, "删除标签失败：参数无效");
            return;
        }
        try {
            JPushInterface.deleteTags(context, 0, tags);
            LogUtils.i(context, TAG, "删除标签: " + tags.toString());
        } catch (Exception e) {
            LogUtils.e(context, TAG, "删除标签失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清空所有标签
     * @param context 上下文
     */
    public static void clearTags(Context context) {
        if (context == null) {
            return;
        }
        try {
            JPushInterface.cleanTags(context, 0);
            LogUtils.i(context, TAG, "清空所有标签");
        } catch (Exception e) {
            LogUtils.e(context, TAG, "清空标签失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取注册ID
     * @param context 上下文
     * @return 注册ID，如果未注册则返回空字符串
     */
    public static String getRegistrationId(Context context) {
        if (context == null) {
            return "";
        }
        try {
            String registrationId = JPushInterface.getRegistrationID(context);
            LogUtils.i(context, TAG, "获取RegistrationId: " + registrationId);
            return registrationId != null ? registrationId : "";
        } catch (Exception e) {
            LogUtils.e(context, TAG, "获取RegistrationId失败: " + e.getMessage(), e);
            return "";
        }
    }

    /**
     * 停止推送
     * @param context 上下文
     */
    public static void stopPush(Context context) {
        if (context == null) {
            return;
        }
        try {
            JPushInterface.stopPush(context);
            LogUtils.i(context, TAG, "停止推送");
        } catch (Exception e) {
            LogUtils.e(context, TAG, "停止推送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 恢复推送
     * @param context 上下文
     */
    public static void resumePush(Context context) {
        if (context == null) {
            return;
        }
        try {
            JPushInterface.resumePush(context);
            LogUtils.i(context, TAG, "恢复推送");
        } catch (Exception e) {
            LogUtils.e(context, TAG, "恢复推送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查推送是否被停止
     * @param context 上下文
     * @return true表示推送已停止，false表示推送正常
     */
    public static boolean isPushStopped(Context context) {
        if (context == null) {
            return false;
        }
        try {
            return JPushInterface.isPushStopped(context);
        } catch (Exception e) {
            LogUtils.e(context, TAG, "检查推送状态失败: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 设置推送时间
     * @param context 上下文
     * @param days 星期数组，0表示周日，1表示周一，以此类推
     * @param startHour 开始小时（0-23）
     * @param endHour 结束小时（0-23）
     */
    public static void setPushTime(Context context, Set<Integer> days, int startHour, int endHour) {
        if (context == null || days == null || days.isEmpty()) {
            LogUtils.w(context, TAG, "设置推送时间失败：参数无效");
            return;
        }
        try {
            JPushInterface.setPushTime(context, days, startHour, endHour);
            LogUtils.i(context, TAG, "设置推送时间: " + days.toString() + ", " + startHour + "-" + endHour);
        } catch (Exception e) {
            LogUtils.e(context, TAG, "设置推送时间失败: " + e.getMessage(), e);
        }
    }

    /**
     * 设置静默时间
     * @param context 上下文
     * @param startHour 开始小时（0-23）
     * @param startMinute 开始分钟（0-59）
     * @param endHour 结束小时（0-23）
     * @param endMinute 结束分钟（0-59）
     */
    public static void setSilenceTime(Context context, int startHour, int startMinute, int endHour, int endMinute) {
        if (context == null) {
            return;
        }
        try {
            JPushInterface.setSilenceTime(context, startHour, startMinute, endHour, endMinute);
            LogUtils.i(context, TAG, "设置静默时间: " + startHour + ":" + startMinute + " - " + endHour + ":" + endMinute);
        } catch (Exception e) {
            LogUtils.e(context, TAG, "设置静默时间失败: " + e.getMessage(), e);
        }
    }
}

