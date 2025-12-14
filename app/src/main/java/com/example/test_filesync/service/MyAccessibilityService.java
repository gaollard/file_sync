package com.example.test_filesync.service;

import com.example.test_filesync.util.LogUtils;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityService extends AccessibilityService {
    
    private static MyAccessibilityService instance;
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 监听系统事件
        LogUtils.d(this, "onAccessibilityEvent: " + event.toString());
    }

    @Override
    public void onInterrupt() {
        // 服务中断处理
    }
    
    /**
     * 获取服务实例
     */
    public static MyAccessibilityService getInstance() {
        return instance;
    }
    
    /**
     * 检查服务是否已启用
     */
    public static boolean isServiceEnabled() {
        return instance != null;
    }
    
    // 触发截图的关键方法
    public void triggerScreenshot() {
        // 用户无感（无界面跳转/无弹窗提示）的实现应优先选择 performGlobalAction（Android 9+ 才支持，用户无需感知）
        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
    }
}
