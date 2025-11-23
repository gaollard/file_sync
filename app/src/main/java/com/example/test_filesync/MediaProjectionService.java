package com.example.test_filesync;

//协程和线程调度相关

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.os.Handler;
import android.os.Looper;

// 必须添加的导入语句
import android.graphics.Bitmap;
import android.media.Image;
import android.media.Image.Plane;

//屏幕录制
import android.content.Context;
import android.media.projection.MediaProjectionManager;
import android.view.WindowManager;

// 核心回调定义
import android.hardware.display.VirtualDisplay;
// 系统服务

import java.util.concurrent.Executors;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.DataOutputStream;

import android.graphics.BitmapFactory;
import android.os.HandlerThread;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.nio.ByteBuffer;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Environment;
import java.util.List;

public class MediaProjectionService extends Service {
    private final Handler handler = new Handler(Looper.getMainLooper()); //通过Handler实现周期性任务触发通知 绑定主线程消息循环，确保UI操作安全
    private final HandlerThread handlerThread = new HandlerThread("NetworkThread");
    private final Handler networkHandler;

    private MediaProjection mProjection;
    private ImageReader mImageReader;

    //用于在虚拟显示层通过帧间隔获取指定真的内容
    private int frameCounter = 0;
    private final int targetFrames = 60 * 60; // 60fps × 半小时一次,但是只有在画面有变动的时候才是最短半小时,所以除去晚上睡觉时间,一天大概10-20张截图左右(10小时手机时间)

    //必须放在这里
    // MediaProjection回调（处理投影生命周期）
    private final MediaProjection.Callback mediaProjCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            //Toast.makeText(MediaProjectionService.this, "当用户主动停止屏幕捕获（如关闭录屏/投屏）", Toast.LENGTH_LONG).show()
        }
    };

    // VirtualDisplay回调（处理显示状态）
    private final VirtualDisplay.Callback virtualDisplayCallback = new VirtualDisplay.Callback() {
        @Override
        public void onPaused() {
            //Toast.makeText(MediaProjectionService.this, "虚拟显示器内容暂停渲染（如应用退到后台）", Toast.LENGTH_LONG).show()
        }

        @Override
        public void onResumed() {
            //Toast.makeText(MediaProjectionService.this, "虚拟显示器恢复渲染（如应用回到前台）", Toast.LENGTH_LONG).show()
        }
    };

    public MediaProjectionService() {
        handlerThread.start();
        networkHandler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void onCreate() {
        try {
            startForegroundService(); //创建通知渠道并启动服务
        } catch (Exception e) {
            Toast.makeText(this, "onCreate 错误：\n" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //创建指定分辨率（width×height）和像素格式（RGBA_8888）的图像读取器，缓冲区数量为 2
        try {
            if (intent != null && intent.getExtras() != null) {
                //Log.d("IntentDebug", it.toString())
            } else {
                Toast.makeText(this, "Intent为空", Toast.LENGTH_LONG).show();
                return START_STICKY;
            }

            if (intent != null) {
                int resultCode = intent.getIntExtra("resultCode", -1);
                Intent data = intent.getParcelableExtra("data");
                if (resultCode == -1 && data != null) {
                    MediaProjection projection = ((MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE))
                        .getMediaProjection(resultCode, data);
                    // 开始屏幕捕获逻辑...
                    //现代的调用方式,和 val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager 这个传统的调用方式功能是一样的
                    WindowManager windowManager = getSystemService(WindowManager.class);
                    android.graphics.Rect bounds = windowManager.getCurrentWindowMetrics().getBounds();
                    int width = bounds.width();
                    int height = bounds.height();
                    int densityDpi = getResources().getDisplayMetrics().densityDpi;
                    float density = getResources().getDisplayMetrics().density;
                    int dpi = densityDpi;
                    //虚拟显示器必须使用 PixelFormat.RGBA_8888 格式的原始数据
                    mImageReader = ImageReader.newInstance(
                        width, height,
                        PixelFormat.RGBA_8888, 2
                    );
                    if (mImageReader != null && mImageReader.getSurface() != null && mImageReader.getSurface().isValid()) {
                        //width 1224 height 2700
                        //Toast.makeText(this, "width " + width + " height " + height, Toast.LENGTH_LONG).show()
                    } else {
                        stopSelf(); // 主动停止服务避免崩溃
                        Toast.makeText(this, "Surface初始化失败", Toast.LENGTH_LONG).show();
                    }

                    //创建虚拟显示层
                    //创建回调
                    projection.registerCallback(
                        mediaProjCallback,
                        new Handler(Looper.getMainLooper())
                    );
                    VirtualDisplay virtualDisplay = projection.createVirtualDisplay(
                        "ScreenCapture",
                        width, height, dpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mImageReader.getSurface(),
                        virtualDisplayCallback,
                        null
                    );
                    if (virtualDisplay == null) {
                        Toast.makeText(this, "VirtualDisplay创建失败", Toast.LENGTH_LONG).show();
                    } else {
                        //width 1224 height 2700
                        //Toast.makeText(this, "VirtualDisplay width " + width + " height " + height, Toast.LENGTH_LONG).show()
                    }

                    //创建监听器 - 实际测试,这个回调并不是按照帧率进行触发,如果屏幕画面没有变化则不会进行触发...
                    mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(ImageReader reader) {
                            try {
                                //放到if语句之外,用于消费缓冲区的图片防止因为缓冲区满导致停止触发回调
                                Image image = reader.acquireLatestImage();
                                if (image != null) {
//                                     if (++frameCounter >= targetFrames) {
                                    frameCounter = 0;
                                    // 处理图像数据
                                    List<Image.Plane> planes = Arrays.asList(image.getPlanes());
                                    ByteBuffer buffer = (planes != null && !planes.isEmpty()) ? planes.get(0).getBuffer() : null;
                                    int width = image.getWidth();
                                    int height = image.getHeight();
                                    Integer pixelStride = (planes != null && !planes.isEmpty()) ? planes.get(0).getPixelStride() : null; //单个像素占用的字节数（RGBA_8888固定为4字节）
                                    Integer rowStride = (planes != null && !planes.isEmpty()) ? planes.get(0).getRowStride() : null; //每行实际字节数（含可能的内存对齐填充）

                                    if (buffer != null && pixelStride != null && rowStride != null) {
                                        byte[] byteArray = new byte[buffer.remaining()];
                                        buffer.get(byteArray); // 将数据从buffer复制到byteArray
                                        // 交换字节顺序：RGBA -> ARGB
                                        //buffer.rewind()  // 重置指针
                                        ByteBuffer newBuffer = ByteBuffer.wrap(byteArray);
                                        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                                        // 多于了 bitmap.copyPixelsFromBuffer(newBuffer) // 直接复用原始Buffer,将 newBuffer 的原始像素数据复制到 bitmap
                                        // 带stride处理的像素转换
                                        for (int y = 0; y < height; y++) {
                                            for (int x = 0; x < width; x++) {
                                                int offset = y * rowStride + x * pixelStride;
                                                if (offset + 3 >= buffer.limit()) break;

                                                int r = buffer.get(offset) & 0xFF;
                                                int g = buffer.get(offset + 1) & 0xFF;
                                                int b = buffer.get(offset + 2) & 0xFF;
                                                int a = buffer.get(offset + 3) & 0xFF;
                                                bitmap.setPixel(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                                            }
                                        }
                                        if (bitmap != null) {
                                            // 推送通知截图成功
                                            // showScreenshotSuccessNotification();
                                            // 把图片写入相册实现
                                            try {
                                                saveImageToGallery(bitmap);
                                            } catch (Exception e) {
                                                Toast.makeText(
                                                    MediaProjectionService.this,
                                                    "保存图片到相册失败: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT
                                                ).show();
                                            }

                                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                            boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream); // JPEG格式，质量80%
                                            final byte[] finalBytes = outputStream.toByteArray();
                                            outputStream.close();
                                            bitmap.recycle();

                                          Toast.makeText(
                                                    MediaProjectionService.this,
                                                    "获取到了图片数据,开始进行上传逻辑",
                                                    Toast.LENGTH_LONG
                                                ).show();

                                            //上传图片
//                                            try {
//                                                Toast.makeText(
//                                                    MediaProjectionService.this,
//                                                    "获取到了图片数据,开始进行上传逻辑",
//                                                    Toast.LENGTH_LONG
//                                                ).show();
//                                                //专用的后台线程处理网络请求
//                                                networkHandler.post(new Runnable() {
//                                                    @Override
//                                                    public void run() {
//                                                        uploadBitmap(finalBytes);
//                                                    }
//                                                });
//                                            } catch (Exception e) {
//                                                Toast.makeText(
//                                                    MediaProjectionService.this,
//                                                    "uploadBitmap方法异常 " + e.getMessage(),
//                                                    Toast.LENGTH_LONG
//                                                ).show();
//                                            }
                                        }
                                    }
//                                     }
                                    image.close(); //防止没有释放
                                }
                            } catch (Exception e) {
                                Toast.makeText(MediaProjectionService.this, "监听器错误：\n" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            } finally {
                            }
                        }
                    }, new Handler(Looper.getMainLooper()));
                    //Toast.makeText(this, "创建监听器 OK", Toast.LENGTH_LONG).show()
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "onStartCommand 错误：\n" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        return START_STICKY;
    }

    //保存图片到相册
    private Uri saveImageToGallery(Bitmap bitmap) throws IOException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, "screenshot_" + System.currentTimeMillis() + ".jpg");
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (uri != null) {
            try {
                try (java.io.OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear();
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(uri, contentValues, null, null);
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(
                            MediaProjectionService.this,
                            "图片已保存到相册",
                            Toast.LENGTH_SHORT
                        ).show();
                    }
                });
                return uri;
            } catch (Exception e) {
                getContentResolver().delete(uri, null, null);
                throw e;
            }
        }
        return null;
    }

    //上传图片
    private final java.util.concurrent.ExecutorService executor = Executors.newSingleThreadExecutor(); //线程池 创建的线程池只包含1个工作线程

    private void uploadBitmap(byte[] bytes) {
        //创建了一个线程的线程池, executor.execute 中的任务会按顺序执行, 前一个异常不会导致有一个任务终止，所有execute提交的任务都在这个唯一线程中执行。
        //任务队列遵循先进先出原则，前一个任务的run()方法完全执行完毕后，才会开始执行下一个任务。
        //该工作线程不会销毁，而是持续存活等待新任务（适合长期后台任务场景）
        //更多: newFixedThreadPool(2)：并行执行2个任务 newCachedThreadPool()：动态扩容线程数 newScheduledThreadPool()：支持延迟/定时任务
        if (bytes == null || bytes.length == 0) {
            Toast.makeText(this, "无效的图片数据", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] byteArray = bytes;
            // 3. 上传配置
            String boundary = "Boundary-" + System.currentTimeMillis();
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String fileName = "screenshot_" + System.currentTimeMillis() + ".jpg";

            // 4. 执行上传文件
            HttpURLConnection connection =
                (HttpURLConnection) new URL("https://footprint.codevtool.com/updata_file.php").openConnection();
            //请求头设置
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=" + boundary
            );
            //请求体构建
            try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                // 添加文件部分
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes(
                    "Content-Disposition: form-data; " +
                            "name=\"file\"; filename=\"" + fileName + "\"" + lineEnd
                );
                dos.writeBytes("Content-Type: file/*" + lineEnd + lineEnd);

                // 写入文件数据
                dos.write(byteArray);

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                dos.flush();
            }
            //响应处理 获取HTTP响应状态码 断开连接 返回上传是否成功(状态码200-299表示成功)
            int responseCode = connection.getResponseCode();

            String response = "";
            try {
                if (responseCode >= 200 && responseCode < 300) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder responseBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                        response = responseBuilder.toString();
                    }
                } else {
                    // 处理错误流
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                        StringBuilder responseBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                        response = responseBuilder.toString();
                    } catch (IOException ex) {
                        response = "无法读取错误响应";
                    }
                }
            } catch (IOException e) {
                response = "无法读取响应: " + e.getMessage();
            } finally {
                connection.disconnect();
            }

            Toast.makeText(this, "我的足迹手机端: 完成了一次截图 " + response, Toast.LENGTH_LONG)
                .show();
        } catch (Exception e) {
            String errorDetail;
            if (e instanceof NullPointerException) {
                errorDetail = "空指针异常: " + getStackTraceString(e);
            } else if (e instanceof IOException) {
                errorDetail = "网络错误: " + (e.getMessage() != null ? e.getMessage() : "无详细信息");
            } else if (e instanceof IllegalStateException) {
                errorDetail = "状态异常: " + e.getMessage();
            } else {
                errorDetail = "未知异常: " + e.getClass().getSimpleName();
            }
            Toast.makeText(this, "我的足迹手机端: 上传异常 " + errorDetail, Toast.LENGTH_LONG)
                .show();
        }
    }

    private String getStackTraceString(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    //创建通知渠道并启动服务
    private void startForegroundService() {
        try {
            //NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            //创建通知通道
            NotificationChannel channel = new NotificationChannel(
                "Clipboard_channel",
                "截屏服务", //<--通知类型会展示这四个字
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setSound(null, null); // 禁用通知音
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); //锁屏可见
            channel.enableVibration(false); // 禁用震动

            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .createNotificationChannel(channel);
            //启动服务并发送通知, 启动前台服务，避免被系统回收
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    2,
                    once_Notification("截屏服务服务运行中"),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                );
                // 用于移除通知
              // 启动辅助服务来移除通知
              Intent helperIntent = new Intent(this, RemoveNoticeService.class);
              startService(helperIntent);
            } else {
                startForeground(2, once_Notification("截屏服务服务运行中"));
            }
            Toast.makeText(this, "通知渠道创建成功,截屏服务服务已启动", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "错误：" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    //启动服务需要的一次性服务
    private Notification once_Notification(String text) {
        return new NotificationCompat.Builder(this, "Clipboard_channel")
            .setContentTitle("截屏服务") //<--停止的标题,但是会被下面的覆盖
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_location)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build();
    }

    //使用兼容库NotificationCompat构建通知，确保旧版本兼容性
    private void buildNotification(String text) {
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(new Date());
        Notification notification = new NotificationCompat.Builder(this, "Clipboard_channel")
            .setContentTitle("剪切板复制的内容") //<--停止的标题,但是会被下面的覆盖
            .setContentText(text + "\n更新时间: " + currentTime)
            .setSmallIcon(R.mipmap.ic_location)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
            .notify(10001, notification);
    }

    //显示截图成功通知
    private void showScreenshotSuccessNotification() {
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(new Date());
        Notification notification = new NotificationCompat.Builder(this, "Clipboard_channel")
            .setContentTitle("截图成功")
            .setContentText("截图已保存并上传\n时间: " + currentTime)
            .setSmallIcon(R.mipmap.ic_location)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // 点击后自动取消
            .build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
            .notify(10003, notification); // 使用不同的通知ID避免覆盖其他通知
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mImageReader != null) {
            mImageReader.close();
        }
        handlerThread.quitSafely();
    }

    @Override
    public android.os.IBinder onBind(Intent intent) {
        return null;
    }
}

