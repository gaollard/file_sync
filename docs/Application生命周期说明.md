# Application 生命周期说明

## 🔄 为什么关闭 APP 后 Application.onCreate() 不会重新执行？

### 核心原因
`Application.onCreate()` 只在 **应用进程创建时** 调用一次，而不是每次打开应用时都调用。

---

## 📱 Android 应用进程生命周期

### 1️⃣ **进程创建**
当应用首次启动或进程被系统杀死后重新启动时：
```
系统创建新进程 
→ Application.onCreate() 被调用
→ Activity.onCreate() 被调用
→ Activity.onStart() 被调用
→ Activity.onResume() 被调用（应用可见）
```

### 2️⃣ **按返回键退出**
```
Activity.onPause() 
→ Activity.onStop()
→ Activity.onDestroy()
→ 进程可能继续运行（在后台）
```
**注意**：`Application` 对象仍然存在，`onCreate()` 不会再次调用！

### 3️⃣ **按 Home 键切换到后台**
```
Activity.onPause()
→ Activity.onStop()
→ 进程在后台继续运行
```
**注意**：Activity 没有被销毁，只是不可见。

### 4️⃣ **从后台返回前台**
```
Activity.onRestart()（如果 Activity 未销毁）
→ Activity.onStart()
→ Activity.onResume()
```
**注意**：`Application.onCreate()` 不会被调用！

### 5️⃣ **进程被系统杀死**
当系统内存不足时，可能会杀死后台进程：
```
进程被杀死
→ 下次启动时，进程重新创建
→ Application.onCreate() 再次被调用
```

---

## ✅ 如何检测应用是否进入前台？

### 方案 1：使用 Activity 生命周期（推荐）

在 `MainActivity` 中重写 `onResume()` 方法：

```java
@Override
protected void onResume() {
    super.onResume();
    
    // 每次应用进入前台都会执行
    LogUtils.i(this, "MainActivity", "应用进入前台");
    
    // 在这里执行需要重复执行的初始化逻辑
    PullConfig.pullConfig(this);  // 拉取最新配置
    refreshUserInfo();            // 刷新用户信息
}
```

### 方案 2：使用 ActivityLifecycleCallbacks（全局监听）

在 `StudentApplication` 中注册全局监听：

```java
public class StudentApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 注册全局 Activity 生命周期监听
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private int activityReferences = 0;
            private boolean isActivityChangingConfigurations = false;

            @Override
            public void onActivityStarted(Activity activity) {
                if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                    // 应用进入前台
                    LogUtils.i(activity, "App", "应用进入前台");
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations();
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    // 应用进入后台
                    LogUtils.i(activity, "App", "应用进入后台");
                }
            }

            // 其他回调方法...
            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }
}
```

---

## 🎯 最佳实践建议

### ✅ 应该在 Application.onCreate() 中执行的操作
- **一次性初始化**：数据库、日志系统、推送 SDK 等
- **全局配置**：网络框架、图片加载库等
- **进程级别的设置**

```java
@Override
public void onCreate() {
    super.onCreate();
    
    // ✅ 一次性初始化
    initDatabase();
    initJPush();
    initHonorPush();
    LocationClient.setAgreePrivacy(true);
}
```

### ✅ 应该在 Activity.onResume() 中执行的操作
- **需要每次返回前台都执行的操作**
- **刷新 UI 数据**
- **拉取最新配置**
- **恢复前台服务**

```java
@Override
protected void onResume() {
    super.onResume();
    
    // ✅ 每次进入前台都执行
    PullConfig.pullConfig(this);  // 拉取最新配置
    refreshData();                // 刷新数据
    updateUI();                   // 更新 UI
}
```

---

## 🔍 如何验证进程是否被重启？

### 方法 1：查看日志中的进程 ID
```
首次启动：进程 ID: 12345
返回前台：进程 ID: 12345  ← 相同，说明进程未重启
系统杀死后：进程 ID: 67890  ← 不同，说明进程重启了
```

### 方法 2：在代码中记录
```java
@Override
public void onCreate() {
    super.onCreate();
    
    long processStartTime = System.currentTimeMillis();
    int processId = android.os.Process.myPid();
    
    LogUtils.i(this, "App", "进程启动时间: " + processStartTime);
    LogUtils.i(this, "App", "进程 ID: " + processId);
}
```

---

## 🐛 常见误区

### ❌ 错误做法：依赖 Application.onCreate() 更新数据
```java
// ❌ 错误：关闭应用后再打开，这段代码不会执行
@Override
public void onCreate() {
    super.onCreate();
    PullConfig.pullConfig(this);  // 只在进程创建时执行一次
}
```

### ✅ 正确做法：在 Activity 生命周期中更新
```java
// ✅ 正确：每次进入前台都会执行
@Override
protected void onResume() {
    super.onResume();
    PullConfig.pullConfig(this);  // 每次进入前台都会执行
}
```

---

## 📝 总结

| 场景 | Application.onCreate() | Activity.onResume() |
|------|------------------------|---------------------|
| 首次启动应用 | ✅ 调用 | ✅ 调用 |
| 按返回键退出后再打开 | ❌ 不调用（如果进程未被杀死） | ✅ 调用 |
| 按 Home 键后再返回 | ❌ 不调用 | ✅ 调用 |
| 进程被系统杀死后重启 | ✅ 调用 | ✅ 调用 |

**结论**：
- `Application.onCreate()` 用于 **一次性初始化**
- `Activity.onResume()` 用于 **每次进入前台的操作**

---

## 🔧 在您的项目中的应用

您已经在 `MainActivity` 中添加了 `onResume()` 方法：

```java
@Override
protected void onResume() {
    super.onResume();
    
    // 每次应用进入前台都会执行
    PullConfig.pullConfig(this);  // 拉取最新配置
}
```

现在，无论您如何关闭和打开应用，只要 `MainActivity` 进入前台，都会自动拉取最新配置。

---

## 🧪 测试方法

1. **打开应用**，查看日志，记录进程 ID
2. **按返回键**退出应用
3. **重新打开应用**，查看日志
   - 如果进程 ID 相同，说明进程未被杀死，`Application.onCreate()` 不会执行
   - 但 `MainActivity.onResume()` 会执行
4. **从最近任务中滑掉应用**（强制停止）
5. **重新打开应用**，查看日志
   - 进程 ID 不同，说明进程被重启
   - `Application.onCreate()` 和 `MainActivity.onResume()` 都会执行

