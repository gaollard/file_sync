package com.example.test_filesync.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

/**
 * WebView JavaScript 接口类
 * 提供原生功能给网页 JavaScript 调用
 *
 * 使用方式（JavaScript端）:
 *   window.Android.showToast("消息");
 *   window.Android.getDeviceInfo();
 *   等等...
 */
public class WebAppInterface {
    private final Context context;
    private WebView webView;
    private final Handler mainHandler;

    public WebAppInterface(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public WebAppInterface(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 设置 WebView 引用（用于回调 JavaScript）
     */
    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    // ==================== 基础功能 ====================

    /**
     * 显示 Toast 消息
     * JS调用: window.Android.showToast("Hello")
     */
    @JavascriptInterface
    public void showToast(String msg) {
        mainHandler.post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }

    /**
     * 拨打电话
     * JS调用: window.Android.callPhone("10086");
     */
    @JavascriptInterface
    public void callPhone(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            showToast("无法拨打电话");
        }
    }

    // ==================== 回调 JavaScript ====================

    /**
     * 从原生回调 JavaScript 方法
     * 原生调用此方法，会执行网页中的 JavaScript 函数
     *
     * @param functionName JavaScript 函数名
     * @param args         参数（JSON 字符串）
     */
    public void callJavaScript(String functionName, String args) {
        if (webView == null) return;

        String script;
        if (args == null || args.isEmpty()) {
            script = String.format("javascript:%s()", functionName);
        } else {
            script = String.format("javascript:%s(%s)", functionName, args);
        }

        mainHandler.post(() -> webView.evaluateJavascript(script, null));
    }

    /**
     * 执行任意 JavaScript 代码
     *
     * @param jsCode JavaScript 代码
     */
    public void executeJavaScript(String jsCode) {
        if (webView == null) return;
        mainHandler.post(() -> webView.evaluateJavascript(jsCode, null));
    }
}
