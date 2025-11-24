package com.example.test_filesync.util;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.example.test_filesync.receiver.PolicyReceiver;

/**
 * 设备管理员工具类
 * 用于激活、禁用和管理设备管理员功能
 */
public class DeviceAdminHelper {
    
    private static final String TAG = "DeviceAdminHelper";
    
    /**
     * 获取设备管理员组件名称
     */
    public static ComponentName getAdminComponent(Context context) {
        return new ComponentName(context, PolicyReceiver.class);
    }
    
    /**
     * 检查设备管理员是否已激活
     * @param context 上下文
     * @return true 如果设备管理员已激活
     */
    public static boolean isDeviceAdminActive(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = getAdminComponent(context);
        return dpm != null && dpm.isAdminActive(adminComponent);
    }
    
    /**
     * 激活设备管理员
     * 会弹出系统对话框让用户确认
     * @param activity Activity 上下文（用于 startActivityForResult）
     * @param requestCode 请求码，用于在 onActivityResult 中处理结果
     */
    public static void activateDeviceAdmin(android.app.Activity activity, int requestCode) {
        if (isDeviceAdminActive(activity)) {
            Toast.makeText(activity, "设备管理员已激活", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getAdminComponent(activity));
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "激活设备管理员以启用设备管理功能，如锁定屏幕、清除数据等");
        activity.startActivityForResult(intent, requestCode);
        
        LogUtils.i(activity, TAG, "请求激活设备管理员");
    }
    
    /**
     * 禁用设备管理员
     * 注意：需要先取消激活设备管理员权限
     */
    public static void deactivateDeviceAdmin(Context context) {
        if (!isDeviceAdminActive(context)) {
            Toast.makeText(context, "设备管理员未激活", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = getAdminComponent(context);
        
        if (dpm != null) {
            dpm.removeActiveAdmin(adminComponent);
            LogUtils.i(context, TAG, "设备管理员已禁用");
            Toast.makeText(context, "设备管理员已禁用", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 锁定设备屏幕
     * 需要设备管理员已激活
     */
    public static boolean lockNow(Context context) {
        if (!isDeviceAdminActive(context)) {
            Toast.makeText(context, "请先激活设备管理员", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = getAdminComponent(context);
        
        if (dpm != null) {
            try {
                dpm.lockNow();
                LogUtils.i(context, TAG, "设备已锁定");
                return true;
            } catch (SecurityException e) {
                LogUtils.e(context, TAG, "锁定设备失败: " + e.getMessage());
                Toast.makeText(context, "锁定设备失败", Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

}

