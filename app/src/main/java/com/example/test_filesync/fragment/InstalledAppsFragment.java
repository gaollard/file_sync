package com.example.test_filesync.fragment;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
import com.example.test_filesync.databinding.FragmentInstalledAppsBinding;

import java.util.ArrayList;
import java.util.Collections;
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

        // 设置 RecyclerView
        adapter = new InstalledAppsAdapter(appList, requireContext(), this::onKillAppClick);
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
                    binding.appCountText.setText("共 " + apps.size() + " 个应用");

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

        List<PackageInfo> packages = pm.getInstalledPackages(0);

        for (PackageInfo packageInfo : packages) {
            try {
                String appName = packageInfo.applicationInfo.loadLabel(pm).toString();
                String packageName = packageInfo.packageName;
                String versionName = packageInfo.versionName != null ? packageInfo.versionName : "未知";
                long versionCode = packageInfo.getLongVersionCode();
                Drawable icon = packageInfo.applicationInfo.loadIcon(pm);
                boolean isSystemApp = (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

                apps.add(new AppItem(appName, packageName, versionName, versionCode, icon, isSystemApp));
            } catch (Exception e) {
                // 忽略无法获取信息的应用
            }
        }

        // 按应用名称排序
        Collections.sort(apps, (a, b) -> a.appName.compareToIgnoreCase(b.appName));

        return apps;
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

        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(item.packageName);
                Toast.makeText(context, "已尝试关闭: " + item.appName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "关闭失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        AppItem(String appName, String packageName, String versionName, long versionCode,
                Drawable icon, boolean isSystemApp) {
            this.appName = appName;
            this.packageName = packageName;
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.icon = icon;
            this.isSystemApp = isSystemApp;
        }
    }

    // RecyclerView Adapter
    static class InstalledAppsAdapter extends RecyclerView.Adapter<InstalledAppsAdapter.ViewHolder> {
        private List<AppItem> appList;
        private Context context;
        private OnKillAppClickListener killListener;

        interface OnKillAppClickListener {
            void onKillClick(AppItem item);
        }

        InstalledAppsAdapter(List<AppItem> appList, Context context, OnKillAppClickListener killListener) {
            this.appList = appList;
            this.context = context;
            this.killListener = killListener;
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
            Button btnKillApp;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                appIcon = itemView.findViewById(R.id.app_icon);
                appName = itemView.findViewById(R.id.app_name);
                appPackage = itemView.findViewById(R.id.app_package);
                appVersion = itemView.findViewById(R.id.app_version);
                appType = itemView.findViewById(R.id.app_type);
                btnKillApp = itemView.findViewById(R.id.btn_kill_app);
            }
        }
    }
}
