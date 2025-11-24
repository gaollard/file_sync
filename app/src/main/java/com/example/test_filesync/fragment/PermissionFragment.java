package com.example.test_filesync.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.test_filesync.R;

import java.util.ArrayList;
import java.util.List;

public class PermissionFragment extends Fragment {

    private RecyclerView permissionRecyclerView;
    private PermissionAdapter adapter;
    private List<PermissionItem> permissionList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_permission, container, false);
        permissionRecyclerView = view.findViewById(R.id.permission_recycler_view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化权限列表
        permissionList = getPermissionList();
        
        // 设置 RecyclerView
        adapter = new PermissionAdapter(permissionList, requireContext());
        permissionRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        permissionRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次 Fragment 可见时重新检查权限状态
        if (adapter != null && permissionList != null) {
            updatePermissionStatus();
            adapter.notifyDataSetChanged();
        }
    }

    private void updatePermissionStatus() {
        Context context = requireContext();
        for (PermissionItem item : permissionList) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                item.isGranted = ContextCompat.checkSelfPermission(context, item.permission) 
                        == PackageManager.PERMISSION_GRANTED;
            } else {
                // Android 6.0 以下，权限在安装时授予
                item.isGranted = true;
            }
        }
    }

    private List<PermissionItem> getPermissionList() {
        List<PermissionItem> list = new ArrayList<>();
        Context context = requireContext();

        // 从 MainActivity 中获取的权限列表
        String[] permissions = new String[]{
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
                Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION
        };

        // 权限名称映射
        for (String permission : permissions) {
            String name = getPermissionName(permission);
            String description = getPermissionDescription(permission);
            boolean isGranted = false;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        if (Manifest.permission.BIND_DEVICE_ADMIN.equals(permission)) {
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
        }
        return permission;
    }

    private String getPermissionDescription(String permission) {
        if (Manifest.permission.BIND_DEVICE_ADMIN.equals(permission)) {
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

        PermissionAdapter(List<PermissionItem> permissionList, Context context) {
            this.permissionList = permissionList;
            this.context = context;
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
