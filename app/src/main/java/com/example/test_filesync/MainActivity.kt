package com.example.test_filesync

import android.Manifest
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.test_filesync.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

import android.media.projection.MediaProjectionManager

class MainActivity : AppCompatActivity() {
    private lateinit var clipboard: ClipboardManager


    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    //创建应用
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // <-- 这句是用于创建界面的
        setSupportActionBar(binding.toolbar)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }

        // 打印日志实现
        LogUtils.i(
            this,
            "MainActivity",
            "应用已启动，执行onCreate方法"
        )

        //检查权限
        checkPermissions()
        //开启第二个服务
        //屏幕录制涉及跨应用数据捕获，属于最高级别的敏感权限（PROTECTION_FLAG_APPOP），需要用户显式交互确认
        //相比存储权限等普通危险权限，系统会优先处理这类特殊权限的授权流程。
        //所以会先弹出 屏幕录制 的权限选项
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), 1001)


    }

    //检查权限
    private fun checkPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED, // Android 14+必需 部分照片权限
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION, //定位前台服务权限
            Manifest.permission.ACCESS_COARSE_LOCATION,  // 粗略定位
            Manifest.permission.ACCESS_FINE_LOCATION,   // 精确定位
            //Manifest.permission.SCHEDULE_EXACT_ALARM, //定时器
            //Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.FOREGROUND_SERVICE, // 适配Android 9+ 通用前台服务权限
            // 剪切板权限为普通权限,不需要动态申请
            Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION //截图权限
        )
        if (requiredPermissions.all { checkSelfPermission(it) == PERMISSION_GRANTED }) {
            try {
                startLocationService()
            } catch (e: Exception) {
                Toast.makeText(this, "服务启动失败", Toast.LENGTH_SHORT).show()
            }
            finish() // 关闭Activity，保留后台服务
        } else {
            //权限不足,请求权限
            requestPermissions(requiredPermissions, 100)
        }
    }

    //执行 requestPermissions 弹出系统权限请求对话框,设置完之后在回调中进行处理
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.all { it == PERMISSION_GRANTED }) {
            //如果服务启动失败,就弹出错误提示框
            try {
                startLocationService()
            } catch (e: Exception) {
                Toast.makeText(this, "服务启动失败", Toast.LENGTH_SHORT).show()
            }
            //finish()
        } else {
            // 处理权限被拒绝的情况
            // 构建被拒绝的权限列表
            val deniedPermissions = permissions
                .filterIndexed { index, _ ->
                    grantResults[index] != PackageManager.PERMISSION_GRANTED
                }
                .joinToString("\n") {
                    when (it) {
                        Manifest.permission.READ_MEDIA_IMAGES -> "读取图片权限"
                        Manifest.permission.READ_MEDIA_VIDEO -> "读取视频权限"
                        // 其他权限的友好名称映射...
                        else -> it
                    }
                }
            AlertDialog.Builder(this)
                .setTitle("权限被拒绝")
                .setMessage("以下权限未获得授权：\n$deniedPermissions")
                .setPositiveButton("去设置") { _, _ ->
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    })
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    //开启文件同步服务
    private fun startLocationService() {
        val serviceIntent = Intent(this, FileSyncService::class.java)
        // Android 8.0+必须使用此方法启动前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    // 接收授权结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            if (resultCode == RESULT_OK && data != null) {
                // 将授权结果传递给服务
                val serviceIntent = Intent(this, MediaProjectionService::class.java).apply {
                    putExtra("resultCode", resultCode)
                    putExtra("data", data)
                }
                // Android 8.0+必须使用此方法启动前台服务
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}