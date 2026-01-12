package com.example.test_filesync.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import com.example.test_filesync.R;

/**
 * 悬浮窗工具类
 * 用于创建、显示和移除悬浮窗
 */
public class FloatingWindowHelper {
    
    private static final String TAG = "FloatingWindowHelper";
    private static WindowManager windowManager;
    private static View floatingView;
    private static WindowManager.LayoutParams layoutParams;
    
    /**
     * 检查是否有悬浮窗权限
     * @param context 上下文
     * @return true 如果有权限
     */
    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true; // Android 6.0以下不需要权限
    }
    
    /**
     * 请求悬浮窗权限
     * @param activity Activity上下文
     * @param requestCode 请求码
     */
    public static void requestOverlayPermission(Activity activity, int requestCode) {
        if (hasOverlayPermission(activity)) {
            LogUtils.i(activity, TAG, "悬浮窗权限已授予");
            Toast.makeText(activity, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(android.net.Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, requestCode);
            LogUtils.i(activity, TAG, "请求悬浮窗权限");
        }
    }
    
    /**
     * 显示悬浮窗
     * @param context 上下文（建议使用Service或Application Context）
     * @return true 如果成功显示
     */
    public static boolean showFloatingWindow(Context context) {
        if (!hasOverlayPermission(context)) {
            Toast.makeText(context, "请先授予悬浮窗权限", Toast.LENGTH_LONG).show();
            return false;
        }
        
        if (floatingView != null) {
            LogUtils.w(context, TAG, "悬浮窗已存在");
            return false;
        }
        
        try {
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            LayoutInflater inflater = LayoutInflater.from(context);
            floatingView = inflater.inflate(R.layout.floating_window, null);
            
            // 设置布局参数
            layoutParams = new WindowManager.LayoutParams();
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            }
            
            layoutParams.format = PixelFormat.TRANSLUCENT;
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            
            // 设置悬浮窗位置和大小
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.gravity = Gravity.TOP | Gravity.END;
            layoutParams.x = 0;
            layoutParams.y = 100;
            
            // 添加悬浮窗
            windowManager.addView(floatingView, layoutParams);
            
            // 设置关闭按钮点击事件
            // View closeButton = floatingView.findViewById(R.id.close_button);
            // if (closeButton != null) {
            //     closeButton.setOnClickListener(v -> hideFloatingWindow(context));
            // }
            
            // 设置悬浮窗可拖动
            setupDragListener();
            
            LogUtils.i(context, TAG, "悬浮窗已显示");
            return true;
        } catch (Exception e) {
            LogUtils.e(context, TAG, "显示悬浮窗失败: " + e.getMessage(), e);
            Toast.makeText(context, "显示悬浮窗失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    /**
     * 隐藏悬浮窗
     * @param context 上下文
     */
    public static void hideFloatingWindow(Context context) {
        if (floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
                floatingView = null;
                windowManager = null;
                layoutParams = null;
                LogUtils.i(context, TAG, "悬浮窗已隐藏");
            } catch (Exception e) {
                LogUtils.e(context, TAG, "隐藏悬浮窗失败: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 检查悬浮窗是否正在显示
     * @return true 如果正在显示
     */
    public static boolean isShowing() {
        return floatingView != null;
    }
    
    /**
     * 设置悬浮窗拖动功能
     */
    private static void setupDragListener() {
        if (floatingView == null || layoutParams == null) {
            return;
        }
        
        View dragView = floatingView.findViewById(R.id.drag_area);
        if (dragView == null) {
            // 如果没有专门的拖动区域，则整个视图可拖动
            dragView = floatingView;
        }
        
        dragView.setOnTouchListener(new FloatingWindowTouchListener(layoutParams, windowManager));
    }
    
    /**
     * 更新悬浮窗内容
     * @param context 上下文
     * @param text 要显示的文本
     */
    public static void updateContent(Context context, String text) {
        if (floatingView == null) {
            return;
        }
        
        android.widget.TextView contentView = floatingView.findViewById(R.id.content_text);
        if (contentView != null) {
            contentView.setText(text);
        }
    }
}

