package com.example.test_filesync;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.test_filesync.util.LogUtils;
import com.example.test_filesync.util.WebAppInterface;

/**
 * WebView 页面
 * 用于在应用内展示网页内容
 */
public class WebviewActivity extends AppCompatActivity {

    // Intent 传递参数的 Key
    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_TITLE = "extra_title";

    private WebView webView;
    private ProgressBar progressBar;

    // 默认加载的 URL
    private static final String DEFAULT_URL = "https://www.baidu.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        // 初始化视图
        initViews();

        // 配置 WebView
        configureWebView();

        // 获取传入的 URL
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null || url.isEmpty()) {
            url = DEFAULT_URL;
        }

        // 获取传入的标题
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title != null && !title.isEmpty() && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }


        // Java 端：调用 JS 方法
        webView.evaluateJavascript("javascript:showMessage('Hello from Android')", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String result) {
                // 处理 JS 返回值
            }
        });

        // 加载网页
        webView.loadUrl(url);
        LogUtils.i(this, "WebviewActivity", "加载网页: " + url);
    }


    /**
     * 初始化视图
     */
    private void initViews() {
        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress_bar);

        // 设置返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * 配置 WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings webSettings = webView.getSettings();

        // 启用 JavaScript
        webSettings.setJavaScriptEnabled(true);

        // 启用 DOM Storage
        webSettings.setDomStorageEnabled(true);

        // 启用数据库
        webSettings.setDatabaseEnabled(true);

        // 设置缓存模式
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 支持缩放
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // 自适应屏幕
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // 允许访问文件
        webSettings.setAllowFileAccess(true);

        // 允许混合内容（HTTP 和 HTTPS）
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // 设置 User-Agent
        String userAgent = webSettings.getUserAgentString();
        webSettings.setUserAgentString(userAgent + " TestFileSyncApp/1.0");

        // 添加 JavaScript 接口（传入 WebView 引用，支持原生回调 JS）
        webView.addJavascriptInterface(new WebAppInterface(this, webView), "Android");

        // 设置 WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String url = uri.toString();

                // 处理特殊协议（如 tel:, mailto:, intent: 等）
                if (url.startsWith("tel:") || url.startsWith("mailto:") ||
                    url.startsWith("sms:") || url.startsWith("intent:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    } catch (Exception e) {
                        LogUtils.i(WebviewActivity.this, "WebviewActivity", "无法处理该链接: " + url);
                    }
                    return true;
                }

                // 在 WebView 内部加载网页
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                // 显示进度条
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 隐藏进度条
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                // 更新标题
                if (getSupportActionBar() != null && view.getTitle() != null) {
                    getSupportActionBar().setTitle(view.getTitle());
                }
            }
        });

        // 设置 WebChromeClient（处理进度、标题、弹窗等）
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                    if (newProgress >= 100) {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (getSupportActionBar() != null && title != null) {
                    getSupportActionBar().setTitle(title);
                }
            }
        });
    }

    /**
     * 处理返回键
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 如果 WebView 可以后退，则后退
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 处理 ActionBar 返回按钮
     */
    @Override
    public boolean onSupportNavigateUp() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
        return true;
    }

    /**
     * 暂停 WebView（进入后台时）
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    /**
     * 恢复 WebView（从后台回来时）
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    /**
     * 销毁 WebView
     */
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();
            webView.clearCache(true);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    /**
     * 静态方法：启动 WebviewActivity
     *
     * @param context 上下文
     * @param url     要加载的 URL
     */
    public static void start(android.content.Context context, String url) {
        Intent intent = new Intent(context, WebviewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        context.startActivity(intent);
    }

    /**
     * 静态方法：启动 WebviewActivity（带标题）
     *
     * @param context 上下文
     * @param url     要加载的 URL
     * @param title   页面标题
     */
    public static void start(android.content.Context context, String url, String title) {
        Intent intent = new Intent(context, WebviewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_TITLE, title);
        context.startActivity(intent);
    }
}
