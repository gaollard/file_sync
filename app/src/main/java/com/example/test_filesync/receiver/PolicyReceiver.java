package com.example.test_filesync.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.example.test_filesync.util.LogUtils;

/**
 * 设备管理员接收器
 * 用于接收设备管理员相关的系统广播
 * 
 * BIND_DEVICE_ADMIN 权限说明：
 * - 这是一个签名权限（signature permission），只有系统应用才能使用
 * - 用于绑定设备管理员服务，允许应用成为设备管理员
 * - 必须在 AndroidManifest.xml 中声明在 receiver 的 permission 属性中
 */
public class PolicyReceiver extends DeviceAdminReceiver {
    
    private static final String TAG = "PolicyReceiver";

    /**
     * 当设备管理员被激活时调用
     */
    @Override
    public void onEnabled(Context context, Intent intent) {
      super.onEnabled(context, intent);
      LogUtils.i(context, TAG, "设备管理员已激活");
      Toast.makeText(context, "设备管理员已激活", Toast.LENGTH_SHORT).show();
    }

    /**
     * 当设备管理员被禁用时调用
     */
    @Override
    public void onDisabled(Context context, Intent intent) {
      super.onDisabled(context, intent);
      LogUtils.i(context, TAG, "设备管理员已禁用");
      Toast.makeText(context, "设备管理员已禁用", Toast.LENGTH_SHORT).show();
    }

    /**
     * 当用户尝试禁用设备管理员时调用
     * 可以在这里显示警告信息
     */
    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
      LogUtils.w(context, TAG, "用户请求禁用设备管理员");
      return "禁用设备管理员可能导致某些功能无法使用";
    }

    /**
     * 当设备管理员被禁用时调用（用户确认后）
     */
    @Override
    public void onDisabled(Context context, Intent intent, CharSequence reason) {
      super.onDisabled(context, intent, reason);
      LogUtils.i(context, TAG, "设备管理员已被用户禁用: " + reason);
    }

    /**
     * 当密码改变时调用
     */
    @Override
    public void onPasswordChanged(Context context, Intent intent) {
      super.onPasswordChanged(context, intent);
      LogUtils.i(context, TAG, "设备密码已更改");
    }

    /**
     * 当密码失败尝试次数达到限制时调用
     */
    @Override
    public void onPasswordFailed(Context context, Intent intent) {
      super.onPasswordFailed(context, intent);
      LogUtils.w(context, TAG, "密码输入失败");
    }

    /**
     * 当密码成功时调用
     */
    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
      super.onPasswordSucceeded(context, intent);
      LogUtils.i(context, TAG, "密码输入成功");
    }
}
