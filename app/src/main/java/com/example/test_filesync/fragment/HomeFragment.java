package com.example.test_filesync.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.test_filesync.R;
import com.example.test_filesync.activity.BindActivity;
import com.example.test_filesync.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 权限管理按钮点击事件
        binding.btnPermission.setOnClickListener(v -> {

            navigateToFragment(new PermissionFragment());
        });

        // 应用管理按钮点击事件
        binding.btnAppList.setOnClickListener(v -> {
            navigateToFragment(new InstalledAppsFragment());
        });

        // 设备绑定按钮点击事件
        binding.btnBind.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), BindActivity.class);
            startActivity(intent);
        });
    }

    private void navigateToFragment(Fragment fragment) {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
