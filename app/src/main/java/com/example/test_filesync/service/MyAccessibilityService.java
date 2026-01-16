package com.example.test_filesync.service;

import com.example.test_filesync.api.ApiCallback;
import com.example.test_filesync.api.ApiConfig;
import com.example.test_filesync.api.dto.UserInfo;
import com.example.test_filesync.util.LogUtils;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;

import com.example.test_filesync.util.HttpUtil;
import com.google.gson.Gson;

public class MyAccessibilityService extends AccessibilityService {
    private static final String TAG = "MyAccessibilityService";
    private static MyAccessibilityService instance;
    // 要强制停止的目标包名
    private static String targetPackageToForceStop = null;
    // 是否正在执行强制停止操作
    private static boolean isForceStopInProgress = false;
    // 回调接口
    private static ForceStopCallback forceStopCallback;
    private Handler handler = new Handler(Looper.getMainLooper());
    // 是否处于监控模式 0: 不监控 1: 监控
    private static int isMonitor = 0;
    // 定时任务 Runnable，每10秒执行一次
    private Runnable queryConfigRunnable = new Runnable() {
        @Override
        public void run() {
            query_config();
            // 5秒后再次执行
            handler.postDelayed(this, 10 * 1000);
        }
    };

    // 强制停止回调接口
    public interface ForceStopCallback {
        void onForceStopResult(boolean success, String message);
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        LogUtils.d(this, "Accessibility Service Connected");
        handler.post(queryConfigRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 停止定时任务
        handler.removeCallbacks(queryConfigRunnable);
        instance = null;
        targetPackageToForceStop = null;
        isForceStopInProgress = false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        int eventType = event.getEventType();

        // 监听窗口状态变化（App 打开/切换）
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // 获取当前打开的应用包名
            CharSequence packageName = event.getPackageName();
            // 获取当前 Activity 类名
            CharSequence className = event.getClassName();

            if (packageName != null) {
                String pkg = packageName.toString();
                String cls = className != null ? className.toString() : "";

                LogUtils.d(this, "App 打开: " + pkg + " / " + cls);

                // 示例：检测特定应用打开
                if (pkg.equals("com.tencent.mm")) {
                    LogUtils.d(this, "微信被打开了！");
                    Toast.makeText(this, "微信被打开了！", Toast.LENGTH_SHORT).show();
                } else if (pkg.equals("com.tencent.mobileqq")) {
                    LogUtils.d(this, "QQ被打开了！");
                    Toast.makeText(this, "QQ被打开了！", Toast.LENGTH_SHORT).show();
                } else if (pkg.equals("com.hpbr.bosszhipin")) {
                    LogUtils.d(this, "BOSS直聘被打开了！");
                    Toast.makeText(this, "BOSS直聘被打开了！", Toast.LENGTH_SHORT).show();
                   if (isMonitor == 1) {
                        Toast.makeText(this, "监控模式下，正在自动执行强制停止...", Toast.LENGTH_SHORT).show();
                        // 执行强制停止操作
                        setForceStopTarget(pkg, null);
                        // 打开BOSS直聘的设置页面
                        try {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + "com.hpbr.bosszhipin"));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getApplicationContext().startActivity(intent);
                            Toast.makeText(getApplicationContext(), "正在自动执行强制停止...", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            cancelForceStop();
                            Toast.makeText(getApplicationContext(), "无法打开应用设置: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                   }
                }
            }
        }

        // 如果不是正在执行强制停止操作，直接返回
        if (!isForceStopInProgress || targetPackageToForceStop == null) {
            return;
        }

        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";

        // 只处理设置应用的事件
        if (!packageName.contains("settings") && !packageName.contains("Settings")) {
            return;
        }

        LogUtils.d(this, "Event: " + eventType + ", Package: " + packageName);

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            // 延迟一点执行，确保界面已经加载完成
            handler.postDelayed(this::performForceStopActions, 300);
        }
    }

    /**
     * 执行强制停止操作
     */
    private void performForceStopActions() {
        if (!isForceStopInProgress) return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            LogUtils.d(this, "Root node is null");
            return;
        }

        try {
            // 步骤1: 查找并点击"强制停止"按钮
            if (clickForceStopButton(rootNode)) {
                LogUtils.d(this, "Clicked force stop button");
                return;
            }

            // 步骤2: 查找并点击确认对话框中的"确定"/"强制停止"按钮
            if (clickConfirmButton(rootNode)) {
                LogUtils.d(this, "Clicked confirm button");
                // 操作完成
                handler.postDelayed(() -> {
                    completeForceStop(true, "强制停止成功");
                }, 500);
                return;
            }

        } catch (Exception e) {
            LogUtils.e(this, "Error in performForceStopActions: " + e.getMessage());
        } finally {
            rootNode.recycle();
        }
    }

    /**
     * 点击"强制停止"按钮
     */
    private boolean clickForceStopButton(AccessibilityNodeInfo rootNode) {
        // 中文设备上的按钮文本
        String[] forceStopTexts = {"强制停止", "强行停止", "Force stop", "FORCE STOP"};

        for (String text : forceStopTexts) {
            List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node.isClickable() && node.isEnabled()) {
                        boolean clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        node.recycle();
                        if (clicked) {
                            LogUtils.d(this, "Found and clicked: " + text);
                            return true;
                        }
                    }
                    // 如果节点本身不可点击，尝试点击其父节点
                    AccessibilityNodeInfo parent = node.getParent();
                    if (parent != null && parent.isClickable() && parent.isEnabled()) {
                        boolean clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        parent.recycle();
                        node.recycle();
                        if (clicked) {
                            LogUtils.d(this, "Found and clicked parent of: " + text);
                            return true;
                        }
                    }
                    node.recycle();
                }
            }
        }
        return false;
    }

    /**
     * 点击确认对话框中的"确定"按钮
     */
    private boolean clickConfirmButton(AccessibilityNodeInfo rootNode) {
        // 确认对话框中的按钮文本
        String[] confirmTexts = {"确定", "确认", "OK", "强制停止", "Force stop", "FORCE STOP"};

        for (String text : confirmTexts) {
            List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    // 检查是否在对话框中（通常是AlertDialog）
                    if (node.isClickable() && node.isEnabled()) {
                        boolean clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        node.recycle();
                        if (clicked) {
                            LogUtils.d(this, "Clicked confirm: " + text);
                            return true;
                        }
                    }
                    // 尝试点击父节点
                    AccessibilityNodeInfo parent = node.getParent();
                    if (parent != null && parent.isClickable() && parent.isEnabled()) {
                        boolean clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        parent.recycle();
                        node.recycle();
                        if (clicked) {
                            return true;
                        }
                    }
                    node.recycle();
                }
            }
        }
        return false;
    }

    /**
     * 完成强制停止操作
     */
    private void completeForceStop(boolean success, String message) {
        isForceStopInProgress = false;
        targetPackageToForceStop = null;

        if (forceStopCallback != null) {
            forceStopCallback.onForceStopResult(success, message);
            forceStopCallback = null;
        }
    }

    @Override
    public void onInterrupt() {
        // 服务中断处理
        LogUtils.d(this, "Accessibility Service Interrupted");
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

    /**
     * 设置要强制停止的目标应用包名
     * @param packageName 目标应用包名
     * @param callback 结果回调
     */
    public static void setForceStopTarget(String packageName, ForceStopCallback callback) {
        targetPackageToForceStop = packageName;
        isForceStopInProgress = true;
        forceStopCallback = callback;

        // 设置超时，10秒后自动取消
        if (instance != null) {
            instance.handler.postDelayed(() -> {
                if (isForceStopInProgress) {
                    completeForceStopStatic(false, "操作超时");
                }
            }, 10000);
        }
    }

    /**
     * 取消强制停止操作
     */
    public static void cancelForceStop() {
        isForceStopInProgress = false;
        targetPackageToForceStop = null;
        forceStopCallback = null;
    }

    /**
     * 检查是否正在执行强制停止
     */
    public static boolean isForceStopInProgress() {
        return isForceStopInProgress;
    }

    /**
     * 静态方法完成强制停止
     */
    private static void completeForceStopStatic(boolean success, String message) {
        isForceStopInProgress = false;
        targetPackageToForceStop = null;

        if (forceStopCallback != null) {
            forceStopCallback.onForceStopResult(success, message);
            forceStopCallback = null;
        }
    }

    // 触发截图的关键方法
    public void triggerScreenshot() {
        // 用户无感（无界面跳转/无弹窗提示）的实现应优先选择 performGlobalAction（Android 9+ 才支持，用户无需感知）
        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
    }

    /**
     * 查询配置方法 - 每5秒自动调用
     */
    private void query_config() {
        Context context = this;
        LogUtils.d(this, "query_config 被调用");
        HttpUtil.config(ApiConfig.user_userInfo, new HashMap<String, Object>())
        .postRequest(this, new ApiCallback() {
            @Override
            public void onSuccess(String res) {
                LogUtils.d(context, "accessibility service query_config success: " + res);
                Gson gson = new Gson();
                UserInfo userInfo = gson.fromJson(res, UserInfo.class);
                isMonitor = userInfo.getConfig().getIsMonitor();
                Log.d("UserApi accessibility service", "configId: " + userInfo.getUniqueId());
                Log.d("UserApi accessibility service", "is_monitor: " + isMonitor);
            }
            @Override
            public void onFailure(Exception e) {
                LogUtils.d(context, "accessibility service query_config failure: " + e.getMessage());
            }
        });
    }
}
