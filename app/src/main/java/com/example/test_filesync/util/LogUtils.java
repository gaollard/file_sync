package com.example.test_filesync.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

public class LogUtils {
    private static final String TAG = "MediaProjectionService";
    private static final String LOG_FILE_NAME = "screenshot_log.txt";
    private static final long MAX_LOG_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final ReentrantLock lock = new ReentrantLock();

    private static File getLogFile(Context context) {
        // 优先使用外部存储目录，如果不可用则回退到内部存储
        // 外部存储路径：/storage/emulated/0/Android/data/com.example.test_filesync/files/logs/
        // 内部存储路径：/data/data/com.example.test_filesync/files/logs/
        // 外部存储可以通过手机文件管理器直接访问（无需Root）
        File baseDir = context.getExternalFilesDir(null);
        if (baseDir == null) {
            baseDir = context.getFilesDir();
        }
        File logDir = new File(baseDir, "logs");
        if (!logDir.exists()) {
            boolean created = logDir.mkdirs();
            if (!created) {
                Log.e(TAG, "无法创建日志目录: " + logDir.getAbsolutePath());
            }
        }
        return new File(logDir, LOG_FILE_NAME);
    }

    private static String formatLogMessage(String level, String tag, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(new Date());
        return "[" + timestamp + "] [" + level + "] [" + tag + "] " + message + "\n";
    }

    private static void writeToFile(Context context, String level, String tag, String message) {
        lock.lock();
        try {
            File logFile = getLogFile(context);

            // 检查文件大小，如果超过限制则清空文件
            if (logFile.exists() && logFile.length() > MAX_LOG_FILE_SIZE) {
                boolean deleted = logFile.delete();
                if (!deleted) {
                    Log.w(TAG, "无法删除旧的日志文件");
                }
            }

            String logMessage = formatLogMessage(level, tag, message);
            try (FileOutputStream fos = new FileOutputStream(logFile, true)) {
                fos.write(logMessage.getBytes(StandardCharsets.UTF_8));
                fos.flush();
                // 强制同步到磁盘，确保数据立即写入
                fos.getFD().sync();
            }
            // 验证写入是否成功（仅在调试时输出）
            long fileSize = logFile.length();
            if (fileSize == 0L) {
                Log.w(TAG, "警告：日志文件大小为0，可能未正确写入。路径: " + logFile.getAbsolutePath());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "写入日志文件失败 - 权限不足: " + e.getMessage(), e);
            Log.e(TAG, "日志文件路径: " + getLogFile(context).getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "写入日志文件失败 - IO错误: " + e.getMessage(), e);
            Log.e(TAG, "日志文件路径: " + getLogFile(context).getAbsolutePath());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "写入日志文件失败 - 未知错误: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
            Log.e(TAG, "日志文件路径: " + getLogFile(context).getAbsolutePath());
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public static void d(Context context, String message) {
        d(context, TAG, message);
    }

    public static void d(Context context, String tag, String message) {
        Log.d(tag, message);
        writeToFile(context, "DEBUG", tag, message);
    }

    public static void i(Context context, String message) {
        i(context, TAG, message);
    }

    public static void i(Context context, String tag, String message) {
        Log.i(tag, message);
        writeToFile(context, "INFO", tag, message);
    }

    public static void w(Context context, String message) {
        w(context, TAG, message);
    }

    public static void w(Context context, String tag, String message) {
        Log.w(tag, message);
        writeToFile(context, "WARN", tag, message);
    }

    public static void e(Context context, String message) {
        e(context, TAG, message, null);
    }

    public static void e(Context context, String message, Throwable throwable) {
        e(context, TAG, message, throwable);
    }

    public static void e(Context context, String tag, String message) {
        e(context, tag, message, null);
    }

    public static void e(Context context, String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
        String errorMessage;
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            errorMessage = message + "\n" + sw.toString();
        } else {
            errorMessage = message;
        }
        writeToFile(context, "ERROR", tag, errorMessage);
    }

    /**
     * 获取日志文件的完整路径，用于调试
     */
    public static String getLogFilePath(Context context) {
        return getLogFile(context).getAbsolutePath();
    }

    /**
     * 获取日志文件对象，用于读取日志内容
     */
    public static File getLogFileForRead(Context context) {
        return getLogFile(context);
    }
}

