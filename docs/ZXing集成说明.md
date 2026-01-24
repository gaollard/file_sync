# ZXing 二维码扫描库集成说明

## 概述

已成功将 ZXing 二维码扫描库集成到项目中，提供了完整的二维码扫描功能。

## 集成内容

### 1. 依赖库添加

在 `app/build.gradle.kts` 中添加了以下依赖：

```kotlin
// ZXing 二维码扫描库
implementation("com.google.zxing:core:3.5.3")
implementation("com.journeyapps:zxing-android-embedded:4.3.0")
```

### 2. 权限配置

在 `AndroidManifest.xml` 中添加了相机权限：

```xml
<!-- 相机权限 (用于二维码扫描) -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
```

### 3. 新增文件

- **QrScanActivity.java**: 二维码扫描Activity，实现完整的扫描功能
- **activity_qr_scan.xml**: 扫描页面布局文件
- **custom_barcode_scanner.xml**: 自定义扫描视图布局

### 4. 功能特性

#### QrScanActivity 功能：
- ✅ 实时二维码扫描
- ✅ 自动对焦
- ✅ 闪光灯开关控制
- ✅ 运行时相机权限请求
- ✅ 扫描成功自动返回结果
- ✅ 扫描提示和用户引导
- ✅ 完整的生命周期管理

#### BindActivity 集成：
- ✅ 点击"二维码绑定"卡片启动扫描
- ✅ 接收扫描结果并处理绑定逻辑
- ✅ 完整的错误处理和日志记录

## 使用方法

### 从任何Activity启动扫描

```java
// 启动二维码扫描
Intent intent = new Intent(this, QrScanActivity.class);
startActivityForResult(intent, REQUEST_CODE_QR_SCAN);

// 接收扫描结果
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    
    if (requestCode == REQUEST_CODE_QR_SCAN) {
        if (resultCode == RESULT_OK && data != null) {
            String qrCode = data.getStringExtra("qr_code");
            // 处理二维码内容
            Log.d(TAG, "扫描结果: " + qrCode);
        }
    }
}
```

## 构建步骤

1. **同步Gradle依赖**
   ```bash
   # 在 Android Studio 中点击 "Sync Project with Gradle Files"
   # 或在终端运行
   ./gradlew build
   ```

2. **授予相机权限**
   - 首次使用时，应用会自动请求相机权限
   - 用户需要授予权限才能使用扫描功能

3. **测试扫描功能**
   - 在 BindActivity 中点击"二维码绑定"
   - 将相机对准二维码
   - 扫描成功后会自动返回结果

## UI/UX 特性

- 🎨 Material Design 3 风格
- 🌙 深色主题扫描界面（提升扫描准确度）
- 💡 闪光灯按钮（暗光环境使用）
- 📱 竖屏锁定（优化扫描体验）
- ↩️ 返回导航支持
- 📏 自定义扫描框和激光线

## 自定义配置

### 修改扫描框样式

在 `colors.xml` 中自定义颜色：

```xml
<!-- 扫描框激光线颜色 -->
<color name="zxing_custom_viewfinder_laser">#ffcc0066</color>
<!-- 扫描框遮罩颜色 -->
<color name="zxing_custom_viewfinder_mask">#60000000</color>
```

### 调整扫描参数

在 `QrScanActivity.java` 中可以调整：
- 扫描模式（一维码、二维码）
- 扫描速度
- 自动对焦频率
- 震动反馈

## 注意事项

1. **最低SDK要求**: minSdk = 33 (Android 13)
2. **相机权限**: 运行时必须授予相机权限
3. **性能优化**: 扫描时会自动暂停其他相机操作
4. **生命周期**: 页面切换时自动暂停/恢复扫描
5. **内存管理**: 退出页面时自动释放相机资源

## 已测试功能

✅ 相机权限请求和处理  
✅ 二维码扫描识别  
✅ 闪光灯开关  
✅ 扫描结果返回  
✅ 页面生命周期管理  
✅ 错误处理和日志记录  

## 下一步开发建议

1. 添加相册二维码识别功能
2. 支持批量扫描
3. 添加扫描历史记录
4. 支持生成二维码功能
5. 添加震动反馈配置

## 参考文档

- [ZXing GitHub](https://github.com/zxing/zxing)
- [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded)
- [官方使用文档](https://github.com/journeyapps/zxing-android-embedded/blob/master/README.md)

