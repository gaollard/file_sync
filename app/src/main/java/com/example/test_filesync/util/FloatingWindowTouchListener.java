package com.example.test_filesync.util;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * 悬浮窗触摸监听器
 * 用于实现悬浮窗的拖动功能
 */
public class FloatingWindowTouchListener implements View.OnTouchListener {
    
    private WindowManager.LayoutParams layoutParams;
    private WindowManager windowManager;
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    
    public FloatingWindowTouchListener(WindowManager.LayoutParams layoutParams, WindowManager windowManager) {
        this.layoutParams = layoutParams;
        this.windowManager = windowManager;
    }
    
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 记录初始位置
                initialX = layoutParams.x;
                initialY = layoutParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                return true;
                
            case MotionEvent.ACTION_MOVE:
                // 计算移动距离
                int deltaX = (int) (event.getRawX() - initialTouchX);
                int deltaY = (int) (event.getRawY() - initialTouchY);
                
                // 更新悬浮窗位置
                layoutParams.x = initialX + deltaX;
                layoutParams.y = initialY + deltaY;
                
                // 更新视图位置
                if (windowManager != null) {
                    windowManager.updateViewLayout(view, layoutParams);
                }
                return true;
                
            case MotionEvent.ACTION_UP:
                // 触摸结束，可以在这里添加点击事件处理
                return true;
        }
        return false;
    }
}

