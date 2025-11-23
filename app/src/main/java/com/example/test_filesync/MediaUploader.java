package com.example.test_filesync;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MediaUploader {
    private final Context context;
    private final String boundary; // 用于分隔多部分表单数据的不同部分，使用时间戳确保唯一性
    private final String lineEnd = "\r\n"; // 换行符，符合HTTP协议规范
    private final String twoHyphens = "--"; // 边界标识符前缀

    public MediaUploader(Context context) {
        this.context = context;
        this.boundary = String.valueOf(System.currentTimeMillis());
    }

    public String uploadMedia(Uri uri) {
        try {
            // 文件信息查询 参数 URI 通过ContentResolver查询URI对应的文件信息
            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{
                            MediaStore.Images.Media.DISPLAY_NAME, // 获取文件名(DISPLAY_NAME)和相对路径(RELATIVE_PATH)
                            MediaStore.Images.Media.RELATIVE_PATH
                    },
                    null, null, null
            );

            if (cursor == null) {
                return "查询失败1"; // 如果查询失败(返回null)则直接返回false
            }

            try (cursor) {
                if (!cursor.moveToFirst()) {
                    return "查询失败2";
                }

                String fileName = cursor.getString(0);
                InputStream inputStream = context.getContentResolver().openInputStream(uri); // 文件流准备 从Cursor获取文件名 打开文件输入流，准备读取文件内容

                if (inputStream == null) {
                    return "打开文件流失败";
                }

                HttpURLConnection connection = (HttpURLConnection) new URL("https://footprint.codevtool.com/updata_file.php").openConnection(); // HTTP请求构建

                // 请求头设置
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=" + boundary
                );

                // 请求体构建
                try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                    // 添加文件部分
                    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                    outputStream.writeBytes(
                            "Content-Disposition: form-data; " +
                                    "name=\"file\"; filename=\"" + fileName + "\"" + lineEnd
                    );
                    outputStream.writeBytes("Content-Type: file/*" + lineEnd + lineEnd);

                    try (inputStream) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }

                    outputStream.writeBytes(lineEnd);
                    outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    outputStream.flush();
                }

                // 响应处理 获取HTTP响应状态码 断开连接 返回上传是否成功(状态码200-299表示成功)
                int responseCode = connection.getResponseCode();

                String response;
                try (InputStream responseStream = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream))) {
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    response = responseBuilder.toString();
                }

                connection.disconnect();

                // 扩展成功状态码判断范围
                return response;
            }
        } catch (Exception e) {
            // 因为
            String message = e.getMessage();
            if (message != null && message.contains("pending")) {
                return "文件审核中，请稍后重试";
            } else if (message != null && message.contains("trashed")) {
                return "文件已在回收站，需先恢复";
            } else {
                return "无文件访问权限Error: " + message;
            }
        }
    }
}

