package com.example.test_filesync.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.test_filesync.R;
import com.example.test_filesync.service.FloatingWindowService;
import com.example.test_filesync.service.MyAccessibilityService;
import com.example.test_filesync.util.DeviceAdminHelper;
import com.example.test_filesync.util.FloatingWindowHelper;
import com.example.test_filesync.util.LogUtils;

import java.util.ArrayList;
import java.util.List;

public class PermissionFragment extends Fragment {

    private RecyclerView permissionRecyclerView;
    private PermissionAdapter adapter;
    private List<PermissionItem> permissionList;
    private Button screenshotButton;
    private Button floatingWindowButton;
    private static final String TAG = "PermissionFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_permission, container, false);
        permissionRecyclerView = view.findViewById(R.id.permission_recycler_view);
        screenshotButton = view.findViewById(R.id.screenshot_button);
        floatingWindowButton = view.findViewById(R.id.floating_window_button);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化权限列表
        permissionList = getPermissionList();

        // 设置 RecyclerView
        adapter = new PermissionAdapter(permissionList, requireContext(), this::onPermissionItemClick);
        permissionRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        permissionRecyclerView.setAdapter(adapter);

        // 设置截图按钮点击事件
        if (screenshotButton != null) {
            screenshotButton.setOnClickListener(v -> onScreenshotButtonClick());
        }

        // 设置悬浮窗按钮点击事件
        if (floatingWindowButton != null) {
            floatingWindowButton.setOnClickListener(v -> onFloatingWindowButtonClick());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次 Fragment 可见时重新检查权限状态
        if (adapter != null && permissionList != null) {
            updatePermissionStatus();
            adapter.notifyDataSetChanged();
        }

        // 更新悬浮窗按钮文本
        if (floatingWindowButton != null) {
            if (FloatingWindowHelper.isShowing()) {
                floatingWindowButton.setText("隐藏悬浮窗");
            } else {
                floatingWindowButton.setText("显示悬浮窗");
            }
        }
    }

    private void updatePermissionStatus() {
        Context context = requireContext();
        for (PermissionItem item : permissionList) {
            if (Manifest.permission.BIND_DEVICE_ADMIN.equals(item.permission)) {
                // 设备管理员权限需要特殊检查
                item.isGranted = DeviceAdminHelper.isDeviceAdminActive(context);
            } else if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(item.permission)) {
                // 悬浮窗权限需要特殊检查
                item.isGranted = FloatingWindowHelper.hasOverlayPermission(context);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                item.isGranted = ContextCompat.checkSelfPermission(context, item.permission)
                        == PackageManager.PERMISSION_GRANTED;
            } else {
                // Android 6.0 以下，权限在安装时授予
                item.isGranted = true;
            }
        }
    }

    /**
     * 处理悬浮窗按钮点击事件
     */
    private void onFloatingWindowButtonClick() {
        Context context = requireContext();

        // 检查悬浮窗权限
        if (!FloatingWindowHelper.hasOverlayPermission(context)) {
            Toast.makeText(context, "请先授予悬浮窗权限", Toast.LENGTH_LONG).show();
            // 跳转到悬浮窗权限设置页面
            if (getActivity() != null) {
                FloatingWindowHelper.requestOverlayPermission(getActivity(), 111);
            }
            return;
        }

        // 切换悬浮窗显示状态
        if (FloatingWindowHelper.isShowing()) {
            // 隐藏悬浮窗
//            FloatingWindowHelper.hideFloatingWindow(context);
//            FloatingWindowService.stopService(context);
            floatingWindowButton.setText("显示悬浮窗");
            Toast.makeText(context, "悬浮窗已隐藏", Toast.LENGTH_SHORT).show();
            LogUtils.i(context, TAG, "悬浮窗已隐藏");
        } else {
            // 显示悬浮窗
            boolean success = FloatingWindowHelper.showFloatingWindow(context);
            if (success) {
                // 启动服务以保持悬浮窗显示
                FloatingWindowService.startService(context, true);
                floatingWindowButton.setText("隐藏悬浮窗");
                Toast.makeText(context, "悬浮窗已显示", Toast.LENGTH_SHORT).show();
                LogUtils.i(context, TAG, "悬浮窗已显示");
            } else {
                Toast.makeText(context, "显示悬浮窗失败", Toast.LENGTH_SHORT).show();
                LogUtils.e(context, TAG, "显示悬浮窗失败");
            }
        }
    }

    /**
     * 处理截图按钮点击事件
     */
    private void onScreenshotButtonClick() {
        Context context = requireContext();

        // 检查无障碍服务是否启用
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(context, "请先启用无障碍服务权限", Toast.LENGTH_LONG).show();
            // 跳转到无障碍服务设置页面
            openAccessibilitySettings();
            return;
        }

        // 检查服务实例是否可用
        MyAccessibilityService service = MyAccessibilityService.getInstance();
        if (service == null) {
            Toast.makeText(context, "无障碍服务未运行，请重新启用", Toast.LENGTH_LONG).show();
            openAccessibilitySettings();
            return;
        }

        // 调用无障碍服务进行截图
        try {
            service.triggerScreenshot();
            Toast.makeText(context, "正在使用无障碍服务模拟截图", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "截图失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 检查无障碍服务是否启用
     */
    private boolean isAccessibilityServiceEnabled() {
        Context context = requireContext();
        String serviceName = context.getPackageName() + "/" + MyAccessibilityService.class.getName();

        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );

            if (accessibilityEnabled == 1) {
                String settingValue = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                );

                if (settingValue != null) {
                    String[] enabledServices = settingValue.split(":");
                    for (String enabledService : enabledServices) {
                        if (enabledService.equalsIgnoreCase(serviceName)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            // 设置项不存在，返回 false
        }

        return false;
    }

    /**
     * 打开无障碍服务设置页面
     */
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    /**
     * 处理权限项点击事件
     */
    private void onPermissionItemClick(PermissionItem item) {
        Context context = requireContext();

        // 如果权限已授予，提示用户
        if (item.isGranted) {
            Toast.makeText(context, item.name + "已授予", Toast.LENGTH_SHORT).show();
            return;
        }

        // 根据权限类型处理
        if (Manifest.permission.BIND_ACCESSIBILITY_SERVICE.equals(item.permission)) {
            // 无障碍服务权限
            if (getActivity() != null) {
                // 申请无障碍服务权限
                requestPermissions(new String[]{Manifest.permission.BIND_ACCESSIBILITY_SERVICE}, getRequestCode(item.permission));
            }
        } else if (Manifest.permission.BIND_DEVICE_ADMIN.equals(item.permission)) {
            // 设备管理员权限
            if (getActivity() != null) {
                DeviceAdminHelper.activateDeviceAdmin(getActivity(), getRequestCode(item.permission));
            }
        } else if (Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION.equals(item.permission)) {
            // 媒体投影权限需要特殊处理，跳转到设置页面
            openAppSettings();
        } else if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(item.permission)) {
            // 悬浮窗权限需要特殊处理，跳转到悬浮窗权限设置页面
            if (getActivity() != null) {
                FloatingWindowHelper.requestOverlayPermission(getActivity(), getRequestCode(item.permission));
            }
        } else {
            // 普通权限，使用requestPermissions请求
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{item.permission}, getRequestCode(item.permission));
            } else {
                Toast.makeText(context, "该权限在Android 6.0以下自动授予", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 打开应用设置页面
     */
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    /**
     * 根据权限获取请求码
     */
    private int getRequestCode(String permission) {
        // 为不同权限分配不同的请求码，便于在回调中区分
        if (Manifest.permission.BIND_ACCESSIBILITY_SERVICE.equals(permission)) {
            return 201;
        } else if (Manifest.permission.BIND_DEVICE_ADMIN.equals(permission)) {
            return 200;
        } else if (Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION.equals(permission)) {
            return 202;
        } else if (Manifest.permission.READ_MEDIA_IMAGES.equals(permission)) {
            return 101;
        } else if (Manifest.permission.READ_MEDIA_VIDEO.equals(permission)) {
            return 102;
        } else if (Manifest.permission.READ_MEDIA_AUDIO.equals(permission)) {
            return 103;
        } else if (Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED.equals(permission)) {
            return 104;
        } else if (Manifest.permission.POST_NOTIFICATIONS.equals(permission)) {
            return 105;
        } else if (Manifest.permission.FOREGROUND_SERVICE_LOCATION.equals(permission)) {
            return 106;
        } else if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission)) {
            return 107;
        } else if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)) {
            return 108;
        } else if (Manifest.permission.ACCESS_WIFI_STATE.equals(permission)) {
            return 109;
        } else if (Manifest.permission.FOREGROUND_SERVICE.equals(permission)) {
            return 110;
        } else if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
            return 111;
        }
        return 100; // 默认请求码
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "权限已授予", Toast.LENGTH_SHORT).show();
            // 更新权限状态
            updatePermissionStatus();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        } else {
            Toast.makeText(requireContext(), "权限被拒绝，请在设置中手动开启", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200) {
            // 设备管理员权限请求结果
            if (resultCode == android.app.Activity.RESULT_OK) {
                Toast.makeText(requireContext(), "设备管理员已激活", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "设备管理员激活被取消", Toast.LENGTH_SHORT).show();
            }
            // 更新权限状态
            updatePermissionStatus();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        } else if (requestCode == 111) {
            // 悬浮窗权限请求结果
            if (FloatingWindowHelper.hasOverlayPermission(requireContext())) {
                Toast.makeText(requireContext(), "悬浮窗权限已授予", Toast.LENGTH_SHORT).show();
                LogUtils.i(requireContext(), TAG, "悬浮窗权限已授予");
            } else {
                Toast.makeText(requireContext(), "悬浮窗权限被拒绝", Toast.LENGTH_SHORT).show();
                LogUtils.e(requireContext(), TAG, "悬浮窗权限被拒绝");
            }
            // 更新权限状态
            updatePermissionStatus();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private List<PermissionItem> getPermissionList() {
        List<PermissionItem> list = new ArrayList<>();
        Context context = requireContext();

        // 从 MainActivity 中获取的权限列表
        String[] permissions = new String[]{
            Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
            Manifest.permission.BIND_DEVICE_ADMIN,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION,
                Manifest.permission.SYSTEM_ALERT_WINDOW
        };

        // 权限名称映射
        for (String permission : permissions) {
            String name = getPermissionName(permission);
            String description = getPermissionDescription(permission);
            boolean isGranted = false;

            if (Manifest.permission.BIND_DEVICE_ADMIN.equals(permission)) {
                // 设备管理员权限需要特殊检查
                isGranted = DeviceAdminHelper.isDeviceAdminActive(context);
            } else if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
                // 悬浮窗权限需要特殊检查
                isGranted = FloatingWindowHelper.hasOverlayPermission(context);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isGranted = ContextCompat.checkSelfPermission(context, permission)
                        == PackageManager.PERMISSION_GRANTED;
            } else {
                isGranted = true;
            }

            list.add(new PermissionItem(permission, name, description, isGranted));
        }

        return list;
    }

    private String getPermissionName(String permission) {
        if (Manifest.permission.BIND_ACCESSIBILITY_SERVICE.equals(permission)) {
            return "无障碍服务权限";
        } else if (Manifest.permission.BIND_DEVICE_ADMIN.equals(permission)) {
            return "设备管理员权限";
        } else if (Manifest.permission.READ_MEDIA_IMAGES.equals(permission)) {
            return "读取图片权限";
        } else if (Manifest.permission.READ_MEDIA_VIDEO.equals(permission)) {
            return "读取视频权限";
        } else if (Manifest.permission.READ_MEDIA_AUDIO.equals(permission)) {
            return "读取音频权限";
        } else if (Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED.equals(permission)) {
            return "选择性媒体访问权限";
        } else if (Manifest.permission.POST_NOTIFICATIONS.equals(permission)) {
            return "通知权限";
        } else if (Manifest.permission.FOREGROUND_SERVICE_LOCATION.equals(permission)) {
            return "定位前台服务权限";
        } else if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission)) {
            return "粗略定位权限";
        } else if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)) {
            return "精确定位权限";
        } else if (Manifest.permission.ACCESS_WIFI_STATE.equals(permission)) {
            return "获取WiFi状态权限";
        } else if (Manifest.permission.FOREGROUND_SERVICE.equals(permission)) {
            return "前台服务权限";
        } else if (Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION.equals(permission)) {
            return "媒体投影服务权限";
        } else if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
            return "悬浮窗权限";
        }
        return permission;
    }

    private String getPermissionDescription(String permission) {
        if (Manifest.permission.BIND_ACCESSIBILITY_SERVICE.equals(permission)) {
            return "允许应用无障碍服务";
        } else if (Manifest.permission.BIND_DEVICE_ADMIN.equals(permission)) {
            return "允许应用成为设备管理员";
        } else if (Manifest.permission.READ_MEDIA_IMAGES.equals(permission)) {
            return "允许应用访问设备上的图片文件";
        } else if (Manifest.permission.READ_MEDIA_VIDEO.equals(permission)) {
            return "允许应用访问设备上的视频文件";
        } else if (Manifest.permission.READ_MEDIA_AUDIO.equals(permission)) {
            return "允许应用访问设备上的音频文件";
        } else if (Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED.equals(permission)) {
            return "允许应用访问用户选择的媒体文件（Android 14+）";
        } else if (Manifest.permission.POST_NOTIFICATIONS.equals(permission)) {
            return "允许应用发送通知";
        } else if (Manifest.permission.FOREGROUND_SERVICE_LOCATION.equals(permission)) {
            return "允许应用在后台获取位置信息";
        } else if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission)) {
            return "允许应用获取大致位置信息（基于网络）";
        } else if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)) {
            return "允许应用获取精确位置信息（基于GPS）";
        } else if (Manifest.permission.ACCESS_WIFI_STATE.equals(permission)) {
            return "允许应用获取WiFi连接状态";
        } else if (Manifest.permission.FOREGROUND_SERVICE.equals(permission)) {
            return "允许应用运行前台服务";
        } else if (Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION.equals(permission)) {
            return "允许应用进行屏幕录制或截图";
        } else if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
            return "允许应用在其他应用上层显示悬浮窗";
        }
        return "未知权限";
    }

    // 权限项数据类
    static class PermissionItem {
        String permission;
        String name;
        String description;
        boolean isGranted;

        PermissionItem(String permission, String name, String description, boolean isGranted) {
            this.permission = permission;
            this.name = name;
            this.description = description;
            this.isGranted = isGranted;
        }
    }

    // RecyclerView Adapter
    static class PermissionAdapter extends RecyclerView.Adapter<PermissionAdapter.ViewHolder> {
        private List<PermissionItem> permissionList;
        private Context context;
        private OnPermissionItemClickListener listener;

        interface OnPermissionItemClickListener {
            void onItemClick(PermissionItem item);
        }

        PermissionAdapter(List<PermissionItem> permissionList, Context context, OnPermissionItemClickListener listener) {
            this.permissionList = permissionList;
            this.context = context;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_permission, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PermissionItem item = permissionList.get(position);
            holder.permissionNameText.setText(item.name);
            holder.permissionDescriptionText.setText(item.description);

            if (item.isGranted) {
                holder.permissionStatusText.setText("已授予");
                holder.permissionStatusText.setBackgroundColor(
                        ContextCompat.getColor(context, android.R.color.holo_green_light));
            } else {
                holder.permissionStatusText.setText("未授予");
                holder.permissionStatusText.setBackgroundColor(
                        ContextCompat.getColor(context, android.R.color.holo_red_light));
            }

            // 设置点击事件
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return permissionList != null ? permissionList.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView permissionNameText;
            TextView permissionDescriptionText;
            TextView permissionStatusText;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                permissionNameText = itemView.findViewById(R.id.permission_name_text);
                permissionDescriptionText = itemView.findViewById(R.id.permission_description_text);
                permissionStatusText = itemView.findViewById(R.id.permission_status_text);
            }
        }
    }
}
