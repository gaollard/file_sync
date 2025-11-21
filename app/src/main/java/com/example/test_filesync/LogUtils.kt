package com.example.test_filesync

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object LogUtils {
    private const val TAG = "MediaProjectionService"
    private const val LOG_FILE_NAME = "screenshot_log.txt"
    private const val MAX_LOG_FILE_SIZE = 5 * 1024 * 1024 // 5MB
    private val lock = ReentrantLock()
    
    private fun getLogFile(context: Context): File {
        // 优先使用外部存储目录，如果不可用则回退到内部存储
        // 外部存储路径：/storage/emulated/0/Android/data/com.example.test_filesync/files/logs/
        // 内部存储路径：/data/data/com.example.test_filesync/files/logs/
        // 外部存储可以通过手机文件管理器直接访问（无需Root）
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val logDir = File(baseDir, "logs")
        if (!logDir.exists()) {
            val created = logDir.mkdirs()
            if (!created) {
                Log.e(TAG, "无法创建日志目录: ${logDir.absolutePath}")
            }
        }
        return File(logDir, LOG_FILE_NAME)
    }
    
    private fun formatLogMessage(level: String, tag: String, message: String): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            .format(Date())
        return "[$timestamp] [$level] [$tag] $message\n"
    }
    
    private fun writeToFile(context: Context, level: String, tag: String, message: String) {
        lock.withLock {
            try {
                val logFile = getLogFile(context)
                
                // 检查文件大小，如果超过限制则清空文件
                if (logFile.exists() && logFile.length() > MAX_LOG_FILE_SIZE) {
                    val deleted = logFile.delete()
                    if (!deleted) {
                        Log.w(TAG, "无法删除旧的日志文件")
                    }
                }
                
                val logMessage = formatLogMessage(level, tag, message)
                FileOutputStream(logFile, true).use { fos ->
                    fos.write(logMessage.toByteArray(Charsets.UTF_8))
                    fos.flush()
                    // 强制同步到磁盘，确保数据立即写入
                    fos.fd.sync()
                }
                // 验证写入是否成功（仅在调试时输出）
                val fileSize = logFile.length()
                if (fileSize == 0L) {
                    Log.w(TAG, "警告：日志文件大小为0，可能未正确写入。路径: ${logFile.absolutePath}")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "写入日志文件失败 - 权限不足: ${e.message}", e)
                Log.e(TAG, "日志文件路径: ${getLogFile(context).absolutePath}")
            } catch (e: IOException) {
                Log.e(TAG, "写入日志文件失败 - IO错误: ${e.message}", e)
                Log.e(TAG, "日志文件路径: ${getLogFile(context).absolutePath}")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e(TAG, "写入日志文件失败 - 未知错误: ${e.javaClass.simpleName} - ${e.message}", e)
                Log.e(TAG, "日志文件路径: ${getLogFile(context).absolutePath}")
                e.printStackTrace()
            }
        }
    }
    
    fun d(context: Context, tag: String = TAG, message: String) {
        Log.d(tag, message)
        writeToFile(context, "DEBUG", tag, message)
    }
    
    fun i(context: Context, tag: String = TAG, message: String) {
        Log.i(tag, message)
        writeToFile(context, "INFO", tag, message)
    }
    
    fun w(context: Context, tag: String = TAG, message: String) {
        Log.w(tag, message)
        writeToFile(context, "WARN", tag, message)
    }
    
    fun e(context: Context, tag: String = TAG, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val errorMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        writeToFile(context, "ERROR", tag, errorMessage)
    }
    
    /**
     * 获取日志文件的完整路径，用于调试
     */
    fun getLogFilePath(context: Context): String {
        return getLogFile(context).absolutePath
    }
}

