package com.example.test_filesync.service;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

import android.content.Intent;
import android.app.Service; // 服务基类

import android.app.Notification; //
import android.app.NotificationChannel;   // Android 8.0+ 通知通道
import android.app.NotificationManager;

import androidx.core.app.NotificationCompat; // 通知构建器

import android.os.Build;                  // 版本兼容性处理

//import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
import android.database.ContentObserver;
import android.provider.MediaStore;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import android.database.Cursor;

import com.example.test_filesync.util.MediaUploader;
import com.example.test_filesync.R;


public class FileSyncService extends Service {
    private Handler handler = new Handler(Looper.getMainLooper()); //通过Handler实现周期性任务触发通知 绑定主线程消息循环，确保UI操作安全
    private NotificationManager notificationManager; //延迟初始化用于通知管理，符合Android 8.0+的通知渠道要求

    private ContentObserver mediaObserver; //内容观察者

    private FileObserver fileObserver; //文件观察者

    private MediaUploader uploader; //上传文件功能 class

    // 在Service类顶部添加去重集合
    private ConcurrentHashMap<String, Boolean> processingUris = new ConcurrentHashMap<>(); // 线程安全的去重集合

    private ConcurrentHashMap<String, Boolean> pendingUris = new ConcurrentHashMap<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor(); //线程池

    //初始化媒体观察者 创建通知渠道并启动服务 实现10秒间隔的循环任务
    @Override
    public void onCreate() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        try {
            // 初始化媒体观察者
            mediaObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    handleMediaChange(uri);
                }
            };

            startForegroundService(); //创建通知渠道并启动服务
            startPeriodicTask(); //通过 handler.postDelayed() 实现10秒间隔的循环任务
        } catch (Exception e) {
            Toast.makeText(this, "错误：" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    //注册内容观察者（监听图片和视频）
    //onCreate() → onStartCommand() 服务已启动后再次调用 仅触发 onStartCommand() 方法， 不会再次执行 onCreate()
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getContentResolver().registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaObserver //<--这是一个对象,看上面
        );
        getContentResolver().registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaObserver
        );
        return START_STICKY; //当服务关闭后自动重新启动服务
    }

    //每10秒发送一次通知任务
    private void startPeriodicTask() {
        handler.postDelayed(periodicTask, 10_000);
    }

    //这是任务的详细内容
    //periodicTask作为Runnable对象，每次执行时更新通知内容并重新调度下一次任务，形成闭环
    private Runnable periodicTask = new Runnable() {
        @Override
        public void run() {
            // 执行定时任务逻辑
            //sendBroadcast(new Intent(FileSyncService.this, AlarmReceiver.class));
            buildNotification("任务已触发");
            // 重新调度下一次任务
            handler.postDelayed(this, 10_000);
        }
    };

    //媒体观察者回调方法
    //触发情况:
    //拍照、截图、下载图片等操作写入媒体库时
    //重命名、移动文件路径或修改 EXIF 信息
    //删除多张图片时可能触发库级 URI 通知（取决于厂商实现）
    //可能不会触发
    //直接文件删除 通过文件管理器删除文件但未更新媒体库
    // 厂商定制路径 部分设备截屏保存到非标准路径（如 MIUI/screen_cap），未触发系统媒体库通知
    //分区存储限制下，应用无权限访问的目录变更不会通知
    private void handleMediaChange(Uri uri) {
        //媒体库的URI中的ID是唯一的,且大概率是递增的,所以后续可以根据这个ID来获取新增的媒体文件,确保截图或者拍照时没有触发回调的问题
        if (uri != null) {
            final Uri safeUri = uri;
            //是线程池任务提交
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    //确保单次线程异常不会导致服务被停止
                    String uriStr = safeUri.toString();
                    try {
                        //线程安全的将uri字符串作为key放入Map 当key不存在时返回null，存在时返回原值,所以只会在第一次时执行
                        if (pendingUris.putIfAbsent(uriStr, true) == null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(FileSyncService.this, "处理媒体数据,有可能查不到\n" + uriStr, Toast.LENGTH_LONG)
                                        .show();
                                }
                            });
                            // 1. 获取媒体文件元数据 (有可能查询不到,因为没有更新到媒体库中,后续采用其它方式主动轮询数据库获取媒体数据)
                            String[] projection = {
                                MediaStore.Images.Media.SIZE,
                                MediaStore.Images.Media.DATE_MODIFIED
                            };
                            Cursor cursor = getContentResolver().query(
                                safeUri,
                                projection,
                                null,
                                null,
                                null
                            );
                            if (cursor != null) {
                                try {
                                    if (cursor.moveToFirst()) {
                                        // 此时 cursor 指向最新的一条记录
                                        long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
                                        long dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED));
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(FileSyncService.this, "文件大小:" + size + "\n更新时间:" + dateModified, Toast.LENGTH_LONG).show();
                                            }
                                        });
                                        //执行循环
                                        checkFileSizeWithRetry(safeUri, 15, 1000);
                                    }
                                } finally {
                                    cursor.close();
                                }
                            } else {  // 处理查询返回null的情况
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(FileSyncService.this, "查询失败：游标为空", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        final Exception exception = e;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(FileSyncService.this, "出错了: " + exception, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            });
        }
    }

    //采用Handler延迟消息机制实现非阻塞轮询查询媒体文件数据
    private void checkFileSizeWithRetry(Uri uri, int maxRetries, long delayMs) {
        TimeoutRunnable timeoutRunnable = new TimeoutRunnable(uri, maxRetries, delayMs);
        // 立即执行第一次检查
        handler.post(timeoutRunnable);
    }

    private class TimeoutRunnable implements Runnable {
        private Uri uri;
        private int maxRetries;
        private long delayMs;
        private int retryCount = 0;

        public TimeoutRunnable(Uri uri, int maxRetries, long delayMs) {
            this.uri = uri;
            this.maxRetries = maxRetries;
            this.delayMs = delayMs;
        }

        @Override
        public void run() {
            try {
                String[] projection = {
                    MediaStore.Images.Media.SIZE,
                    MediaStore.Images.Media.DATE_MODIFIED
                };

                Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            long size = cursor.getLong(0);
                            long dateModified = cursor.getLong(1);

                            if (size > 0) {
                                // 成功获取有效大小
                                final long finalSize = size;
                                final long finalDateModified = dateModified;
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
                                        String dateStr = sdf.format(new Date(finalDateModified * 1000));
                                        Toast.makeText(
                                            FileSyncService.this,
                                            "最终文件大小: " + (finalSize / 1024.0f) + "KB\n更新时间:" + dateStr,
                                            Toast.LENGTH_LONG
                                        ).show();
                                        //请求网络
                                        upFileData(uri);
                                    }
                                });
                                return;
                            }
                        }
                    } catch (Exception e) {
                        // if 里面可能会出现异常
                    } finally {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                //异常处理
            }
            //如果尝试次数小于最大次数,就继续执行,否则就弹出超时提示框(基本上不会超过十几秒)
            //无需添加异常处理,因为这个只是 handler.post(Android框架内部已处理线程调度异常) 将任务发送给 主线程执行
            if (++retryCount < maxRetries) {
                handler.postDelayed(this, delayMs);
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(
                            FileSyncService.this,
                            "获取文件大小超时，请检查文件是否完整写入",
                            Toast.LENGTH_LONG
                        ).show();
                    }
                });
            }
        }
    }

    private void upFileData(Uri uri) {
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String result = getUploader().uploadMedia(uri);
                        Toast.makeText(this, "我的足迹: " + result, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    // if 里面可能会出现的异常
                    Toast.makeText(this, "上传方法中的 上传过程 出错了", Toast.LENGTH_LONG).show();
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "上传方法中的 contentResolver 出错了", Toast.LENGTH_LONG).show();
        }
    }

    //创建通知渠道并启动服务
    private void startForegroundService() {
        try {
            //创建通知通道
            NotificationChannel channel = new NotificationChannel(
                "periodic_channel",
                "位置服务", //<--通知类型会展示这四个字
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
                    1,
                    onceNotification("服务运行中"),
                    FOREGROUND_SERVICE_TYPE_LOCATION
                );
            } else {
                startForeground(1, onceNotification("服务运行中"));
            }
            Toast.makeText(this, "通知渠道创建成功,媒体同步服务已启动", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "错误：" + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    //启动服务需要的一次性服务
    private Notification onceNotification(String text) {
        return new NotificationCompat.Builder(this, "periodic_channel")
            .setContentTitle("一次服务通知") //<--停止的标题,但是会被下面的覆盖
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
        Notification notification = new NotificationCompat.Builder(this, "periodic_channel")
            .setContentTitle("周期提醒") //<--停止的标题,但是会被下面的覆盖
            .setContentText(text + "\n更新时间: " + currentTime)
            .setSmallIcon(R.mipmap.ic_location)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
            .notify(10002, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        handler.removeCallbacks(periodicTask); //中移除所有待处理消息，避免内存泄漏
        getContentResolver().unregisterContentObserver(mediaObserver); //注销媒体观察者

        executor.shutdown();
    }

    @Override
    public android.os.IBinder onBind(Intent intent) {
        return null;
    }

    // 延迟初始化 uploader
    private MediaUploader getUploader() {
        if (uploader == null) {
            uploader = new MediaUploader(this);
        }
        return uploader;
    }
}

