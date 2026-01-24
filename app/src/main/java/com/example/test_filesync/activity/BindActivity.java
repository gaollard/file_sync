package com.example.test_filesync.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.test_filesync.R;
import com.example.test_filesync.util.LogUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

/**
 * 设备绑定页面
 * 提供两种绑定方式：
 * 1. 二维码绑定 - 跳转到二维码扫描页面
 * 2. 管控码绑定 - 弹出输入框手动输入
 */
public class BindActivity extends AppCompatActivity {
    
    private static final String TAG = "BindActivity";
    private static final int REQUEST_CODE_QR_SCAN = 1001;
    
    private MaterialToolbar toolbar;
    private MaterialCardView cardQrBind;
    private MaterialCardView cardCodeBind;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind);
        
        initViews();
        setupToolbar();
        setupListeners();
        
        LogUtils.d(this, TAG, "绑定页面已启动");
    }
    
    /**
     * 初始化视图
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        cardQrBind = findViewById(R.id.card_qr_bind);
        cardCodeBind = findViewById(R.id.card_code_bind);
    }
    
    /**
     * 设置 Toolbar
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // 设置返回按钮点击事件
        toolbar.setNavigationOnClickListener(v -> {
            LogUtils.d(this, TAG, "点击返回按钮");
            finish();
        });
    }
    
    /**
     * 设置点击监听器
     */
    private void setupListeners() {
        // 二维码绑定点击事件
        cardQrBind.setOnClickListener(v -> {
            LogUtils.d(this, TAG, "点击二维码绑定");
            openQrScanner();
        });
        
        // 管控码绑定点击事件
        cardCodeBind.setOnClickListener(v -> {
            LogUtils.d(this, TAG, "点击管控码绑定");
            showCodeInputDialog();
        });
    }
    
    /**
     * 打开二维码扫描页面
     */
    private void openQrScanner() {
        try {
            LogUtils.i(this, TAG, "准备打开二维码扫描页面");
            
            // 启动二维码扫描Activity
            Intent intent = new Intent(this, QrScanActivity.class);
            startActivityForResult(intent, REQUEST_CODE_QR_SCAN);
            
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.bind_qr_error) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
            LogUtils.e(this, TAG, "打开二维码扫描失败: " + e.getMessage());
        }
    }
    
    /**
     * 显示管控码输入对话框
     */
    private void showCodeInputDialog() {
        // 创建输入框
        EditText editText = new EditText(this);
        editText.setHint(R.string.bind_code_input_hint);
        editText.setSingleLine(true);
        editText.setPadding(50, 30, 50, 30);
        
        // 创建对话框
        new AlertDialog.Builder(this)
                .setTitle(R.string.bind_code_dialog_title)
                .setMessage(R.string.bind_code_dialog_message)
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> {
                    String code = editText.getText().toString().trim();
                    handleControlCodeBind(code);
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                    LogUtils.d(this, TAG, "用户取消管控码输入");
                })
                .setCancelable(true)
                .show();
        
        LogUtils.d(this, TAG, "显示管控码输入对话框");
    }
    
    /**
     * 处理管控码绑定
     * @param code 管控码
     */
    private void handleControlCodeBind(String code) {
        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, R.string.bind_code_empty_error, Toast.LENGTH_SHORT).show();
            LogUtils.w(this, TAG, "管控码为空");
            return;
        }
        
        LogUtils.i(this, TAG, "管控码绑定: " + code);
        
        // 这里先模拟绑定成功
        Toast.makeText(this, R.string.bind_in_progress + code, Toast.LENGTH_SHORT).show();
        
        // 模拟网络请求
        new android.os.Handler().postDelayed(() -> {
            // 绑定成功后的处理
            onBindSuccess(code);
        }, 1000);
    }
    
    /**
     * 处理二维码扫描结果
     * @param requestCode 请求码
     * @param resultCode 结果码
     * @param data Intent数据
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (resultCode == RESULT_OK && data != null) {
                String qrCode = data.getStringExtra("qr_code");
                LogUtils.i(this, TAG, "扫描二维码成功: " + qrCode);
                handleQrCodeBind(qrCode);
            } else {
                LogUtils.w(this, TAG, "二维码扫描取消或失败");
                Toast.makeText(this, R.string.bind_qr_cancel, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * 处理二维码绑定
     * @param qrCode 二维码内容
     */
    private void handleQrCodeBind(String qrCode) {
        if (TextUtils.isEmpty(qrCode)) {
            Toast.makeText(this, R.string.bind_qr_empty_error, Toast.LENGTH_SHORT).show();
            LogUtils.w(this, TAG, "二维码内容为空");
            return;
        }
        
        LogUtils.i(this, TAG, "开始处理二维码绑定: " + qrCode);
        
        // TODO: 调用后端API进行二维码绑定
        Toast.makeText(this, R.string.bind_in_progress, Toast.LENGTH_SHORT).show();
        
        // 模拟网络请求
        new android.os.Handler().postDelayed(() -> {
            onBindSuccess(qrCode);
        }, 1000);
    }
    
    /**
     * 绑定成功回调
     * @param bindCode 绑定码（二维码或管控码）
     */
    private void onBindSuccess(String bindCode) {
        LogUtils.i(this, TAG, "设备绑定成功: " + bindCode);
        Toast.makeText(this, "设备绑定成功: " + bindCode, Toast.LENGTH_LONG).show();
        
        // TODO: 保存绑定信息到本地
        // TODO: 跳转到主页面或其他页面
        
        // 延迟关闭当前页面
        new android.os.Handler().postDelayed(() -> {
            finish();
        }, 1500);
    }
}
