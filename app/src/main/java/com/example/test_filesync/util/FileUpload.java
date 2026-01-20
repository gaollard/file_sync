package com.example.test_filesync.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.test_filesync.api.ApiCallback;
import com.example.test_filesync.api.ApiConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

/**
 * 文件上传工具类
 * 支持上传字节数组、文件、Bitmap等多种格式
 * 使用 OkHttp 实现异步上传，支持进度回调
 */
public class FileUpload {
    private static final String TAG = "FileUpload";
    private static final String PREF_NAME = "news";
    
    // 默认超时设置（文件上传需要更长的超时时间）
    private static final int CONNECT_TIMEOUT = 30; // 秒
    private static final int WRITE_TIMEOUT = 60; // 秒
    private static final int READ_TIMEOUT = 60; // 秒
    
    private OkHttpClient client;
    private Context context;
    private Handler mainHandler;
    
    /**
     * 构造函数
     * @param context 上下文对象
     */
    public FileUpload(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * 上传文件（字节数组）
     * @param apiPath API路径（相对路径，会自动拼接BASE_URL）
     * @param fileBytes 文件字节数组
     * @param fileName 文件名
     * @param callback 回调接口
     */
    public void uploadBytes(String apiPath, byte[] fileBytes, String fileName, ApiCallback callback) {
        if (fileBytes == null || fileBytes.length == 0) {
            if (callback != null) {
                mainHandler.post(() -> callback.onFailure(new IllegalArgumentException("文件数据为空")));
            }
            return;
        }
        
        String url = ApiConfig.BASE_URl + apiPath;
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), fileBytes);
        
        uploadFile(url, fileBody, fileName, "file", callback);
    }
    
    /**
     * 上传图片（字节数组）
     * @param apiPath API路径
     * @param imageBytes 图片字节数组
     * @param fileName 文件名
     * @param callback 回调接口
     */
    public void uploadImage(String apiPath, byte[] imageBytes, String fileName, ApiCallback callback) {
        if (imageBytes == null || imageBytes.length == 0) {
            if (callback != null) {
                mainHandler.post(() -> callback.onFailure(new IllegalArgumentException("图片数据为空")));
            }
            return;
        }
        
        String url = ApiConfig.BASE_URl + apiPath;
        RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);
        
        uploadFile(url, fileBody, fileName, "file", callback);
    }
    
    /**
     * 上传 Bitmap
     * @param apiPath API路径
     * @param bitmap Bitmap对象
     * @param fileName 文件名
     * @param quality 压缩质量（0-100）
     * @param callback 回调接口
     */
    public void uploadBitmap(String apiPath, Bitmap bitmap, String fileName, int quality, ApiCallback callback) {
        if (bitmap == null) {
            if (callback != null) {
                mainHandler.post(() -> callback.onFailure(new IllegalArgumentException("Bitmap为空")));
            }
            return;
        }
        
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            outputStream.close();
            
            uploadImage(apiPath, imageBytes, fileName, callback);
        } catch (IOException e) {
            if (callback != null) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        }
    }
    
    /**
     * 上传文件对象
     * @param apiPath API路径
     * @param file 文件对象
     * @param callback 回调接口
     */
    public void uploadFile(String apiPath, File file, ApiCallback callback) {
        if (file == null || !file.exists()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onFailure(new IllegalArgumentException("文件不存在")));
            }
            return;
        }
        
        String url = ApiConfig.BASE_URl + apiPath;
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        
        uploadFile(url, fileBody, file.getName(), "file", callback);
    }
    
    /**
     * 核心上传方法
     * @param url 完整URL
     * @param fileBody 文件请求体
     * @param fileName 文件名
     * @param formFieldName 表单字段名
     * @param callback 回调接口
     */
    private void uploadFile(String url, RequestBody fileBody, String fileName, String formFieldName, ApiCallback callback) {
        try {
            // 获取 token
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            String token = sp.getString("token", AppInfo.uuid(context));
            
            // 构建 multipart 请求体
            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(formFieldName, fileName, fileBody);
            
            RequestBody requestBody = builder.build();
            
            // 构建请求
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("token", token)
                    .post(requestBody)
                    .build();
            
            // 执行异步请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "上传失败: " + e.getMessage());
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFailure(e));
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String result = response.body() != null ? response.body().string() : "";
                        
                        if (response.isSuccessful()) {
                            Log.d(TAG, "上传成功: " + result);
                            if (callback != null) {
                                String finalResult = result;
                                mainHandler.post(() -> callback.onSuccess(finalResult));
                            }
                        } else {
                            Log.e(TAG, "上传失败，响应码: " + response.code());
                            if (callback != null) {
                                mainHandler.post(() -> callback.onFailure(
                                    new IOException("上传失败，响应码: " + response.code())
                                ));
                            }
                        }
                    } finally {
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "构建请求失败: " + e.getMessage());
            if (callback != null) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        }
    }
    
    /**
     * 上传进度回调接口
     */
    public interface UploadProgressCallback extends ApiCallback {
        /**
         * 上传进度回调
         * @param bytesWritten 已上传字节数
         * @param totalBytes 总字节数
         */
        void onProgress(long bytesWritten, long totalBytes);
    }
    
    /**
     * 带进度回调的上传方法
     * @param apiPath API路径
     * @param fileBytes 文件字节数组
     * @param fileName 文件名
     * @param callback 带进度的回调接口
     */
    public void uploadBytesWithProgress(String apiPath, byte[] fileBytes, String fileName, UploadProgressCallback callback) {
        if (fileBytes == null || fileBytes.length == 0) {
            if (callback != null) {
                mainHandler.post(() -> callback.onFailure(new IllegalArgumentException("文件数据为空")));
            }
            return;
        }
        
        String url = ApiConfig.BASE_URl + apiPath;
        
        // 创建带进度监听的 RequestBody
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), fileBytes);
        ProgressRequestBody progressRequestBody = new ProgressRequestBody(fileBody, (bytesWritten, totalBytes) -> {
            if (callback != null) {
                mainHandler.post(() -> callback.onProgress(bytesWritten, totalBytes));
            }
        });
        
        uploadFileWithProgress(url, progressRequestBody, fileName, "file", callback);
    }
    
    /**
     * 带进度的核心上传方法
     */
    private void uploadFileWithProgress(String url, RequestBody fileBody, String fileName, String formFieldName, UploadProgressCallback callback) {
        try {
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            String token = sp.getString("token", AppInfo.uuid(context));
            
            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(formFieldName, fileName, fileBody);
            
            RequestBody requestBody = builder.build();
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("token", token)
                    .post(requestBody)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "上传失败: " + e.getMessage());
                    if (callback != null) {
                        mainHandler.post(() -> callback.onFailure(e));
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String result = response.body() != null ? response.body().string() : "";
                        
                        if (response.isSuccessful()) {
                            Log.d(TAG, "上传成功: " + result);
                            if (callback != null) {
                                String finalResult = result;
                                mainHandler.post(() -> callback.onSuccess(finalResult));
                            }
                        } else {
                            Log.e(TAG, "上传失败，响应码: " + response.code());
                            if (callback != null) {
                                mainHandler.post(() -> callback.onFailure(
                                    new IOException("上传失败，响应码: " + response.code())
                                ));
                            }
                        }
                    } finally {
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "构建请求失败: " + e.getMessage());
            if (callback != null) {
                mainHandler.post(() -> callback.onFailure(e));
            }
        }
    }
    
    /**
     * 带进度监听的 RequestBody 包装类
     */
    private static class ProgressRequestBody extends RequestBody {
        private final RequestBody requestBody;
        private final ProgressListener progressListener;
        
        public interface ProgressListener {
            void onProgress(long bytesWritten, long totalBytes);
        }
        
        public ProgressRequestBody(RequestBody requestBody, ProgressListener progressListener) {
            this.requestBody = requestBody;
            this.progressListener = progressListener;
        }
        
        @Override
        public MediaType contentType() {
            return requestBody.contentType();
        }
        
        @Override
        public long contentLength() throws IOException {
            return requestBody.contentLength();
        }
        
        @Override
        public void writeTo(okio.BufferedSink sink) throws IOException {
            okio.BufferedSink bufferedSink = okio.Okio.buffer(new okio.ForwardingSink(sink) {
                long bytesWritten = 0L;
                long totalBytes = 0L;
                
                @Override
                public void write(okio.Buffer source, long byteCount) throws IOException {
                    super.write(source, byteCount);
                    if (totalBytes == 0) {
                        totalBytes = contentLength();
                    }
                    bytesWritten += byteCount;
                    if (progressListener != null) {
                        progressListener.onProgress(bytesWritten, totalBytes);
                    }
                }
            });
            
            requestBody.writeTo(bufferedSink);
            bufferedSink.flush();
        }
    }
}
