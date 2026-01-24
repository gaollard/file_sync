package com.example.test_filesync;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.test_filesync.databinding.ActivityMainBinding;
import com.example.test_filesync.service.MediaProjectionService;
import com.example.test_filesync.util.LogUtils;
import com.example.test_filesync.util.PullConfig;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;
    private BottomNavigationView bottomNavigationView;
    public static final String MODE_PARENT = "parent"; // 家长模式
    public static final String MODE_CHILD = "child"; // 孩子模式
    public static final String EXTRA_USER_MODE = "extra_user_mode"; // Intent 传递用户模式的 key

    // 创建应用
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot()); // <-- 这句是用于创建界面的
        // setSupportActionBar(binding.toolbar);

        // 初始化 NavController 和 BottomNavigationView
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setVisibility(View.GONE);
        if (navController != null) {
            navController.navigate(R.id.navigation_home);
        }

        // 设置截图按钮点击事件
        binding.btnScreenshot.setOnClickListener(v -> {
            LogUtils.i(this, "MainActivity", "截图按钮被点击");
            // 优先使用AccessibilityService截图（Android 9+，更简单，不需要媒体投影权限）
            if (tryAccessibilityScreenshot()) {
                LogUtils.i(this, "MainActivity", "使用AccessibilityService截图");
            } else {
                // 降级使用MediaProjection截图（需要用户授权）
                LogUtils.i(this, "MainActivity", "使用MediaProjection截图");
                startMediaProjectionService();
            }
        });

        // 打印日志实现
        LogUtils.i(
                this,
                "MainActivity",
                "应用已启动，执行onCreate方法");

        PullConfig.pullConfig(this);
        // checkPermissions();
    }

    /**
     * 尝试使用AccessibilityService进行截图
     * @return 如果成功触发返回true，否则返回false
     */
    private boolean tryAccessibilityScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // Android 9+
            if (com.example.test_filesync.service.MyAccessibilityService.isServiceEnabled()) {
                com.example.test_filesync.service.MyAccessibilityService service =
                    com.example.test_filesync.service.MyAccessibilityService.getInstance();
                if (service != null) {
                    service.triggerScreenshot();
                    return true;
                } else {
                    Toast.makeText(this, "无障碍服务未运行，请在设置中开启", Toast.LENGTH_SHORT).show();
                }
            } else {
                // 引导用户开启无障碍服务
                new AlertDialog.Builder(this)
                    .setTitle("需要开启无障碍服务")
                    .setMessage("使用自动截图功能需要开启无障碍服务，是否前往设置？")
                    .setPositiveButton("去设置", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("取消", null)
                    .show();
            }
        } else {
            Toast.makeText(this, "系统版本过低，不支持无障碍截图", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void startMediaProjectionService() {
        // 开启第二个服务
        // 屏幕录制涉及跨应用数据捕获，属于最高级别的敏感权限（PROTECTION_FLAG_APPOP），需要用户显式交互确认
        // 相比存储权限等普通危险权限，系统会优先处理这类特殊权限的授权流程。
        // 所以会先弹出 屏幕录制 的权限选项
        MediaProjectionManager manager = (MediaProjectionManager)
        getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(), 1001);
    }

    // 检查权限
    private void checkPermissions() {
        // 根据 Android 版本动态构建权限列表
        java.util.ArrayList<String> permissionList = new java.util.ArrayList<>();
        permissionList.add(Manifest.permission.READ_MEDIA_IMAGES);
        permissionList.add(Manifest.permission.READ_MEDIA_VIDEO);
        permissionList.add(Manifest.permission.READ_MEDIA_AUDIO);

        // Android 14+ 才需要部分照片访问权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionList.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        }

        permissionList.add(Manifest.permission.POST_NOTIFICATIONS);
        permissionList.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION); // 定位前台服务权限
        permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION); // 粗略定位
        permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION); // 精确定位
        permissionList.add(Manifest.permission.ACCESS_WIFI_STATE); // 获取WIFI状态
        // permissionList.add(Manifest.permission.SCHEDULE_EXACT_ALARM); //定时器
        // permissionList.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
        permissionList.add(Manifest.permission.FOREGROUND_SERVICE); // 适配Android 9+ 通用前台服务权限
        // 剪切板权限为普通权限,不需要动态申请
        permissionList.add(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION); // 截图权限

        String[] requiredPermissions = permissionList.toArray(new String[0]);

        boolean allGranted = true;
        for (String permission : requiredPermissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            try {
                // startLocationService();
            } catch (Exception e) {
                LogUtils.i(this, "服务启动失败");
                Toast.makeText(this, "服务启动失败", Toast.LENGTH_SHORT).show();
            }
            finish(); // 关闭Activity，保留后台服务
        } else {
            // 权限不足,请求权限
            requestPermissions(requiredPermissions, 100);
        }
    }

    // 执行 requestPermissions 弹出系统权限请求对话框,设置完之后在回调中进行处理
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // 如果服务启动失败,就弹出错误提示框
                try {
                    // startLocationService();
                } catch (Exception e) {
                    Toast.makeText(this, "服务启动失败", Toast.LENGTH_SHORT).show();
                }
                // finish()
            } else {
                // 处理权限被拒绝的情况
                // 构建被拒绝的权限列表
                StringBuilder deniedPermissions = new StringBuilder();
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        if (deniedPermissions.length() > 0) {
                            deniedPermissions.append("\n");
                        }
                        String permission = permissions[i];
                        if (Manifest.permission.READ_MEDIA_IMAGES.equals(permission)) {
                            deniedPermissions.append("读取图片权限");
                        } else if (Manifest.permission.READ_MEDIA_VIDEO.equals(permission)) {
                            deniedPermissions.append("读取视频权限");
                        } else {
                            // 其他权限的友好名称映射...
                            deniedPermissions.append(permission);
                        }
                    }
                }

                new AlertDialog.Builder(this)
                        .setTitle("权限被拒绝")
                        .setMessage("以下权限未获得授权：\n" + deniedPermissions.toString())
                        .setPositiveButton("去设置", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.fromParts("package", getPackageName(), null));
                            startActivity(intent);
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        }
    }

    // 开启位置服务
    private void startLocationService() {
        // Intent serviceIntent = new Intent(this,
        // com.example.test_filesync.service.LocationService.class);
        // // Android 8.0+必须使用此方法启动前台服务
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // startForegroundService(serviceIntent);
        // } else {
        // startService(serviceIntent);
        // }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 接收屏幕录制授权结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtils.i(this, "MainActivity", "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            if (resultCode == RESULT_OK && data != null) {
                // 将授权结果传递给服务
                Intent serviceIntent = new Intent(this, MediaProjectionService.class);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);
                // Android 8.0+必须使用此方法启动前台服务
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            }
        }
    }

    /**
     * 获取用户模式
     * 优先从 Intent 获取，如果没有则从 SharedPreferences 获取，默认返回家长模式
     */
    private String getUserMode() {
        return MODE_CHILD;
    }

    /**
     * 显示日志页面（供 Fragment 调用）
     */
    public void showAppLogFragment() {
        com.example.test_filesync.fragment.AppLogFragment appLogFragment = new com.example.test_filesync.fragment.AppLogFragment();
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, appLogFragment);
        transaction.addToBackStack(null); // 允许返回
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
