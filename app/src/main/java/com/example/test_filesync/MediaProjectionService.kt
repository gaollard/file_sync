package com.example.test_filesync

//协程和线程调度相关

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.os.Handler
import android.os.Looper

// 必须添加的导入语句
import android.graphics.Bitmap
import android.media.Image
import android.media.Image.Plane

//屏幕录制
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.view.WindowManager

// 核心回调定义
import android.hardware.display.VirtualDisplay.Callback
// 系统服务

import android.hardware.display.VirtualDisplay
import java.util.concurrent.Executors

import java.net.HttpURLConnection
import java.net.URL
import java.io.DataOutputStream

import android.graphics.BitmapFactory
import android.os.HandlerThread
import java.io.ByteArrayOutputStream
import java.io.IOException

import java.nio.ByteBuffer;
import android.provider.MediaStore
import android.content.ContentValues
import android.net.Uri
import android.os.Environment



class MediaProjectionService : Service() {
    private val handler = Handler(Looper.getMainLooper()) //通过Handler实现周期性任务触发通知 绑定主线程消息循环，确保UI操作安全
    private val handlerThread = HandlerThread("NetworkThread").apply { start() }
    private val networkHandler = Handler(handlerThread.looper)

    private lateinit var mProjection: MediaProjection
    private lateinit var mImageReader: ImageReader



    //用于在虚拟显示层通过帧间隔获取指定真的内容
    private var frameCounter = 0
    private val targetFrames = 60 * 1800 // 60fps × 半小时一次,但是只有在画面有变动的时候才是最短半小时,所以除去晚上睡觉时间,一天大概10-20张截图左右(10小时手机时间)
    //必须放在这里
    // MediaProjection回调（处理投影生命周期）
    private val mediaProjCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            //Toast.makeText(this@MediaProjectionService, "当用户主动停止屏幕捕获（如关闭录屏/投屏）", Toast.LENGTH_LONG).show()
        }
    }
    // VirtualDisplay回调（处理显示状态）
    private val virtualDisplayCallback = object : Callback() {
        override fun onPaused() {
            //Toast.makeText(this@MediaProjectionService, "虚拟显示器内容暂停渲染（如应用退到后台）", Toast.LENGTH_LONG).show()
        }
        override fun onResumed() {
            //Toast.makeText(this@MediaProjectionService, "虚拟显示器恢复渲染（如应用回到前台）", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate() {
        try {

            startForegroundService() //创建通知渠道并启动服务
        } catch (e: Exception) {
            Toast.makeText(this, "onCreate 错误：\n${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //创建指定分辨率（width×height）和像素格式（RGBA_8888）的图像读取器，缓冲区数量为 2
        try {
            intent?.extras?.let {
                //Log.d("IntentDebug", it.toString())
            } ?: run {
                Toast.makeText(this, "Intent为空", Toast.LENGTH_LONG).show()
                return START_STICKY
            }

            intent?.run {
                val resultCode = getIntExtra("resultCode", -1)
                val data = getParcelableExtra<Intent>("data")
                if (resultCode == -1 && data != null) {
                    val projection = (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
                        .getMediaProjection(resultCode, data)
                    // 开始屏幕捕获逻辑...
                    //现代的调用方式,和 val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager 这个传统的调用方式功能是一样的
                    val windowMetrics = getSystemService(WindowManager::class.java).currentWindowMetrics
                    val width = windowMetrics.bounds.width()
                    val height = windowMetrics.bounds.height()
                    val densityDpi = resources.displayMetrics.densityDpi
                    val density = resources.displayMetrics.density
                    val dpi = densityDpi
                    //虚拟显示器必须使用 PixelFormat.RGBA_8888 格式的原始数据
                    mImageReader = ImageReader.newInstance(
                        width, height,
                        PixelFormat.RGBA_8888, 2
                    ).also {
                        if (it?.surface?.isValid != true) {
                            stopSelf() // 主动停止服务避免崩溃
                            Toast.makeText(this@MediaProjectionService, "Surface初始化失败", Toast.LENGTH_LONG).show()
                        }else{
                            //width 1224 height 2700
                            //Toast.makeText(this@MediaProjectionService, "width $width height $height", Toast.LENGTH_LONG).show()
                        }
                    }
                    //创建虚拟显示层
                    //创建回调
                    projection.registerCallback(
                        mediaProjCallback,
                        Handler(Looper.getMainLooper())
                    )
                    projection.createVirtualDisplay(
                        "ScreenCapture",
                        width, height, dpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mImageReader.surface,
                        virtualDisplayCallback,
                         null
                    ).also {
                        if (it == null){
                            Toast.makeText(this@MediaProjectionService, "VirtualDisplay创建失败", Toast.LENGTH_LONG).show()
                        }else{
                            //width 1224 height 2700
                            //Toast.makeText(this@MediaProjectionService, "VirtualDisplay width $width height $height", Toast.LENGTH_LONG).show()
                        }
                    }
                    //创建监听器 - 实际测试,这个回调并不是按照帧率进行触发,如果屏幕画面没有变化则不会进行触发...

                    mImageReader.setOnImageAvailableListener({ reader ->
                        try {
                            //放到if语句之外,用于消费缓冲区的图片防止因为缓冲区满导致停止触发回调
                            val image = reader.acquireLatestImage()
                            if (image != null) {
                                // if (++frameCounter >= targetFrames) {
                                    frameCounter = 0
                                    // 处理图像数据
                                    val buffer = image?.planes?.firstOrNull()?.buffer
                                    val width = image.width
                                    val height = image.height
                                    val pixelStride = image?.planes?.firstOrNull()?.pixelStride //单个像素占用的字节数（RGBA_8888固定为4字节）
                                    val rowStride = image?.planes?.firstOrNull()?.rowStride //每行实际字节数（含可能的内存对齐填充）

                                    if(buffer != null && pixelStride != null && rowStride != null){
                                        val byteArray = ByteArray(buffer.remaining())
                                        buffer.get(byteArray) // 将数据从buffer复制到byteArray
                                        // 交换字节顺序：RGBA -> ARGB
                                        //buffer.rewind()  // 重置指针
                                        val newBuffer = ByteBuffer.wrap(byteArray)
                                        val bitmap = Bitmap.createBitmap(1224, 2700, Bitmap.Config.ARGB_8888)
                                        // 多于了 bitmap.copyPixelsFromBuffer(newBuffer) // 直接复用原始Buffer,将 newBuffer 的原始像素数据复制到 bitmap
                                        // 带stride处理的像素转换
                                        for (y in 0 until height) {
                                            for (x in 0 until width) {
                                                val offset = y * rowStride + x * pixelStride
                                                if (offset + 3 >= buffer.limit()) break

                                                val r = buffer.get(offset).toInt() and 0xFF
                                                val g = buffer.get(offset + 1).toInt() and 0xFF
                                                val b = buffer.get(offset + 2).toInt() and 0xFF
                                                val a = buffer.get(offset + 3).toInt() and 0xFF
                                                bitmap.setPixel(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
                                            }
                                        }
                                        if(bitmap != null) {

                                            // 打印日志，并且把日志保存到文件
                                            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                                .format(Date())
                                            LogUtils.i(
                                                this@MediaProjectionService,
                                                "截图成功",
                                                "截图已生成 - 时间: $timestamp, 尺寸: ${bitmap.width}x${bitmap.height}, 帧计数: $frameCounter"
                                            )

                                            // 推送通知截图成功
                                            showScreenshotSuccessNotification()

                                            // 把图片写入相册实现
                                            try {
                                                saveImageToGallery(bitmap)
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    this@MediaProjectionService,
                                                    "保存图片到相册失败: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            
                                            val outputStream = ByteArrayOutputStream()
                                            val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream) // JPEG格式，质量80%
                                            outputStream.close()
                                            bitmap.recycle()

                                            //上传图片
                                            try {
                                                Toast.makeText(
                                                    this@MediaProjectionService,
                                                    "获取到了图片数据,开始进行上传逻辑",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                //专用的后台线程处理网络请求
                                                networkHandler.post {
                                                    uploadBitmap(outputStream.toByteArray() ?: ByteArray(0))
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    this@MediaProjectionService,
                                                    "uploadBitmap方法异常 ${e.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                // }

                                image?.close() //防止没有释放
                            }
                        }catch(e: Exception){
                            Toast.makeText(this@MediaProjectionService, "监听器错误：\n${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        } finally {
                        }
                    }, Handler(Looper.getMainLooper()))
                    //Toast.makeText(this@MediaProjectionService, "创建监听器 OK", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "onStartCommand 错误：\n${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
        return START_STICKY
    }

    //保存图片到相册
    private fun saveImageToGallery(bitmap: Bitmap): Uri? {



        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "screenshot_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(it, contentValues, null, null)
                }
                
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        this@MediaProjectionService,
                        "图片已保存到相册",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return it
            } catch (e: Exception) {
                contentResolver.delete(it, null, null)
                throw e
            }
        }
        return null
    }

    //上传图片
    private val executor = Executors.newSingleThreadExecutor() //线程池 创建的线程池只包含1个工作线程
    private fun uploadBitmap(bytes: ByteArray){
        //创建了一个线程的线程池, executor.execute 中的任务会按顺序执行, 前一个异常不会导致有一个任务终止，所有execute提交的任务都在这个唯一线程中执行。
        //任务队列遵循先进先出原则，前一个任务的run()方法完全执行完毕后，才会开始执行下一个任务。
        //该工作线程不会销毁，而是持续存活等待新任务（适合长期后台任务场景）
        //更多: newFixedThreadPool(2)：并行执行2个任务 newCachedThreadPool()：动态扩容线程数 newScheduledThreadPool()：支持延迟/定时任务
            if (bytes == null || bytes.isEmpty()) {
                Toast.makeText(this@MediaProjectionService, "无效的图片数据", Toast.LENGTH_SHORT).show()
                return
            }
            try {
                val byteArray = bytes
                // 3. 上传配置
                val boundary = "Boundary-${System.currentTimeMillis()}"
                val lineEnd = "\r\n"
                val twoHyphens = "--"
                val fileName = "screenshot_${System.currentTimeMillis()}.jpg"

                // 4. 执行上传文件
                val connection =
                    URL("https://footprint.codevtool.com/updata_file.php").openConnection() as HttpURLConnection
                connection.apply {
                    //请求头设置
                    requestMethod = "POST"
                    doOutput = true
                    doInput = true
                    setRequestProperty("Connection", "Keep-Alive")
                    setRequestProperty("Cache-Control", "no-cache")
                    setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=$boundary"
                    )
                    //请求体构建
                    DataOutputStream(outputStream).apply {
                        // 添加文件部分
                        writeBytes("$twoHyphens$boundary$lineEnd")
                        writeBytes(
                            "Content-Disposition: form-data; " +
                                    "name=\"file\"; filename=\"$fileName\"$lineEnd"
                        )
                        writeBytes("Content-Type: file/*$lineEnd$lineEnd")

                        // 写入文件数据
                        write(byteArray)

                        writeBytes(lineEnd)
                        writeBytes("$twoHyphens$boundary$twoHyphens$lineEnd")
                        flush()
                    }
                }
                //响应处理 获取HTTP响应状态码 断开连接 返回上传是否成功(状态码200-299表示成功)
                val responseCode = connection.responseCode
                connection.disconnect()

                val response =
                    connection.inputStream.bufferedReader().use { it.readText() } //服务器返回的内容

                Toast.makeText(this@MediaProjectionService, "我的足迹手机端: 完成了一次截图 $response", Toast.LENGTH_LONG)
                    .show()
            } catch (e: Exception) {
                val errorDetail = when {
                    e is NullPointerException -> "空指针异常: ${e.stackTraceToString()}"
                    e is IOException -> "网络错误: ${e.message ?: "无详细信息"}"
                    e is IllegalStateException -> "状态异常: ${e.message}"
                    else -> "未知异常: ${e.javaClass.simpleName}"
                }
                Toast.makeText(this@MediaProjectionService, "我的足迹手机端: 上传异常 ${errorDetail}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    //创建通知渠道并启动服务
    private fun startForegroundService() {
        try {
            //val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
            //创建通知通道
            val channel = NotificationChannel(
                "Clipboard_channel",
                "截屏服务", //<--通知类型会展示这四个字
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null) // 禁用通知音
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC //锁屏可见
                enableVibration(false) // 禁用震动
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
            //启动服务并发送通知, 启动前台服务，避免被系统回收
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    2,
                    once_Notification("截屏服务服务运行中"),
                    FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(2, once_Notification("截屏服务服务运行中"))
            }
            Toast.makeText(this, "通知渠道创建成功,截屏服务服务已启动", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "错误：${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
    //启动服务需要的一次性服务
    private fun once_Notification(text: String): Notification {
        return NotificationCompat.Builder(this, "Clipboard_channel")
            .setContentTitle("截屏服务") //<--停止的标题,但是会被下面的覆盖
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_location)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    //使用兼容库NotificationCompat构建通知，确保旧版本兼容性
    private fun buildNotification(text: String){
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
        val notification = NotificationCompat.Builder(this, "Clipboard_channel")
            .setContentTitle("剪切板复制的内容") //<--停止的标题,但是会被下面的覆盖
            .setContentText("$text\n更新时间: $currentTime")
            .setSmallIcon(R.mipmap.ic_location)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(10001, notification)
    }

    //显示截图成功通知
    private fun showScreenshotSuccessNotification() {
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
        val notification = NotificationCompat.Builder(this, "Clipboard_channel")
            .setContentTitle("截图成功")
            .setContentText("截图已保存并上传\n时间: $currentTime")
            .setSmallIcon(R.mipmap.ic_location)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // 点击后自动取消
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(10003, notification) // 使用不同的通知ID避免覆盖其他通知
    }

    override fun onDestroy() {
        super.onDestroy()
        mImageReader?.close()
    }

    override fun onBind(intent: Intent?) = null
}