package com.example.test_filesync

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.test_filesync.databinding.FragmentSecondBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            val context = requireContext()
            // 显示日志文件路径，方便调试
            val logPath = LogUtils.getLogFilePath(context)
            android.util.Log.d("SecondFragment", "日志文件路径: $logPath")
            
            LogUtils.i(
                context,
                "SecondFragment",
                "应用已启动，执行onCreate方法"
            )
            LogUtils.i(
                context,
                "SecondFragment",
                "应用已启动，执行onCreate方法"
            )
            LogUtils.i(
                context,
                "SecondFragment",
                "应用已启动，执行onCreate方法"
            )
            LogUtils.i(
                context,
                "SecondFragment",
                "应用已启动，执行onCreate方法"
            )
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}