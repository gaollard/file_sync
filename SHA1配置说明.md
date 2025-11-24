# SHA1 签名配置说明

## 什么是 SHA1？

SHA1 是 Android 应用的数字签名指纹，用于验证应用的身份。百度定位服务需要 SHA1 来验证 API Key 的合法性。

## 方法一：通过代码自动获取（推荐）

应用启动后，在日志中查找以下信息：
```
========== 百度定位配置信息 ==========
包名 (Package Name): com.example.test_filesync
SHA1 签名: XXXXXXXXXXXXXXXX...
=====================================
```

## 方法二：使用 Gradle 任务获取（最简单）

在项目根目录的 Terminal 中运行：
```bash
./gradlew signingReport
```

或者在 Android Studio 中：
1. 打开右侧边栏的 **Gradle**
2. 展开项目 → `app` → `Tasks` → `android`
3. 双击 `signingReport`
4. 在底部输出窗口查看 SHA1 值（在 "SHA1:" 后面）

这会显示所有构建变体（Debug、Release）的 SHA1 和 SHA256。

## 方法三：使用命令行获取

### 获取 Debug 版本的 SHA1

**macOS/Linux:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Windows:**
```cmd
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### 获取 Release 版本的 SHA1

如果你有自定义的签名文件：
```bash
keytool -list -v -keystore <你的keystore路径> -alias <你的alias名称>
```

### 从已安装的 APK 获取 SHA1

```bash
# 1. 解压 APK
unzip app-debug.apk

# 2. 获取签名
keytool -printcert -file META-INF/CERT.RSA
```

## 方法四：使用 Android Studio GUI

1. 打开 Android Studio
2. 点击右侧边栏的 **Gradle**
3. 展开项目 → `app` → `Tasks` → `android`
4. 双击 `signingReport`
5. 在底部输出窗口查看 SHA1 值

## 在百度开发者控制台配置 SHA1

1. 登录 [百度开发者控制台](https://lbsyun.baidu.com/)
2. 进入 **控制台** → **应用管理** → 选择你的应用
3. 找到 **安全码设置** 或 **SHA1 配置**
4. 添加 SHA1 值（注意：Debug 和 Release 版本需要分别配置）
5. 保存配置

## 重要提示

1. **Debug 和 Release 使用不同的 SHA1**
   - Debug 版本使用默认的 debug.keystore
   - Release 版本使用你配置的签名文件
   - 两个 SHA1 都需要在百度控制台配置

2. **包名必须完全匹配**
   - 当前包名：`com.example.test_filesync`
   - 百度控制台中的包名必须与此完全一致

3. **配置后需要等待生效**
   - 配置保存后，可能需要几分钟才能生效
   - 如果仍然报错，请检查 SHA1 是否正确复制（注意大小写）

4. **常见错误**
   - `TypeServerCheckKeyError`：SHA1 未配置或配置错误
   - 确保 SHA1 值没有多余的空格或换行符
   - 确保使用的是正确的 SHA1（Debug/Release）

## 验证配置

配置完成后，重新运行应用，查看日志：
- 如果不再出现 `TypeServerCheckKeyError`，说明配置成功
- 如果仍然报错，请检查：
  1. SHA1 是否正确
  2. 包名是否匹配
  3. API Key 是否正确
  4. 定位服务是否已启用

