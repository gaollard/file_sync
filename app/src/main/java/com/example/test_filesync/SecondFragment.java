package com.example.test_filesync;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.test_filesync.databinding.FragmentSecondBinding;

/**
 * A simple {@link Fragment} subclass as the second destination in the navigation.
 */
public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = requireContext();
                // 显示日志文件路径，方便调试
                String logPath = LogUtils.getLogFilePath(context);
                android.util.Log.d("SecondFragment", "日志文件路径: " + logPath);

                LogUtils.i(context, "SecondFragment", "应用已启动，执行onCreate方法");
                LogUtils.i(context, "SecondFragment", "应用已启动，执行onCreate方法");
                LogUtils.i(context, "SecondFragment", "应用已启动，执行onCreate方法");
                LogUtils.i(context, "SecondFragment", "应用已启动，执行onCreate方法");
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

