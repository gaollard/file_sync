# MediaProjection 单次截图说明

## 功能概述

修改 MediaProjectionService，使其在完成一次截图并上传后自动关闭服务，不再持续运行和重复截图。

## 问题背景

**原来的实现**：
- 服务启动后会持续运行
- 使用 `frameCounter` 计数器控制截图频率
- 每隔一定帧数才截图一次
- 服务不会自动停止，需要手动关闭

**问题**：
- 资源浪费：服务一直在后台运行
- 耗电增加：持续监听屏幕变化
- 不符合需求：只需要截图一次，不需要持续截图

## 解决方案

### 1. 移除计数器逻辑

**删除**：
```java
private int frameCounter = 0;
private final int targetFrames = 60 * 60;
```

**新增**：
```java
private VirtualDisplay mVirtualDisplay;
private boolean screenshotTaken = false;  // 标记是否已截图
```

### 2. 优化截图逻辑

**修改前**：
```java
if (++frameCounter < 5) {
    frameCounter = 0;
    // 处理截图...
}
```

**修改后**：
```java
// 如果已经截图，直接返回
if (screenshotTaken) {
    return;
}

// 标记已经开始截图，避免重复处理
screenshotTaken = true;

// 处理截图...
```

### 3. 自动关闭服务

在上传成功或失败后，都会调用 `stopScreenCapture()` 方法关闭服务：

```java
fileUpload.uploadImage(
    ApiConfig.report_screenshot,
    finalBytes,
    "screenshot_" + System.currentTimeMillis() + ".jpg",
    new ApiCallback() {
        @Override
        public void onSuccess(String res) {
            Toast.makeText(MediaProjectionService.this,
                    "截图上传成功", Toast.LENGTH_SHORT).show();
            // 上传成功后关闭服务
            handler.postDelayed(() -> stopScreenCapture(), 500);
        }

        @Override
        public void onFailure(Exception e) {
            Toast.makeText(MediaProjectionService.this,
                    "上传失败: " + e.getMessage(), Toast.LENGTH_SHORT)
                    .show();
            // 即使上传失败也关闭服务
            handler.postDelayed(() -> stopScreenCapture(), 500);
        }
    });
```

### 4. 完善资源释放

新增 `stopScreenCapture()` 方法，确保正确释放所有资源：

```java
/**
 * 停止屏幕捕获并释放资源
 */
private void stopScreenCapture() {
    try {
        // 1. 关闭虚拟显示器
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        
        // 2. 停止媒体投影
        if (mProjection != null) {
            mProjection.stop();
            mProjection = null;
        }
        
        // 3. 关闭图像读取器
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        
        Toast.makeText(this, "截图服务已关闭", Toast.LENGTH_SHORT).show();
        
        // 4. 停止前台服务
        stopForeground(true);
        stopSelf();
    } catch (Exception e) {
        Toast.makeText(this, "关闭服务失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
```

## 执行流程

### 完整截图流程

```
1. 用户点击截图按钮
   ↓
2. 启用无障碍自动授权
   ↓
3. 弹出 MediaProjection 授权对话框
   ↓
4. 无障碍服务自动点击"立即开始"
   ↓
5. 启动 MediaProjectionService
   ↓
6. 创建 VirtualDisplay 开始捕获屏幕
   ↓
7. ImageReader 回调触发（屏幕有变化时）
   ↓
8. 检查 screenshotTaken 标志
   ├─ true：直接返回（避免重复截图）
   └─ false：继续处理
       ↓
9. 设置 screenshotTaken = true
   ↓
10. 处理图像数据，转换为 Bitmap
    ↓
11. 保存到相册
    ↓
12. 压缩图片（JPEG, 80%）
    ↓
13. 上传到服务器
    ↓
14. 上传成功或失败
    ↓
15. 延迟 500ms 后调用 stopScreenCapture()
    ↓
16. 释放所有资源
    ├─ 关闭 VirtualDisplay
    ├─ 停止 MediaProjection
    └─ 关闭 ImageReader
    ↓
17. 停止前台服务
    ↓
18. 服务完全关闭
```

## 关键改进点

### 1. **避免重复截图**
使用 `screenshotTaken` 布尔标志：
- 首次触发回调时设置为 `true`
- 后续回调直接返回，不再处理

### 2. **确保资源释放**
按正确顺序释放资源：
1. VirtualDisplay（停止屏幕捕获）
2. MediaProjection（停止媒体投影）
3. ImageReader（关闭图像读取器）

### 3. **异常处理**
所有可能出错的地方都会调用 `stopScreenCapture()`：
- Bitmap 创建失败
- 数据获取失败
- 上传成功
- 上传失败
- 发生异常

### 4. **延迟关闭**
使用 `handler.postDelayed(..., 500)` 延迟 500ms 后关闭：
- 确保 Toast 消息能显示
- 确保上传回调完全执行
- 避免资源释放时的并发问题

## 代码对比

### 原来的实现

```java
// ❌ 会持续运行，每隔一定帧数截图一次
if (++frameCounter < 5) {
    frameCounter = 0;
    // 处理截图...
    // 上传完成后服务继续运行
}
```

### 现在的实现

```java
// ✅ 只截图一次，完成后自动关闭服务
if (screenshotTaken) {
    return;  // 避免重复截图
}

screenshotTaken = true;

// 处理截图...
// 上传完成后调用 stopScreenCapture() 关闭服务
```

## 测试验证

### 1. 功能测试

```bash
# 启动应用并监听日志
adb logcat -s MediaProjectionService:* MyAccessibilityService:* MainActivity:*

# 测试步骤：
# 1. 点击截图按钮
# 2. 观察无障碍服务是否自动授权
# 3. 等待截图完成
# 4. 检查图片是否保存到相册
# 5. 检查图片是否上传成功
# 6. 验证服务是否自动关闭
```

### 2. 资源释放测试

```bash
# 查看服务是否还在运行
adb shell dumpsys activity services | grep MediaProjectionService

# 预期结果：截图完成后，服务应该不在列表中
```

### 3. 通知测试

截图过程中应该能看到通知，完成后通知应该自动消失。

## 注意事项

### 1. **首次回调可能有延迟**

ImageReader 的回调在屏幕内容变化时触发：
- 如果屏幕静止，可能需要等待
- 可以在授权后稍微滑动屏幕触发回调
- 建议在授权后延迟 1-2 秒再进行其他操作

### 2. **权限要求**

需要以下权限：
- `FOREGROUND_SERVICE_MEDIA_PROJECTION`：前台服务权限
- 无障碍服务：用于自动授权
- MediaProjection 授权：用户必须同意屏幕捕获

### 3. **Android 版本兼容**

- **Android 5.0 (API 21)+**：支持 MediaProjection API
- **Android 8.0 (API 26)+**：必须使用前台服务
- **Android 14 (API 34)+**：需要指定前台服务类型

### 4. **内存管理**

确保 Bitmap 被正确回收：
```java
bitmap.recycle();  // 释放 Bitmap 内存
image.close();     // 释放 Image 资源
```

## 优势对比

| 特性 | 原实现 | 新实现 |
|------|--------|--------|
| 截图次数 | 持续截图 | 单次截图 |
| 资源占用 | 一直运行 | 完成即关闭 |
| 耗电情况 | 持续耗电 | 按需使用 |
| 通知显示 | 一直显示 | 自动消失 |
| 用户体验 | 需要手动关闭 | 自动关闭 |
| 代码复杂度 | 需要计数器 | 简单标志位 |

## 扩展功能

如果将来需要支持连续截图，可以：

### 方案 1：延迟重启服务

```java
// 在关闭服务前设置定时器
handler.postDelayed(() -> {
    Intent intent = new Intent(this, MediaProjectionService.class);
    // 重新传入授权数据
    startForegroundService(intent);
}, 30000); // 30秒后重新截图
```

### 方案 2：添加配置参数

```java
public class MediaProjectionService extends Service {
    private boolean continuousMode = false;  // 是否连续截图模式
    
    // 在上传成功后判断
    if (continuousMode) {
        screenshotTaken = false;  // 重置标志，继续截图
    } else {
        stopScreenCapture();  // 单次模式，关闭服务
    }
}
```

## 故障排除

### Q1: 服务没有自动关闭？

**可能原因**：
1. 上传回调没有触发
2. Handler 延迟没有执行
3. 异常被捕获但没有调用关闭方法

**解决方法**：
1. 检查日志，确认上传是否完成
2. 添加更多日志输出
3. 确保所有异常路径都调用 `stopScreenCapture()`

### Q2: 服务关闭太快，截图不完整？

**解决方法**：
增加延迟时间：
```java
handler.postDelayed(() -> stopScreenCapture(), 1000); // 改为 1 秒
```

### Q3: 多次点击截图按钮会怎样？

**当前行为**：
- 每次点击都会启动新的服务实例
- 如果上一次还没完成，可能会有多个服务同时运行

**改进建议**：
在 MainActivity 中添加状态检查：
```java
private boolean isCapturing = false;

private void startMediaProjectionService() {
    if (isCapturing) {
        Toast.makeText(this, "正在截图中，请稍候...", Toast.LENGTH_SHORT).show();
        return;
    }
    
    isCapturing = true;
    // 启动服务...
}
```

## 总结

通过这次修改：
1. ✅ 实现了单次截图自动关闭功能
2. ✅ 避免了资源浪费和持续耗电
3. ✅ 确保了资源的正确释放
4. ✅ 改善了用户体验
5. ✅ 简化了代码逻辑

现在 MediaProjectionService 的行为更符合"按需使用"的原则，不会在后台持续运行，提升了应用的整体性能和用户体验。

