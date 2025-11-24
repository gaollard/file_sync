package com.example.test_filesync.util;

import okhttp3.*;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 请求工具类
 * 基于 OkHttp 实现常用的 HTTP 请求功能
 */
public class HttpUtil {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    
    private static OkHttpClient client;
    
    static {
        // 初始化 OkHttpClient，设置超时时间
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * 执行 GET 请求（同步）
     * 
     * @param url 请求地址
     * @return 响应字符串，失败返回 null
     */
    public static String get(String url) {
        return get(url, null);
    }
    
    /**
     * 执行 GET 请求（同步，带请求头）
     * 
     * @param url 请求地址
     * @param headers 请求头
     * @return 响应字符串，失败返回 null
     */
    public static String get(String url, Map<String, String> headers) {
        try {
            Request.Builder requestBuilder = new Request.Builder().url(url);
            
            // 添加请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            
            Request request = requestBuilder.build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else {
                    return null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 执行 POST 请求（同步，JSON 格式）
     * 
     * @param url 请求地址
     * @param jsonBody JSON 请求体
     * @return 响应字符串，失败返回 null
     */
    public static String postJson(String url, String jsonBody) {
        return postJson(url, jsonBody, null);
    }
    
    /**
     * 执行 POST 请求（同步，JSON 格式，带请求头）
     * 
     * @param url 请求地址
     * @param jsonBody JSON 请求体
     * @param headers 请求头
     * @return 响应字符串，失败返回 null
     */
    public static String postJson(String url, String jsonBody, Map<String, String> headers) {
        try {
            RequestBody body = RequestBody.create(jsonBody, JSON);
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(body);
            
            // 添加请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            
            Request request = requestBuilder.build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else {
                    return null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 执行 POST 请求（同步，表单格式）
     * 
     * @param url 请求地址
     * @param formData 表单数据
     * @return 响应字符串，失败返回 null
     */
    public static String postForm(String url, Map<String, String> formData) {
        return postForm(url, formData, null);
    }
    
    /**
     * 执行 POST 请求（同步，表单格式，带请求头）
     * 
     * @param url 请求地址
     * @param formData 表单数据
     * @param headers 请求头
     * @return 响应字符串，失败返回 null
     */
    public static String postForm(String url, Map<String, String> formData, Map<String, String> headers) {
        try {
            FormBody.Builder formBuilder = new FormBody.Builder();
            if (formData != null) {
                for (Map.Entry<String, String> entry : formData.entrySet()) {
                    formBuilder.add(entry.getKey(), entry.getValue());
                }
            }
            
            RequestBody body = formBuilder.build();
            Request.Builder requestBuilder = new Request.Builder()
                    .url(url)
                    .post(body);
            
            // 添加请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            
            Request request = requestBuilder.build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else {
                    return null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 执行异步 GET 请求
     * 
     * @param url 请求地址
     * @param callback 回调接口
     */
    public static void getAsync(String url, HttpCallback callback) {
        getAsync(url, null, callback);
    }
    
    /**
     * 执行异步 GET 请求（带请求头）
     * 
     * @param url 请求地址
     * @param headers 请求头
     * @param callback 回调接口
     */
    public static void getAsync(String url, Map<String, String> headers, HttpCallback callback) {
        Request.Builder requestBuilder = new Request.Builder().url(url);
        
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        
        Request request = requestBuilder.build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
            
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (callback != null) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body().string());
                    } else {
                        callback.onFailure(new IOException("请求失败，状态码: " + response.code()));
                    }
                }
            }
        });
    }
    
    /**
     * 执行异步 POST 请求（JSON 格式）
     * 
     * @param url 请求地址
     * @param jsonBody JSON 请求体
     * @param callback 回调接口
     */
    public static void postJsonAsync(String url, String jsonBody, HttpCallback callback) {
        postJsonAsync(url, jsonBody, null, callback);
    }
    
    /**
     * 执行异步 POST 请求（JSON 格式，带请求头）
     * 
     * @param url 请求地址
     * @param jsonBody JSON 请求体
     * @param headers 请求头
     * @param callback 回调接口
     */
    public static void postJsonAsync(String url, String jsonBody, Map<String, String> headers, HttpCallback callback) {
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body);
        
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        
        Request request = requestBuilder.build();
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
            
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (callback != null) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body().string());
                    } else {
                        callback.onFailure(new IOException("请求失败，状态码: " + response.code()));
                    }
                }
            }
        });
    }
    
    /**
     * HTTP 回调接口
     */
    public interface HttpCallback {
        /**
         * 请求成功回调
         * 
         * @param response 响应内容
         */
        void onSuccess(String response);
        
        /**
         * 请求失败回调
         * 
         * @param e 异常信息
         */
        void onFailure(IOException e);
    }
    
    /**
     * 设置自定义的 OkHttpClient
     * 
     * @param customClient 自定义的 OkHttpClient
     */
    public static void setClient(OkHttpClient customClient) {
        client = customClient;
    }
    
    /**
     * 获取当前的 OkHttpClient
     * 
     * @return 当前的 OkHttpClient
     */
    public static OkHttpClient getClient() {
        return client;
    }
}
