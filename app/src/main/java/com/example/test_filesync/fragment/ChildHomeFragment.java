package com.example.test_filesync.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.test_filesync.R;
import com.example.test_filesync.databinding.FragmentChildHomeBinding;

public class ChildHomeFragment extends Fragment {

    private FragmentChildHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChildHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 设置按钮点击监听器
        binding.btnLog.setOnClickListener(v -> {
            // 跳转到日志页面
            if (getActivity() instanceof com.example.test_filesync.MainActivity) {
                ((com.example.test_filesync.MainActivity) getActivity()).showAppLogFragment();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

