# BindActivity 使用文档

## 概述

`BindActivity` 是设备绑定页面，实现了根据需求文档 `绑定页面.md` 中描述的两种绑定方式：

1. **二维码绑定** - 点击后跳转二维码扫描页面
2. **管控码绑定** - 点击后出现输入弹窗

## 功能特性

- ✅ 美观的 Material Design UI
- ✅ 两种绑定方式供用户选择
- ✅ 完整的错误处理和用户提示
- ✅ 完善的日志记录
- ✅ 国际化支持（字符串资源分离）
- ✅ 模块化代码结构

## 文件说明

### 1. Activity 类
**位置**: `app/src/main/java/com/example/test_filesync/activity/BindActivity.java`

主要方法：
- `onCreate()` - 初始化页面
- `openQrScanner()` - 打开二维码扫描（待集成扫描库）
- `showCodeInputDialog()` - 显示管控码输入对话框
- `handleControlCodeBind(String code)` - 处理管控码绑定逻辑
- `handleQrCodeBind(String qrCode)` - 处理二维码绑定逻辑
- `onBindSuccess(String bindCode)` - 绑定成功回调

### 2. 布局文件
**位置**: `app/src/main/res/layout/activity_bind.xml`

布局特点：
- 垂直居中布局
- 使用 MaterialCardView 展示两个选项
- 每个选项包含图标、标题和描述
- 支持点击波纹效果

### 3. 字符串资源
**位置**: `app/src/main/res/values/strings.xml`

已定义的字符串资源：
- `bind_title` - 页面标题
- `bind_qr_title` - 二维码绑定标题
- `bind_qr_description` - 二维码绑定描述
- `bind_code_title` - 管控码绑定标题
- `bind_code_description` - 管控码绑定描述
- `bind_code_input_hint` - 输入框提示
- 各种提示和错误信息

### 4. AndroidManifest 注册
**位置**: `app/src/main/AndroidManifest.xml`

```xml
<activity
    android:name=".activity.BindActivity"
    android:exported="false"
    android:theme="@style/Theme.test_filesync" />
```

## 使用方法

### 从其他 Activity 启动绑定页面

```java
// 在任意 Activity 中启动绑定页面
Intent intent = new Intent(this, BindActivity.class);
startActivity(intent);
```

### 示例：在 MainActivity 中添加绑定入口

```java
// 在 MainActivity.java 中
Button btnBind = findViewById(R.id.btn_bind);
btnBind.setOnClickListener(v -> {
    Intent intent = new Intent(MainActivity.this, BindActivity.class);
    startActivity(intent);
});
```

## 待完成功能（TODO）

### 1. 集成二维码扫描功能

需要集成二维码扫描库，推荐使用 **ZXing** 或 **ML Kit**。

#### 使用 ZXing 的示例

1. 添加依赖到 `build.gradle`:
```gradle
dependencies {
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
    implementation 'com.google.zxing:core:3.5.1'
}
```

2. 修改 `openQrScanner()` 方法:
```java
private void openQrScanner() {
    try {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("扫描设备二维码");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
        
        LogUtils.i(this, TAG, "准备打开二维码扫描页面");
    } catch (Exception e) {
        Toast.makeText(this, getString(R.string.bind_qr_error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        LogUtils.e(this, TAG, "打开二维码扫描失败: " + e.getMessage());
    }
}
```

3. 修改 `onActivityResult()` 方法:
```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
    if (result != null) {
        if (result.getContents() == null) {
            Toast.makeText(this, R.string.bind_qr_cancel, Toast.LENGTH_SHORT).show();
        } else {
            String qrCode = result.getContents();
            LogUtils.i(this, TAG, "扫描二维码成功: " + qrCode);
            handleQrCodeBind(qrCode);
        }
    } else {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
```

### 2. 实现后端 API 调用

需要在 `ApiConfig.java` 中添加绑定接口：

```java
// 在 ApiConfig.java 中添加
public static final String BIND_DEVICE_QR = "/app/device/bind_qr";      // 二维码绑定
public static final String BIND_DEVICE_CODE = "/app/device/bind_code";  // 管控码绑定
```

然后在 `BindActivity` 中实现网络请求：

```java
private void handleControlCodeBind(String code) {
    if (TextUtils.isEmpty(code)) {
        Toast.makeText(this, R.string.bind_code_empty_error, Toast.LENGTH_SHORT).show();
        return;
    }
    
    Toast.makeText(this, R.string.bind_in_progress, Toast.LENGTH_SHORT).show();
    
    // 调用后端 API
    // 使用项目中的网络请求框架
    // 示例：
    // ApiService.bindDeviceWithCode(code, new ApiCallback() {
    //     @Override
    //     public void onSuccess(Response response) {
    //         onBindSuccess(code);
    //     }
    //     
    //     @Override
    //     public void onError(String error) {
    //         Toast.makeText(BindActivity.this, "绑定失败: " + error, Toast.LENGTH_SHORT).show();
    //     }
    // });
}
```

### 3. 保存绑定信息

绑定成功后需要保存设备信息到本地：

```java
private void onBindSuccess(String bindCode) {
    LogUtils.i(this, TAG, "设备绑定成功: " + bindCode);
    Toast.makeText(this, R.string.bind_success, Toast.LENGTH_LONG).show();
    
    // 保存绑定信息到 SharedPreferences
    SharedPreferences prefs = getSharedPreferences("device_info", MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString("bind_code", bindCode);
    editor.putLong("bind_time", System.currentTimeMillis());
    editor.putBoolean("is_bound", true);
    editor.apply();
    
    // 跳转到主页面
    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
}
```

## UI 预览

页面包含以下元素：

```
┌─────────────────────────────────┐
│                                 │
│         设备绑定                 │
│                                 │
│  ┌───────────────────────────┐  │
│  │        📷                 │  │
│  │    二维码绑定              │  │
│  │  扫描二维码快速绑定设备     │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │        ✏️                 │  │
│  │    管控码绑定              │  │
│  │  输入管控码手动绑定设备     │  │
│  └───────────────────────────┘  │
│                                 │
└─────────────────────────────────┘
```

## 测试建议

1. **管控码绑定测试**：
   - 点击管控码绑定
   - 输入空字符串 → 应显示错误提示
   - 输入有效管控码 → 应显示绑定中，然后显示成功

2. **二维码绑定测试**（集成扫描库后）：
   - 点击二维码绑定
   - 取消扫描 → 应显示取消提示
   - 扫描有效二维码 → 应显示绑定中，然后显示成功

3. **UI 测试**：
   - 检查卡片点击波纹效果
   - 检查对话框显示和关闭
   - 检查 Toast 提示显示

## 日志输出

所有关键操作都会记录日志，标签为 `BindActivity`：

- 页面启动
- 点击绑定选项
- 扫描/输入绑定码
- 绑定成功/失败

可以通过 LogUtils 查看详细日志。

## 注意事项

1. 当前二维码扫描功能使用 Toast 提示，需要集成实际扫描库
2. 网络请求使用模拟延迟，需要替换为实际 API 调用
3. 绑定成功后的跳转逻辑需要根据实际需求调整
4. 建议在正式环境中添加网络状态检查
5. 建议添加加载对话框显示绑定进度

## 扩展建议

1. **添加绑定历史记录**
2. **支持多设备绑定**
3. **添加绑定二次确认**
4. **支持解绑功能**
5. **添加绑定状态同步**

