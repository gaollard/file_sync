package com.example.test_filesync.fragment;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.test_filesync.R;
import com.example.test_filesync.StudentApplication;
import com.example.test_filesync.api.ApiCallback;
import com.example.test_filesync.api.ApiConfig;
import com.example.test_filesync.api.dto.UserInfo;
import com.example.test_filesync.databinding.FragmentInstalledAppsBinding;
import com.example.test_filesync.service.MyAccessibilityService;
import com.example.test_filesync.util.HttpUtil;
import com.example.test_filesync.util.LogUtils;
import com.example.test_filesync.util.PullConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InstalledAppsFragment extends Fragment {

    private FragmentInstalledAppsBinding binding;
    private InstalledAppsAdapter adapter;
    private List<AppItem> appList = new ArrayList<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentInstalledAppsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 设置返回按钮点击事件
        binding.backButton.setOnClickListener(v -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        // 设置 RecyclerView
        adapter = new InstalledAppsAdapter(appList, requireContext(), 
                this::onKillAppClick, this::onBlacklistClick);
        binding.appsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.appsRecyclerView.setAdapter(adapter);

        // 加载已安装应用列表
        loadInstalledApps();
    }

    private void loadInstalledApps() {
        // 显示加载进度
        binding.loadingProgress.setVisibility(View.VISIBLE);
        binding.appsRecyclerView.setVisibility(View.GONE);

        executor.execute(() -> {
            List<AppItem> apps = getInstalledApps();

            mainHandler.post(() -> {
                if (binding != null) {
                    appList.clear();
                    appList.addAll(apps);
                    adapter.notifyDataSetChanged();

                    // 更新应用数量显示
                    binding.appCountText.setText("共 " + apps.size() + " 个用户应用");

                    // 隐藏加载进度
                    binding.loadingProgress.setVisibility(View.GONE);
                    binding.appsRecyclerView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private List<AppItem> getInstalledApps() {
        List<AppItem> apps = new ArrayList<>();
        Context context = requireContext();
        PackageManager pm = context.getPackageManager();

        // 获取黑名单应用包名列表
        java.util.Set<String> blacklistedPackages = getBlacklistedPackages(context);

        List<PackageInfo> packages = pm.getInstalledPackages(0);

        for (PackageInfo packageInfo : packages) {
            try {
                boolean isSystemApp = (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

                // 只显示用户应用，跳过系统应用
                if (isSystemApp) {
                    continue;
                }

                String appName = packageInfo.applicationInfo.loadLabel(pm).toString();
                String packageName = packageInfo.packageName;
                String versionName = packageInfo.versionName != null ? packageInfo.versionName : "未知";
                long versionCode = packageInfo.getLongVersionCode();
                Drawable icon = packageInfo.applicationInfo.loadIcon(pm);
                
                // 检查是否在黑名单中
                boolean isBlacklisted = blacklistedPackages.contains(packageName);

                apps.add(new AppItem(appName, packageName, versionName, versionCode, icon, isSystemApp, isBlacklisted));
            } catch (Exception e) {
                // 忽略无法获取信息的应用
            }
        }

        // 上报已安装应用列表
        reportInstalledApps(apps);

        // 按应用名称排序
        Collections.sort(apps, (a, b) -> a.appName.compareToIgnoreCase(b.appName));

        return apps;
    }

    /**
     * 获取黑名单应用包名集合
     */
    private java.util.Set<String> getBlacklistedPackages(Context context) {
        java.util.Set<String> blacklistedPackages = new java.util.HashSet<>();
        try {
            StudentApplication application = (StudentApplication) context.getApplicationContext();
            UserInfo userInfo = application.getUserInfo();
            if (userInfo != null && userInfo.getDisabledApps() != null) {
                for (UserInfo.AppItem appItem : userInfo.getDisabledApps()) {
                    if (appItem.getPackageName() != null) {
                        blacklistedPackages.add(appItem.getPackageName());
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e(context, "获取黑名单应用失败: " + e.getMessage(), e);
        }
        return blacklistedPackages;
    }

    private void reportInstalledApps(List<AppItem> apps) {
        Context context = requireContext();
        HashMap<String, Object> params = new HashMap<String, Object>();
        // "{'apps': [{'app_name': 'appName', 'package_name': 'packageName'}]}"
        ArrayList<HashMap<String, Object>> appsList = new ArrayList<HashMap<String, Object>>();
        for (AppItem app : apps) {
            appsList.add(new HashMap<String, Object>() {{
                put("appName", app.appName);
                put("packageName", app.packageName);
            }});
        }
        params.put("apps", appsList);
        HttpUtil.config(ApiConfig.report_installed_app, params).postRequest(context, new ApiCallback() {
            @Override
            public void onSuccess(String res) {
                LogUtils.i(context, "上报已安装应用列表成功：" + res);
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "上报已安装应用列表失败：" + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                LogUtils.e(context, "上报已安装应用列表失败：" + e.getLocalizedMessage(), e);
            }
        });
    }
    /**
     * 处理黑名单操作点击事件
     */
    private void onBlacklistClick(AppItem item) {
        Context context = requireContext();
        
        if (item.isBlacklisted) {
            // 从黑名单移除
            removeFromBlacklist(item, context);
        } else {
            // 添加到黑名单
            addToBlacklist(item, context);
        }
    }

    /**
     * 添加到黑名单
     */
    private void addToBlacklist(AppItem item, Context context) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("package_name", item.packageName);
        
        Toast.makeText(context, "正在添加到黑名单...", Toast.LENGTH_SHORT).show();
        
        HttpUtil.config(ApiConfig.add_to_blacklist, params).postRequest(context, new ApiCallback() {
            @Override
            public void onSuccess(String res) {
                LogUtils.i(context, "添加到黑名单成功：" + res);
                Toast.makeText(context, item.appName + " 添加到黑名单成功", Toast.LENGTH_SHORT).show();
                
                // 刷新用户信息
                PullConfig.pullConfig(context);
                
                // 延迟刷新应用列表，等待用户信息更新完成
                mainHandler.postDelayed(() -> {
                    if (isAdded() && binding != null) {
                        loadInstalledApps();
                    }
                }, 500); // 延迟500ms刷新列表
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "添加到黑名单失败：" + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                LogUtils.e(context, "添加到黑名单失败：" + e.getLocalizedMessage(), e);
            }
        });
    }

    /**
     * 从黑名单移除
     */
    private void removeFromBlacklist(AppItem item, Context context) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("package_name", item.packageName);
        
        Toast.makeText(context, "正在从黑名单移除...", Toast.LENGTH_SHORT).show();
        
        HttpUtil.config(ApiConfig.remove_from_blacklist, params).postRequest(context, new ApiCallback() {
            @Override
            public void onSuccess(String res) {
                LogUtils.i(context, "从黑名单移除成功：" + res);
                Toast.makeText(context, item.appName + " 从黑名单移除成功", Toast.LENGTH_SHORT).show();
                
                // 刷新用户信息
                PullConfig.pullConfig(context);
                
                // 延迟刷新应用列表，等待用户信息更新完成
                mainHandler.postDelayed(() -> {
                    if (isAdded() && binding != null) {
                        loadInstalledApps();
                    }
                }, 500); // 延迟500ms刷新列表
            }
            
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "从黑名单移除失败：" + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                LogUtils.e(context, "从黑名单移除失败：" + e.getLocalizedMessage(), e);
            }
        });
    }

    /**
     * 处理关闭应用点击事件
     */
    private void onKillAppClick(AppItem item) {
        Context context = requireContext();

        // 不能关闭自己
        if (item.packageName.equals(context.getPackageName())) {
            Toast.makeText(context, "不能关闭当前应用", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查无障碍服务是否启用
        boolean accessibilityEnabled = MyAccessibilityService.isServiceEnabled();

        String message = "请选择关闭方式：\n\n" +
            "• 尝试关闭：仅能关闭后台进程，效果有限\n" +
            "• 手动停止：跳转到系统设置页面手动强制停止\n" +
            "• 自动停止：使用无障碍服务自动点击强制停止" +
            (accessibilityEnabled ? "" : "（需先开启无障碍服务）");

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
            .setTitle("关闭应用: " + item.appName)
            .setMessage(message)
            .setNegativeButton("取消", null);

        // 添加三个选项按钮
        builder.setPositiveButton("自动停止", (dialog, which) -> {
            if (accessibilityEnabled) {
                autoForceStop(item);
            } else {
                // 跳转到无障碍设置页面
                openAccessibilitySettings();
            }
        });

        builder.setNeutralButton("手动停止", (dialog, which) -> {
            openAppSettings(item.packageName);
        });

        // 使用自定义方式添加第三个按钮
        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialogInterface -> {
            // 动态添加"尝试关闭"按钮
            alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                alertDialog.dismiss();
                openAppSettings(item.packageName);
            });
        });

        alertDialog.show();

        // 在对话框显示后添加额外的点击处理
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnLongClickListener(v -> {
            alertDialog.dismiss();
            tryKillBackgroundProcess(item);
            return true;
        });
    }

    /**
     * 使用无障碍服务自动强制停止应用
     */
    private void autoForceStop(AppItem item) {
        Context context = requireContext();

        // 设置目标包名和回调
        MyAccessibilityService.setForceStopTarget(item.packageName, (success, message) -> {
            mainHandler.post(() -> {
                if (isAdded() && context != null) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    // 操作完成后返回到应用
                    if (success) {
                        // 可选：自动返回到我们的应用
                        Intent intent = context.getPackageManager()
                            .getLaunchIntentForPackage(context.getPackageName());
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }
                    }
                }
            });
        });

        // 打开目标应用的设置页面
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + item.packageName));
            startActivity(intent);
            Toast.makeText(context, "正在自动执行强制停止...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            MyAccessibilityService.cancelForceStop();
            Toast.makeText(context, "无法打开应用设置: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 打开无障碍服务设置页面
     */
    private void openAccessibilitySettings() {
        Context context = requireContext();
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(context, "请找到并开启本应用的无障碍服务", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, "无法打开无障碍设置", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 尝试关闭后台进程（效果有限）
     */
    private void tryKillBackgroundProcess(AppItem item) {
        Context context = requireContext();
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(item.packageName);
                Toast.makeText(context,
                    "已尝试关闭后台进程: " + item.appName + "\n（如果应用在前台运行则无效）",
                    Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "关闭失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 打开应用详情设置页面，用户可以手动强制停止
     */
    private void openAppSettings(String packageName) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
            Toast.makeText(requireContext(), "请点击「强制停止」按钮", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "无法打开应用设置: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    // 应用数据类
    static class AppItem {
        String appName;
        String packageName;
        String versionName;
        long versionCode;
        Drawable icon;
        boolean isSystemApp;
        boolean isBlacklisted;

        AppItem(String appName, String packageName, String versionName, long versionCode,
                Drawable icon, boolean isSystemApp, boolean isBlacklisted) {
            this.appName = appName;
            this.packageName = packageName;
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.icon = icon;
            this.isSystemApp = isSystemApp;
            this.isBlacklisted = isBlacklisted;
        }
    }

    // RecyclerView Adapter
    static class InstalledAppsAdapter extends RecyclerView.Adapter<InstalledAppsAdapter.ViewHolder> {
        private List<AppItem> appList;
        private Context context;
        private OnKillAppClickListener killListener;
        private OnBlacklistClickListener blacklistListener;

        interface OnKillAppClickListener {
            void onKillClick(AppItem item);
        }

        interface OnBlacklistClickListener {
            void onBlacklistClick(AppItem item);
        }

        InstalledAppsAdapter(List<AppItem> appList, Context context, 
                OnKillAppClickListener killListener, OnBlacklistClickListener blacklistListener) {
            this.appList = appList;
            this.context = context;
            this.killListener = killListener;
            this.blacklistListener = blacklistListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_installed_app, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AppItem item = appList.get(position);

            holder.appName.setText(item.appName);
            holder.appPackage.setText(item.packageName);
            holder.appVersion.setText("版本: " + item.versionName + " (" + item.versionCode + ")");
            holder.appIcon.setImageDrawable(item.icon);

            if (item.isSystemApp) {
                holder.appType.setText("系统");
                holder.appType.setBackgroundColor(
                        ContextCompat.getColor(context, android.R.color.holo_orange_light));
            } else {
                holder.appType.setText("用户");
                holder.appType.setBackgroundColor(
                        ContextCompat.getColor(context, android.R.color.holo_blue_light));
            }

            // 显示黑名单标签
            if (item.isBlacklisted) {
                holder.appBlacklist.setVisibility(View.VISIBLE);
                holder.appBlacklist.setText("黑名单");
                holder.appBlacklist.setBackgroundColor(
                        ContextCompat.getColor(context, android.R.color.holo_red_dark));
            } else {
                holder.appBlacklist.setVisibility(View.GONE);
            }

            // 设置黑名单操作按钮
            if (item.isBlacklisted) {
                holder.btnBlacklist.setText("从黑名单移除");
                holder.btnBlacklist.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, android.R.color.holo_green_dark));
            } else {
                holder.btnBlacklist.setText("添加到黑名单");
                holder.btnBlacklist.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, android.R.color.holo_orange_dark));
            }

            // 黑名单按钮点击事件
            holder.btnBlacklist.setOnClickListener(v -> {
                if (blacklistListener != null) {
                    blacklistListener.onBlacklistClick(item);
                }
            });

            // 关闭按钮点击事件
            holder.btnKillApp.setOnClickListener(v -> {
                if (killListener != null) {
                    killListener.onKillClick(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return appList != null ? appList.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView appIcon;
            TextView appName;
            TextView appPackage;
            TextView appVersion;
            TextView appType;
            TextView appBlacklist;
            Button btnBlacklist;
            Button btnKillApp;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                appIcon = itemView.findViewById(R.id.app_icon);
                appName = itemView.findViewById(R.id.app_name);
                appPackage = itemView.findViewById(R.id.app_package);
                appVersion = itemView.findViewById(R.id.app_version);
                appType = itemView.findViewById(R.id.app_type);
                appBlacklist = itemView.findViewById(R.id.app_blacklist);
                btnBlacklist = itemView.findViewById(R.id.btn_blacklist);
                btnKillApp = itemView.findViewById(R.id.btn_kill_app);
            }
        }
    }
}
