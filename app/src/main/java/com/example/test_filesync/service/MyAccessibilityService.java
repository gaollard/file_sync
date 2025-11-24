package com.example.test_filesync.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityService extends AccessibilityService {
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 监听系统事件
    }

    @Override
    public void onInterrupt() {
        // 服务中断处理
    }
    
    // 触发截图的关键方法
    public void triggerScreenshot() {
        // 方法一：发送系统截图广播
        Intent screenshotIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        sendBroadcast(screenshotIntent);
        
        // 方法二：执行全局操作（Android 9+）
        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
    }
}
