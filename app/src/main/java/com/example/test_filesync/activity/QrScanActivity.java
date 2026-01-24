package com.example.test_filesync.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.test_filesync.R;
import com.example.test_filesync.util.LogUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

/**
 * 二维码扫描页面
 * 使用 ZXing 库实现二维码扫描功能
 */
public class QrScanActivity extends AppCompatActivity {

    private static final String TAG = "QrScanActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;

    private DecoratedBarcodeView barcodeScanner;
    private MaterialToolbar toolbar;
    private MaterialButton btnFlashlight;
    
    private boolean isFlashlightOn = false;
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        initViews();
        setupToolbar();
        checkCameraPermission();

        LogUtils.d(this, TAG, "二维码扫描页面已启动");
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        barcodeScanner = findViewById(R.id.barcode_scanner);
        btnFlashlight = findViewById(R.id.btn_flashlight);

        // 设置扫描回调
        barcodeScanner.decodeContinuous(callback);

        // 设置闪光灯按钮点击事件
        btnFlashlight.setOnClickListener(v -> toggleFlashlight());
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
     * 检查相机权限
     */
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // 请求相机权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
            LogUtils.d(this, TAG, "请求相机权限");
        } else {
            // 已有权限，开始扫描
            startScanning();
        }
    }

    /**
     * 权限请求结果回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LogUtils.i(this, TAG, "相机权限已授予");
                startScanning();
            } else {
                LogUtils.w(this, TAG, "相机权限被拒绝");
                Toast.makeText(this, "需要相机权限才能扫描二维码", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * 开始扫描
     */
    private void startScanning() {
        if (!isScanning) {
            barcodeScanner.resume();
            isScanning = true;
            LogUtils.d(this, TAG, "开始扫描二维码");
        }
    }

    /**
     * 停止扫描
     */
    private void stopScanning() {
        if (isScanning) {
            barcodeScanner.pause();
            isScanning = false;
            LogUtils.d(this, TAG, "停止扫描二维码");
        }
    }

    /**
     * 切换闪光灯
     */
    private void toggleFlashlight() {
        if (isFlashlightOn) {
            barcodeScanner.setTorchOff();
            btnFlashlight.setText("打开闪光灯");
            isFlashlightOn = false;
            LogUtils.d(this, TAG, "关闭闪光灯");
        } else {
            barcodeScanner.setTorchOn();
            btnFlashlight.setText("关闭闪光灯");
            isFlashlightOn = true;
            LogUtils.d(this, TAG, "打开闪光灯");
        }
    }

    /**
     * 扫描结果回调
     */
    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result != null && result.getText() != null) {
                handleScanResult(result.getText());
            }
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            // 可能的结果点，用于显示扫描轨迹
        }
    };

    /**
     * 处理扫描结果
     * @param qrCode 二维码内容
     */
    private void handleScanResult(String qrCode) {
        LogUtils.i(this, TAG, "扫描成功: " + qrCode);
        
        // 停止扫描
        stopScanning();
        
        // 震动提示（可选）
        // Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // if (vibrator != null) {
        //     vibrator.vibrate(200);
        // }

        // 返回结果
        Intent resultIntent = new Intent();
        resultIntent.putExtra("qr_code", qrCode);
        setResult(RESULT_OK, resultIntent);
        
        // 延迟关闭，让用户看到扫描成功的效果
        barcodeScanner.postDelayed(() -> finish(), 300);
    }

    /**
     * 处理返回键
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeScanner.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScanning();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭闪光灯
        if (isFlashlightOn) {
            barcodeScanner.setTorchOff();
        }
        LogUtils.d(this, TAG, "二维码扫描页面已销毁");
    }
}

