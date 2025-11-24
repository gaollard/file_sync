package com.example.test_filesync.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.test_filesync.R;
import com.example.test_filesync.util.LogUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AppLogFragment extends Fragment {
    private static final String TAG = "AppLogFragment";

    private TextView appLogTextView;
    private Button btnBack;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_log, container, false);
        appLogTextView = view.findViewById(R.id.app_log_text_view);
        btnBack = view.findViewById(R.id.btn_back);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 设置返回按钮点击事件
        btnBack.setOnClickListener(v -> {
            // 返回到上一个Fragment
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
        
        loadLogContent();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次Fragment可见时重新加载日志，以显示最新内容
        loadLogContent();
    }

    private void loadLogContent() {
        if (appLogTextView == null) {
            return;
        }

        Context context = requireContext();
        new Thread(() -> {
            try {
                File logFile = LogUtils.getLogFileForRead(context);
                String logContent = readLogFile(logFile);
                
                // 在主线程更新UI
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (appLogTextView != null) {
                            appLogTextView.setText(logContent);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "读取日志文件失败", e);
                String errorMessage = "读取日志文件失败: " + e.getMessage();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (appLogTextView != null) {
                            appLogTextView.setText(errorMessage);
                        }
                    });
                }
            }
        }).start();
    }

    private String readLogFile(File logFile) {
        if (logFile == null || !logFile.exists()) {
            return "日志文件不存在";
        }

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "读取日志文件时发生IO错误", e);
            return "读取日志文件时发生错误: " + e.getMessage();
        }

        if (content.length() == 0) {
            return "日志文件为空";
        }

        return content.toString();
    }
}
