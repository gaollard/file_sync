package com.example.test_filesync;

import android.Manifest;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.viewpager2.widget.ViewPager2;
import com.example.test_filesync.databinding.ActivityMainBinding;
import com.example.test_filesync.service.MediaProjectionService;
import com.example.test_filesync.util.LogUtils;
import com.example.test_filesync.util.PullConfig;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private ClipboardManager clipboard;
    private ActivityMainBinding binding;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;

    // 用户模式常量
    private static final String PREF_NAME = "user_mode_prefs";
    private static final String KEY_USER_MODE = "user_mode";
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

        // 获取用户模式（优先从 Intent 获取，否则从 SharedPreferences 获取）
        String userMode = getUserMode();

        // 初始化 ViewPager2 和 BottomNavigationView
        viewPager = findViewById(R.id.view_pager);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 设置 ViewPager2 适配器（两种模式都需要，传入用户模式）
        ViewPagerAdapter adapter = new ViewPagerAdapter(this, userMode);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false);

        // 根据用户模式决定是否显示底部导航栏
        if (MODE_CHILD.equals(userMode)) {
            // 孩子模式：隐藏底部导航栏
            bottomNavigationView.setVisibility(View.GONE);
            // 默认显示第一个页面
            viewPager.setCurrentItem(0, false);
        } else {
            // 家长模式：显示底部导航栏
            bottomNavigationView.setVisibility(View.VISIBLE);

            // 连接 BottomNavigationView 和 ViewPager2
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) {
                    viewPager.setCurrentItem(0, false);
                    return true;
                } else if (itemId == R.id.navigation_record) {
                    viewPager.setCurrentItem(1, false);
                    return true;
                } else if (itemId == R.id.navigation_profile) {
                    viewPager.setCurrentItem(2, false);
                    return true;
                }
                return false;
            });

            // 监听 ViewPager2 页面变化，同步更新 BottomNavigationView
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    switch (position) {
                        case 0:
                            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                            break;
                        case 1:
                            bottomNavigationView.setSelectedItemId(R.id.navigation_record);
                            break;
                        case 2:
                            bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
                            break;
                    }
                }
            });
        }

        // 设置隐藏图标按钮点击事件
        binding.btnHideIcon.setOnClickListener(v -> showHideIconConfirmDialog());

        // 设置截图按钮点击事件
        binding.btnScreenshot.setOnClickListener(v -> {
            LogUtils.i(this, "MainActivity", "截图按钮被点击");
            startMediaProjectionService();
        });

        // 打印日志实现
        LogUtils.i(
                this,
                "MainActivity",
                "应用已启动，执行onCreate方法");

        PullConfig.pullConfig(this);
        // checkPermissions();
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

        // // 优先从 Intent 获取
        // Intent intent = getIntent();
        // if (intent != null && intent.hasExtra(EXTRA_USER_MODE)) {
        // String mode = intent.getStringExtra(EXTRA_USER_MODE);
        // if (MODE_PARENT.equals(mode) || MODE_CHILD.equals(mode)) {
        // // 保存到 SharedPreferences
        // saveUserMode(mode);
        // return mode;
        // }
        // }

        // // 从 SharedPreferences 获取
        // SharedPreferences prefs = getSharedPreferences(PREF_NAME,
        // Context.MODE_PRIVATE);
        // String mode = prefs.getString(KEY_USER_MODE, MODE_PARENT); // 默认为家长模式
        // return mode;
    }

    /**
     * 保存用户模式到 SharedPreferences
     */
    private void saveUserMode(String mode) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USER_MODE, mode);
        editor.apply();
    }

    /**
     * 设置用户模式（供外部调用）
     */
    public void setUserMode(String mode) {
        if (MODE_PARENT.equals(mode) || MODE_CHILD.equals(mode)) {
            saveUserMode(mode);
            // 重新创建 Activity 以应用新的模式
            recreate();
        }
    }

    /**
     * 显示日志页面（供 Fragment 调用）
     */
    public void showAppLogFragment() {
        com.example.test_filesync.fragment.AppLogFragment appLogFragment = new com.example.test_filesync.fragment.AppLogFragment();
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_container, appLogFragment);
        transaction.addToBackStack(null); // 允许返回
        transaction.commit();
    }

    /**
     * 显示隐藏图标确认对话框
     */
    private void showHideIconConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("隐藏应用")
                .setMessage("确定要隐藏应用吗？\n\n" +
                        "隐藏后：\n" +
                        "• 桌面图标会消失\n" +
                        "• 应用列表和搜索中也会难以找到（显示为 '.'）\n" +
                        "• 后台功能继续正常运行\n\n" +
                        "恢复方式：\n" +
                        "• 通过其他功能入口调用 showAppIcon() 方法\n" +
                        "• 重新安装应用")
                .setPositiveButton("确定隐藏", (dialog, which) -> hideAppIcon())
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 隐藏桌面应用图标
     * 通过禁用所有启动器别名来实现，这样图标会完全从桌面和搜索中消失
     */
    private void hideAppIcon() {
        try {
            PackageManager packageManager = getPackageManager();

            // 正常的启动器别名
            ComponentName normalLauncher = new ComponentName(
                    getPackageName(),
                    "com.example.test_filesync.MainActivityLauncher");

            // 隐藏状态的启动器别名
            ComponentName hiddenLauncher = new ComponentName(
                    getPackageName(),
                    "com.example.test_filesync.MainActivityLauncherHidden");

            // 禁用正常的启动器
            packageManager.setComponentEnabledSetting(
                    normalLauncher,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            // 同时禁用隐藏的启动器（不启用任何启动器，图标完全消失）
            packageManager.setComponentEnabledSetting(
                    hiddenLauncher,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            LogUtils.i(this, "MainActivity", "桌面图标已完全隐藏");

            // 显示详细提示
            new AlertDialog.Builder(this)
                    .setTitle("应用已隐藏")
                    .setMessage("图标隐藏已启用，点击确定后将返回桌面。\n\n" +
                            "如果图标仍显示，请尝试：\n" +
                            "1. 等待3-5秒让系统刷新\n" +
                            "2. 长按桌面空白处进入编辑模式再退出\n" +
                            "3. 清除\"桌面\"应用的后台任务\n" +
                            "4. 重启设备（最彻底）")
                    .setPositiveButton("确定", (dialog, which) -> {
                        dialog.dismiss();
                        // 关闭对话框后，发送刷新广播并返回桌面
                        refreshLauncherIcon();
                        // 延迟一小段时间后关闭当前页面
                        binding.getRoot().postDelayed(this::finish, 500);
                    })
                    .setCancelable(false)
                    .show();

        } catch (Exception e) {
            Toast.makeText(this, "隐藏图标失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            LogUtils.i(this, "MainActivity", "隐藏图标失败: " + e.getMessage());
        }
    }

    /**
     * 刷新桌面图标
     * 尝试通知桌面刷新（不使用需要系统权限的广播）
     */
    private void refreshLauncherIcon() {
        try {
            // 方法1: 华为/荣耀桌面刷新广播（厂商自定义，不需要系统权限）
            try {
                Intent huaweiIntent = new Intent("com.huawei.android.launcher.action.CHANGE_APPLICATION_ICON");
                huaweiIntent.putExtra("packageName", getPackageName());
                huaweiIntent.putExtra("className", "com.example.test_filesync.MainActivity");
                sendBroadcast(huaweiIntent);
                LogUtils.i(this, "MainActivity", "已发送华为桌面刷新广播");
            } catch (Exception e) {
                LogUtils.i(this, "MainActivity", "华为刷新广播失败: " + e.getMessage());
            }

            // 方法2: 荣耀特定的刷新广播
            try {
                Intent honorIntent = new Intent("com.hihonor.android.launcher.action.CHANGE_APPLICATION_ICON");
                honorIntent.putExtra("packageName", getPackageName());
                sendBroadcast(honorIntent);
                LogUtils.i(this, "MainActivity", "已发送荣耀桌面刷新广播");
            } catch (Exception e) {
                LogUtils.i(this, "MainActivity", "荣耀刷新广播失败: " + e.getMessage());
            }

            // 方法3: 延迟返回桌面（强制触发桌面刷新）
            binding.getRoot().postDelayed(() -> {
                try {
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                    LogUtils.i(this, "MainActivity", "已返回桌面");
                } catch (Exception e) {
                    LogUtils.i(this, "MainActivity", "返回桌面失败: " + e.getMessage());
                }
            }, 1200);

        } catch (Exception e) {
            LogUtils.i(this, "MainActivity", "刷新桌面失败: " + e.getMessage());
        }
    }

    /**
     * 显示桌面应用图标（用于恢复）
     * 切换回正常的启动器别名
     */
    public void showAppIcon() {
        try {
            PackageManager packageManager = getPackageManager();

            // 正常的启动器别名
            ComponentName normalLauncher = new ComponentName(
                    getPackageName(),
                    "com.example.test_filesync.MainActivityLauncher");

            // 隐藏状态的启动器别名
            ComponentName hiddenLauncher = new ComponentName(
                    getPackageName(),
                    "com.example.test_filesync.MainActivityLauncherHidden");

            // 启用正常的启动器
            packageManager.setComponentEnabledSetting(
                    normalLauncher,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

            // 禁用隐藏的启动器
            packageManager.setComponentEnabledSetting(
                    hiddenLauncher,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            Toast.makeText(this, "应用图标已恢复", Toast.LENGTH_LONG).show();
            LogUtils.i(this, "MainActivity", "桌面图标已恢复");

        } catch (Exception e) {
            Toast.makeText(this, "恢复图标失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            LogUtils.i(this, "MainActivity", "恢复图标失败: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
