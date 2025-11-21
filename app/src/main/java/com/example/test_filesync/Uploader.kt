package com.example.test_filesync

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import com.example.test_filesync.FileSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class MediaUploader(private val context: Context) {
    private val boundary = "${System.currentTimeMillis()}" //用于分隔多部分表单数据的不同部分，使用时间戳确保唯一性
    private val lineEnd = "\r\n"                           //换行符，符合HTTP协议规范
    private val twoHyphens = "--"                          //边界标识符前缀

    fun uploadMedia(uri: Uri): String {
        try {
            //文件信息查询 参数 URI 通过ContentResolver查询URI对应的文件信息
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Images.Media.DISPLAY_NAME, //获取文件名(DISPLAY_NAME)和相对路径(RELATIVE_PATH)
                    MediaStore.Images.Media.RELATIVE_PATH
                ),
                null, null, null
            ) ?: return "查询失败1" //如果查询失败(返回null)则直接返回false

            cursor.use {
                if (!it.moveToFirst()) return "查询失败2"

                val fileName = it.getString(0)
                val inputStream = context.contentResolver.openInputStream(uri) //文件流准备 从Cursor获取文件名 打开文件输入流，准备读取文件内容
                    ?: return "打开文件流失败"

                val connection = URL("https://footprint.codevtool.com/updata_file.php").openConnection() as HttpURLConnection //HTTP请求构建

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

                        inputStream.use { stream ->
                            stream.copyTo(this)
                        }

                        writeBytes(lineEnd)
                        writeBytes("$twoHyphens$boundary$twoHyphens$lineEnd")
                        flush()
                    }
                }
                //响应处理 获取HTTP响应状态码 断开连接 返回上传是否成功(状态码200-299表示成功)
                val responseCode = connection.responseCode
                connection.disconnect()

                val response = connection.inputStream.bufferedReader().use { it.readText() } //服务器返回的内容
                // 扩展成功状态码判断范围
                return response
            }
        } catch (e: Exception) {
            //因为
            return when {
                e.message?.contains("pending") == true -> "文件审核中，请稍后重试"
                e.message?.contains("trashed") == true -> "文件已在回收站，需先恢复"
                else -> "无文件访问权限Error: ${e.message}"
            }
        }
    }
}
