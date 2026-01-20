# 截图上传功能使用指南

## 功能概述

本应用支持自动截图并上传到服务器的功能。截图后会自动从相册中获取最新截图并上传。

## 实现方式

应用提供了两种截图方式：

### 1. AccessibilityService 截图（推荐）
- **优点**：简单、无感知、不需要额外授权
- **要求**：Android 9.0+ (API 28+)
- **使用方法**：
  1. 在系统设置中开启本应用的无障碍服务
  2. 点击截图按钮，系统自动触发截图
  3. 等待2秒后自动从相册获取截图并上传

### 2. MediaProjection 截图（备用方案）
- **优点**：兼容性好，支持更低版本Android
- **缺点**：需要用户授权屏幕录制权限
- **使用方法**：
  1. 点击截图按钮
  2. 系统弹出权限请求，允许屏幕录制
  3. 自动截图并保存到相册
  4. 自动上传到服务器

## 核心功能流程

```
用户点击截图按钮
    ↓
检查是否支持AccessibilityService截图
    ↓
是 → 触发系统截图 (performGlobalAction)
    ↓
等待2秒（让系统保存截图到相册）
    ↓
查询MediaStore获取最近5秒内添加的图片
    ↓
读取图片数据转换为字节数组
    ↓
使用FileUpload工具上传到服务器
    ↓
显示上传结果提示
```

## 关键代码位置

### 1. MyAccessibilityService.java
- `triggerScreenshot()` - 触发截图的方法
- `getLatestScreenshotAndUpload()` - 从相册获取最新截图
- `uploadScreenshot()` - 上传截图到服务器

### 2. MainActivity.java
- `tryAccessibilityScreenshot()` - 尝试使用无障碍服务截图
- `startMediaProjectionService()` - 启动媒体投影截图服务

### 3. FileUpload.java
- `uploadImage()` - 上传图片的通用方法

## 权限要求

在AndroidManifest.xml中已配置以下权限：
- `READ_MEDIA_IMAGES` - 读取相册图片
- `READ_MEDIA_VIDEO` - 读取视频（可选）
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` - 前台服务媒体投影

## API配置

上传接口配置在 `ApiConfig.java` 中：
```java
public static final String report_screenshot = "/app/report/report_screenshot";
```

完整URL: `{BASE_URL}/app/report/report_screenshot`

## 注意事项

1. **权限开启**：首次使用需要在系统设置中开启无障碍服务
2. **延迟处理**：截图后需要等待2秒让系统保存文件，才能从相册读取
3. **时间窗口**：只查询最近5秒内添加的图片，确保获取的是刚刚截取的图片
4. **文件命名**：上传的文件名格式为 `screenshot_yyyyMMdd_HHmmss.jpg`
5. **错误处理**：如果未找到截图或上传失败，会显示Toast提示

## 故障排查

### 问题：点击截图按钮没有反应
**解决方案**：
1. 检查是否开启了无障碍服务
2. 检查Android版本是否支持（需要9.0+）
3. 查看LogCat日志确认错误信息

### 问题：截图成功但未找到文件
**解决方案**：
1. 增加延迟时间（默认2秒可能不够）
2. 检查是否有存储权限
3. 确认系统截图保存位置

### 问题：上传失败
**解决方案**：
1. 检查网络连接
2. 确认API地址配置正确
3. 检查服务器端接口是否正常

## 开发建议

如需调试，可以在以下位置添加日志：
```java
LogUtils.d(this, "调试信息");
```

查看完整的截图和上传流程日志：
```bash
adb logcat | grep -E "MyAccessibilityService|FileUpload"
```

