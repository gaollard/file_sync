package com.example.test_filesync.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP 请求工具类
 * 封装常用的 HTTP 操作：GET、POST（JSON）、POST（表单）、文件上传等
 */
public class HttpUtil {
    private static final int DEFAULT_CONNECT_TIMEOUT = 10000; // 连接超时时间（毫秒）
    private static final int DEFAULT_READ_TIMEOUT = 30000;    // 读取超时时间（毫秒）
    private static final String CHARSET = "UTF-8";

    /**
     * 执行 GET 请求
     *
     * @param urlStr 请求地址
     * @return 响应结果字符串
     * @throws IOException 网络异常
     */
    public static String get(String urlStr) throws IOException {
        return get(urlStr, null);
    }

    /**
     * 执行 GET 请求（带请求头）
     *
     * @param urlStr     请求地址
     * @param headers    请求头 Map
     * @return 响应结果字符串
     * @throws IOException 网络异常
     */
    public static String get(String urlStr, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
            connection.setDoInput(true);

            // 设置请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 执行 POST 请求（JSON 格式）
     *
     * @param urlStr 请求地址
     * @param json    JSON 字符串
     * @return 响应结果字符串
     * @throws IOException 网络异常
     */
    public static String postJson(String urlStr, String json) throws IOException {
        return postJson(urlStr, json, null);
    }

    /**
     * 执行 POST 请求（JSON 格式，带请求头）
     *
     * @param urlStr  请求地址
     * @param json    JSON 字符串
     * @param headers 请求头 Map
     * @return 响应结果字符串
     * @throws IOException 网络异常
     */
    public static String postJson(String urlStr, String json, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=" + CHARSET);
            connection.setRequestProperty("Accept", "application/json");

            // 设置额外的请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 写入请求体
            if (json != null && !json.isEmpty()) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 执行 POST 请求（表单格式）
     *
     * @param urlStr 请求地址
     * @param params 表单参数 Map
     * @return 响应结果字符串
     * @throws IOException 网络异常
     */
    public static String postForm(String urlStr, Map<String, String> params) throws IOException {
        return postForm(urlStr, params, null);
    }

    /**
     * 执行 POST 请求（表单格式，带请求头）
     *
     * @param urlStr  请求地址
     * @param params  表单参数 Map
     * @param headers 请求头 Map
     * @return 响应结果字符串
     * @throws IOException 网络异常
     */
    public static String postForm(String urlStr, Map<String, String> params, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=" + CHARSET);

            // 设置额外的请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 构建表单数据
            if (params != null && !params.isEmpty()) {
                StringBuilder formData = new StringBuilder();
                boolean first = true;
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (!first) {
                        formData.append("&");
                    }
                    formData.append(entry.getKey())
                            .append("=")
                            .append(java.net.URLEncoder.encode(entry.getValue(), CHARSET));
                    first = false;
                }

                // 写入请求体
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = formData.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 上传文件（multipart/form-data）
     *
     * @param urlStr   请求地址
     * @param fileName 文件名
     * @param fileData 文件数据（字节数组）
     * @return 响应结果字符串
     * @throws IOException 网络异常
     */
    public static String uploadFile(String urlStr, String fileName, byte[] fileData) throws IOException {
        return uploadFile(urlStr, fileName, fileData, null, null);
    }

    /**
     * 上传文件（multipart/form-data，带额外参数和请求头）
     *
     * @param urlStr   请求地址
     * @param fileName 文件名
     * @param fileData 文件数据（字节数组）
     * @param params   额外的表单参数
     * @param headers  请求头 Map
     * @return 响应结果字符串
     * @throws IOException 网络异常
     */
    public static String uploadFile(String urlStr, String fileName, byte[] fileData,
                                    Map<String, String> params, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = null;
        try {
            String boundary = "Boundary-" + System.currentTimeMillis();
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            // 设置额外的请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 构建请求体
            try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                // 添加额外的表单参数
                if (params != null) {
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + lineEnd);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(entry.getValue() + lineEnd);
                    }
                }

                // 添加文件部分
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + lineEnd);
                dos.writeBytes("Content-Type: application/octet-stream" + lineEnd + lineEnd);

                // 写入文件数据
                dos.write(fileData);

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                dos.flush();
            }

            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 从 InputStream 上传文件
     *
     * @param urlStr     请求地址
     * @param fileName   文件名
     * @param inputStream 文件输入流
     * @return 响应结果字符串
     * @throws IOException 网络异常
     */
    public static String uploadFile(String urlStr, String fileName, InputStream inputStream) throws IOException {
        return uploadFile(urlStr, fileName, inputStream, null, null);
    }

    /**
     * 从 InputStream 上传文件（带额外参数和请求头）
     *
     * @param urlStr      请求地址
     * @param fileName    文件名
     * @param inputStream 文件输入流
     * @param params      额外的表单参数
     * @param headers     请求头 Map
     * @return 响应结果字符串
     * @throws IOException 网络异常
     */
    public static String uploadFile(String urlStr, String fileName, InputStream inputStream,
                                    Map<String, String> params, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = null;
        try {
            String boundary = "Boundary-" + System.currentTimeMillis();
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            // 设置额外的请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 构建请求体
            try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                // 添加额外的表单参数
                if (params != null) {
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + lineEnd);
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(entry.getValue() + lineEnd);
                    }
                }

                // 添加文件部分
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + lineEnd);
                dos.writeBytes("Content-Type: application/octet-stream" + lineEnd + lineEnd);

                // 写入文件数据
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                dos.flush();
            }

            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
        }
    }

    /**
     * 读取 HTTP 响应
     *
     * @param connection HttpURLConnection 对象
     * @return 响应结果字符串
     * @throws IOException 网络异常
     */
    private static String readResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        InputStream inputStream;

        // 根据状态码选择输入流
        if (responseCode >= 200 && responseCode < 300) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
            if (inputStream == null) {
                inputStream = connection.getInputStream();
            }
        }

        if (inputStream == null) {
            throw new IOException("无法获取响应流，状态码: " + responseCode);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    /**
     * 设置连接超时时间（毫秒）
     * 注意：此方法需要在创建连接前调用，当前实现使用默认值
     * 如需自定义超时，可以在方法中增加超时参数
     */
    public static void setConnectTimeout(int timeout) {
        // 可以扩展为实例方法或使用 ThreadLocal 存储
    }

    /**
     * 设置读取超时时间（毫秒）
     * 注意：此方法需要在创建连接前调用，当前实现使用默认值
     * 如需自定义超时，可以在方法中增加超时参数
     */
    public static void setReadTimeout(int timeout) {
        // 可以扩展为实例方法或使用 ThreadLocal 存储
    }
}
