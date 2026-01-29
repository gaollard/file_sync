package com.example.test_filesync.fragment;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.example.test_filesync.R;
import com.example.test_filesync.activity.BindActivity;
import com.example.test_filesync.databinding.FragmentHomeBinding;
import com.example.test_filesync.service.MediaProjectionService;
import com.example.test_filesync.util.LogUtils;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private static final int REQUEST_MEDIA_PROJECTION = 1001;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 设备绑定按钮点击事件
        binding.btnBind.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), BindActivity.class);
            startActivity(intent);
        });

        // 权限管理按钮点击事件
        binding.btnPermission.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_HomeFragment_to_PermissionFragment);
        });

        // 应用管理按钮点击事件（现在是LinearLayout）
        binding.btnAppList.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_HomeFragment_to_InstalledAppsFragment);
        });

        // 隐藏桌面图标按钮点击事件（现在是LinearLayout）
        binding.btnHideIcon.setOnClickListener(v -> {
            showHideIconConfirmDialog();
        });

        // 投影截图按钮点击事件（现在是LinearLayout）
        binding.btnScreenshot.setOnClickListener(v -> {
            LogUtils.i(requireContext(), "HomeFragment", "截图按钮被点击");
            // 优先使用AccessibilityService截图（Android 9+，更简单，不需要媒体投影权限）
            if (tryAccessibilityScreenshot()) {
                LogUtils.i(requireContext(), "HomeFragment", "使用AccessibilityService截图");
            } else {
                // 降级使用MediaProjection截图（需要用户授权）
                LogUtils.i(requireContext(), "HomeFragment", "使用MediaProjection截图");
                startMediaProjectionService();
            }
        });
    }

    /**
     * 显示隐藏图标确认对话框
     */
    private void showHideIconConfirmDialog() {
        new AlertDialog.Builder(requireActivity())
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
            PackageManager packageManager = requireActivity().getPackageManager();

            // 正常的启动器别名
            ComponentName normalLauncher = new ComponentName(
                    requireActivity().getPackageName(),
                    "com.example.test_filesync.MainActivityLauncher");

            // 隐藏状态的启动器别名
            ComponentName hiddenLauncher = new ComponentName(
                    requireActivity().getPackageName(),
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

            LogUtils.i(requireActivity(), "MainActivity", "桌面图标已完全隐藏");

            // 显示详细提示
            new AlertDialog.Builder(requireActivity())
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
                        binding.getRoot().postDelayed(requireActivity()::finish, 500);
                    })
                    .setCancelable(false)
                    .show();

        } catch (Exception e) {
            Toast.makeText(requireContext(), "隐藏图标失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            LogUtils.i(requireContext(), "MainActivity", "隐藏图标失败: " + e.getMessage());
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
                huaweiIntent.putExtra("packageName", requireActivity().getPackageName());
                huaweiIntent.putExtra("className", "com.example.test_filesync.MainActivity");
                requireActivity().sendBroadcast(huaweiIntent);
                LogUtils.i(requireActivity(), "MainActivity", "已发送华为桌面刷新广播");
            } catch (Exception e) {
                LogUtils.i(requireActivity(), "MainActivity", "华为刷新广播失败: " + e.getMessage());
            }

            // 方法2: 荣耀特定的刷新广播
            try {
                Intent honorIntent = new Intent("com.hihonor.android.launcher.action.CHANGE_APPLICATION_ICON");
                honorIntent.putExtra("packageName", requireActivity().getPackageName());
                requireActivity().sendBroadcast(honorIntent);
                LogUtils.i(requireActivity(), "MainActivity", "已发送荣耀桌面刷新广播");
            } catch (Exception e) {
                LogUtils.i(requireActivity(), "MainActivity", "荣耀刷新广播失败: " + e.getMessage());
            }

            // 方法3: 延迟返回桌面（强制触发桌面刷新）
            binding.getRoot().postDelayed(() -> {
                try {
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(homeIntent);
                    LogUtils.i(requireActivity(), "MainActivity", "已返回桌面");
                } catch (Exception e) {
                    LogUtils.i(requireActivity(), "MainActivity", "返回桌面失败: " + e.getMessage());
                }
            }, 1200);

        } catch (Exception e) {
            LogUtils.i(requireActivity(), "MainActivity", "刷新桌面失败: " + e.getMessage());
        }
    }

    /**
     * 显示桌面应用图标（用于恢复）
     * 切换回正常的启动器别名
     */
    public void showAppIcon() {
        try {
            PackageManager packageManager = requireActivity().getPackageManager();

            // 正常的启动器别名
            ComponentName normalLauncher = new ComponentName(
                    requireActivity().getPackageName(),
                    "com.example.test_filesync.MainActivityLauncher");

            // 隐藏状态的启动器别名
            ComponentName hiddenLauncher = new ComponentName(
                    requireActivity().getPackageName(),
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

            Toast.makeText(requireContext(), "应用图标已恢复", Toast.LENGTH_LONG).show();
            LogUtils.i(requireContext(), "MainActivity", "桌面图标已恢复");

        } catch (Exception e) {
            Toast.makeText(requireContext(), "恢复图标失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            LogUtils.i(requireContext(), "MainActivity", "恢复图标失败: " + e.getMessage());
        }
    }

    /**
     * 尝试使用AccessibilityService进行截图
     * 
     * @return 如果成功触发返回true，否则返回false
     */
    private boolean tryAccessibilityScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // Android 9+
            if (com.example.test_filesync.service.MyAccessibilityService.isServiceEnabled()) {
                com.example.test_filesync.service.MyAccessibilityService service = com.example.test_filesync.service.MyAccessibilityService
                        .getInstance();
                if (service != null) {
                    service.triggerScreenshot();
                    return true;
                } else {
                    Toast.makeText(requireContext(), "无障碍服务未运行，请在设置中开启", Toast.LENGTH_SHORT).show();
                }
            } else {
                // 引导用户开启无障碍服务
                new AlertDialog.Builder(requireContext())
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
            Toast.makeText(requireContext(), "系统版本过低，不支持无障碍截图", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    /**
     * 启动媒体投影服务进行截图
     */
    private void startMediaProjectionService() {
        // 开启第二个服务
        // 屏幕录制涉及跨应用数据捕获，属于最高级别的敏感权限（PROTECTION_FLAG_APPOP），需要用户显式交互确认
        // 相比存储权限等普通危险权限，系统会优先处理这类特殊权限的授权流程。
        // 所以会先弹出 屏幕录制 的权限选项
        MediaProjectionManager manager = (MediaProjectionManager) requireContext()
                .getSystemService(Activity.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
    }

    /**
     * 接收屏幕录制授权结果
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        LogUtils.i(requireContext(), "HomeFragment",
                "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // 将授权结果传递给服务
                Intent serviceIntent = new Intent(requireContext(), MediaProjectionService.class);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);
                // Android 8.0+必须使用此方法启动前台服务
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireContext().startForegroundService(serviceIntent);
                } else {
                    requireContext().startService(serviceIntent);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
