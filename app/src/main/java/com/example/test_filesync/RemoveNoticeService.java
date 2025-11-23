
package com.example.test_filesync;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class RemoveNoticeService extends Service {
  private static final String CHANNEL_ID = "Clipboard_channel";
  private static final int NOTIFICATION_ID = 2;

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // 创建相同的通知
    Notification notification = createNotification();

    // 启动前台服务并使用相同的通知ID
    startForeground(NOTIFICATION_ID, notification);

    // 立即停止前台并移除通知
    stopForeground(true);

    // 停止自身
    stopSelf();

    return START_NOT_STICKY;
  }

  private Notification createNotification() {
    return new NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("辅助服务")
      .setContentText("临时服务")
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
