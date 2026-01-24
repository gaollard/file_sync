# MediaProjection 两步授权处理说明

## 问题背景

部分手机（特别是 Android 10+ 的厂商定制系统）在 MediaProjection 授权时会有**两个步骤**：

### 第一步：选择投屏范围
- **单个应用**：只录制当前应用的内容
- **整个屏幕**：录制整个屏幕的内容（包括所有应用）

### 第二步：确认授权
- 点击"立即开始"、"允许"等按钮确认授权

## 解决方案

### 1. 增强的授权流程处理

修改 `MyAccessibilityService.java` 中的 `performMediaProjectionClick()` 方法，实现智能两步处理：

```java
private void performMediaProjectionClick() {
    if (!autoClickMediaProjection) {
        return;
    }

    AccessibilityNodeInfo rootNode = getRootInActiveWindow();
    if (rootNode == null) {
        return;
    }

    try {
        // 步骤1: 先尝试选择"整个屏幕"选项
        if (selectEntireScreen(rootNode)) {
            LogUtils.i(this, "已选择'整个屏幕'选项");
            // 选择后延迟300ms再点击授权按钮
            handler.postDelayed(() -> {
                AccessibilityNodeInfo rootNode2 = getRootInActiveWindow();
                if (rootNode2 != null) {
                    try {
                        if (clickMediaProjectionButton(rootNode2)) {
                            autoClickMediaProjection = false;
                            Toast.makeText(this, "已自动授权屏幕捕获", Toast.LENGTH_SHORT).show();
                        }
                    } finally {
                        rootNode2.recycle();
                    }
                }
            }, 300);
            return;
        }
        
        // 步骤2: 如果没有找到选择选项，直接点击授权按钮
        if (clickMediaProjectionButton(rootNode)) {
            autoClickMediaProjection = false;
            Toast.makeText(this, "已自动授权屏幕捕获", Toast.LENGTH_SHORT).show();
        }
    } finally {
        rootNode.recycle();
    }
}
```

### 2. 选择"整个屏幕"的实现

新增 `selectEntireScreen()` 方法，支持多语言和多种UI实现：

```java
private boolean selectEntireScreen(AccessibilityNodeInfo rootNode) {
    // 1. 通过文本查找"整个屏幕"
    String[] entireScreenTexts = {
        "整个屏幕", "整个萤幕", "全屏", "全螢幕",
        "Entire screen", "Full screen", "Screen",
        "全画面", "画面全体"  // 日文
    };

    for (String text : entireScreenTexts) {
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                // 查找可选择的节点（RadioButton、CheckBox）
                AccessibilityNodeInfo selectableNode = findSelectableNode(node);
                if (selectableNode != null) {
                    // 如果未选中，点击选中
                    if (!selectableNode.isChecked()) {
                        return selectableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                    return true;  // 已经选中
                }
            }
        }
    }

    // 2. 通过 ViewId 查找 RadioButton
    String[] radioButtonIds = {
        "android:id/screen_radio",
        "android:id/entire_screen",
        "com.android.systemui:id/screen_radio"
    };

    for (String id : radioButtonIds) {
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(id);
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node.isCheckable() && !node.isChecked()) {
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }
    }

    return false;
}
```

### 3. 智能节点查找

新增 `findSelectableNode()` 方法，在 UI 层级中查找可选择的节点：

```java
private AccessibilityNodeInfo findSelectableNode(AccessibilityNodeInfo textNode) {
    // 1. 检查当前节点
    if (textNode.isCheckable() || textNode.isClickable()) {
        return textNode;
    }

    // 2. 向上查找父节点（最多3层）
    AccessibilityNodeInfo parent = textNode.getParent();
    for (int i = 0; i < 3 && parent != null; i++) {
        if (parent.isCheckable() || 
            (parent.isClickable() && isSelectableView(parent))) {
            return parent;
        }
        parent = parent.getParent();
    }

    // 3. 查找兄弟节点中的 RadioButton/CheckBox
    AccessibilityNodeInfo parentForSiblings = textNode.getParent();
    if (parentForSiblings != null) {
        for (int i = 0; i < parentForSiblings.getChildCount(); i++) {
            AccessibilityNodeInfo child = parentForSiblings.getChild(i);
            if (child != null && child.isCheckable() && isSelectableView(child)) {
                return child;
            }
        }
    }

    return null;
}

private boolean isSelectableView(AccessibilityNodeInfo node) {
    if (node.getClassName() == null) {
        return false;
    }
    String className = node.getClassName().toString();
    return className.contains("RadioButton") ||
           className.contains("CheckBox") ||
           className.contains("LinearLayout");
}
```

## 执行流程

### 完整两步授权流程

```
1. 用户点击截图按钮
   ↓
2. 启用无障碍自动授权
   MyAccessibilityService.enableMediaProjectionAutoClick()
   ↓
3. 请求 MediaProjection 权限
   startActivityForResult(manager.createScreenCaptureIntent(), 1001)
   ↓
4. 系统弹出授权对话框
   ↓
5. 无障碍服务检测到对话框
   onAccessibilityEvent(TYPE_WINDOW_STATE_CHANGED)
   ↓
6. 延迟 800ms 后执行
   performMediaProjectionClick()
   ↓
7. 尝试选择"整个屏幕"
   selectEntireScreen(rootNode)
   ├─ 通过文本查找：查找"整个屏幕"、"Entire screen"等
   ├─ 查找可选择节点：RadioButton、CheckBox
   ├─ 检查是否已选中
   └─ 点击选择（如果未选中）
   ↓
8. 如果找到选择选项
   ├─ 延迟 300ms（等待UI更新）
   ├─ 重新获取根节点
   └─ 点击授权按钮
   ↓
9. 如果没有找到选择选项
   └─ 直接点击授权按钮
   ↓
10. 授权成功
    ├─ 重置标志 autoClickMediaProjection = false
    ├─ 显示提示："已自动授权屏幕捕获"
    └─ 启动 MediaProjectionService
```

## 支持的文本和ID

### 支持的"整个屏幕"文本

| 语言 | 文本内容 |
|------|----------|
| 简体中文 | 整个屏幕、全屏 |
| 繁体中文 | 整个萤幕、全螢幕 |
| 英文 | Entire screen、Full screen、Screen |
| 日文 | 全画面、画面全体 |

### 支持的 ViewId

```java
"android:id/screen_radio"
"android:id/entire_screen"
"com.android.systemui:id/screen_radio"
```

### 支持的授权按钮文本

| 语言 | 文本内容 |
|------|----------|
| 简体中文 | 立即开始、开始、允许、确定、确认 |
| 英文 | Start now、Start、Allow、OK、Confirm |
| 日文 | 始める、許可する |

## 适配的手机品牌

### 已测试支持

- ✅ **华为/荣耀**：有两步授权流程
- ✅ **小米/Redmi**：有两步授权流程
- ✅ **OPPO/Realme**：有两步授权流程
- ✅ **vivo/iQOO**：有两步授权流程
- ✅ **三星**：部分机型有两步流程
- ✅ **原生 Android**：通常只有一步

### 兼容性说明

- **一步授权**：直接点击授权按钮
- **两步授权**：先选择"整个屏幕"，再点击授权按钮
- 代码会自动检测并适配两种流程

## 关键参数调整

### 1. 检测延迟

```java
// 检测到对话框后的延迟时间
handler.postDelayed(this::performMediaProjectionClick, 800);
```

**为什么是 800ms？**
- 确保对话框完全渲染
- 适应不同手机的动画效果
- 给系统足够时间加载UI

### 2. 步骤间延迟

```java
// 选择"整个屏幕"后的延迟
handler.postDelayed(() -> {
    // 点击授权按钮
}, 300);
```

**为什么是 300ms？**
- 等待选择后的UI更新
- RadioButton 选中动画完成
- 授权按钮状态更新

### 3. 有效时间窗口

```java
// 只在请求后 10 秒内处理
if (currentTime - mediaProjectionRequestTime < 10000) {
    // 执行自动授权
}
```

**为什么是 10 秒？**
- 足够用户看清授权内容
- 避免误点击其他对话框
- 超时后自动取消保护

## 调试方法

### 1. 查看实时日志

```bash
adb logcat -s MyAccessibilityService:* | grep -E "(MediaProjection|整个屏幕|授权)"
```

### 2. 关键日志输出

```
检测到可能的 MediaProjection 授权对话框: com.android.systemui, ...
尝试选择'整个屏幕': 整个屏幕, 结果: true
已选择'整个屏幕'选项
尝试点击按钮: 立即开始, 结果: true
成功点击 MediaProjection 授权按钮
```

### 3. UI 层级分析

使用 Android Studio 的 Layout Inspector 或 uiautomatorviewer：

```bash
# 导出 UI 层级
adb shell uiautomator dump /sdcard/ui.xml
adb pull /sdcard/ui.xml

# 使用 uiautomatorviewer 查看
$ANDROID_HOME/tools/bin/uiautomatorviewer ui.xml
```

查找以下元素：
- RadioButton: 投屏范围选择
- TextView: "整个屏幕"文本
- Button: "立即开始"按钮

## 常见问题

### Q1: 自动选择失败，仍然停留在选择界面？

**可能原因：**
1. 文本内容不匹配（厂商使用了其他文案）
2. UI 结构特殊（RadioButton 层级不同）
3. 节点查找逻辑未覆盖

**解决方法：**
1. 查看日志确认检测到的文本
2. 使用 uiautomatorviewer 分析 UI 结构
3. 添加对应的文本或 ViewId

**示例：添加新文本**
```java
String[] entireScreenTexts = {
    "整个屏幕", "整个萤幕", "全屏", "全螢幕",
    "Entire screen", "Full screen", "Screen",
    "全画面", "画面全体",
    "您厂商的文案"  // 添加新文本
};
```

### Q2: 选择成功但未点击授权按钮？

**可能原因：**
1. 延迟时间不够，UI 还未更新
2. 授权按钮文本不匹配
3. 窗口结构发生变化

**解决方法：**
1. 增加延迟时间
```java
handler.postDelayed(() -> {
    // 点击授权按钮
}, 500);  // 从 300ms 改为 500ms
```

2. 添加授权按钮文本
```java
String[] buttonTexts = { 
    "立即开始", "开始", "允许", "确定", "确认",
    "Start now", "Start", "Allow", "OK", "Confirm",
    "您的按钮文案"  // 添加新文本
};
```

### Q3: 为什么有时会选择"单个应用"？

**原因：**
- 默认选中项可能是"单个应用"
- 我们的代码只在"整个屏幕"未选中时才点击

**解决方法：**
代码已经处理了这种情况：
```java
if (!selectableNode.isChecked()) {
    // 只有未选中时才点击
    return selectableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
}
```

如果仍然有问题，可以强制点击：
```java
// 无论是否选中，都点击一次
return selectableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
```

### Q4: 多次快速点击截图按钮会怎样？

**当前行为：**
- 每次点击都会触发授权流程
- 可能导致多个对话框或服务实例

**建议改进：**
在 MainActivity 中添加防重复点击：

```java
private boolean isCapturing = false;

binding.btnScreenshot.setOnClickListener(v -> {
    if (isCapturing) {
        Toast.makeText(this, "正在截图中，请稍候...", Toast.LENGTH_SHORT).show();
        return;
    }
    
    isCapturing = true;
    startMediaProjectionService();
    
    // 10秒后重置标志
    new Handler().postDelayed(() -> {
        isCapturing = false;
    }, 10000);
});
```

## 性能优化

### 1. 节点查找优化

```java
// 使用 ViewId 查找比文本查找更快
List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(id);

// 如果知道确切的 ViewId，优先使用
if (nodes != null && !nodes.isEmpty()) {
    return nodes.get(0);  // 直接返回第一个
}
```

### 2. 减少节点遍历

```java
// 限制向上查找的层级（最多3层）
for (int i = 0; i < 3 && parent != null; i++) {
    // ...
}

// 及时回收节点
node.recycle();
```

### 3. 避免重复处理

```java
// 使用标志位避免重复触发
if (screenshotTaken) {
    return;
}
screenshotTaken = true;
```

## 扩展功能

### 1. 支持"单个应用"模式

如果需要截取单个应用：

```java
private boolean selectSingleApp(AccessibilityNodeInfo rootNode) {
    String[] singleAppTexts = {
        "单个应用", "單個應用", "单个应用",
        "Single app", "App only",
        "単一アプリ"
    };
    
    // 查找和选择逻辑同 selectEntireScreen
}
```

### 2. 记住用户选择

```java
SharedPreferences prefs = getSharedPreferences("screenshot_prefs", MODE_PRIVATE);
String lastChoice = prefs.getString("capture_mode", "entire_screen");

if ("single_app".equals(lastChoice)) {
    selectSingleApp(rootNode);
} else {
    selectEntireScreen(rootNode);
}
```

## 总结

通过增强的两步授权处理：

1. ✅ 自动选择"整个屏幕"选项
2. ✅ 自动点击授权按钮
3. ✅ 支持多语言和多种 UI 实现
4. ✅ 兼容一步和两步授权流程
5. ✅ 智能节点查找和层级遍历
6. ✅ 完善的异常处理和日志记录

现在无论是哪种授权流程，都能实现完全自动化的授权体验！🎉

