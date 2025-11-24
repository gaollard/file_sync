package com.example.test_filesync.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

public class HttpUtil {
  private static OkHttpClient client;
  private static String requestUrl;
  private static HashMap<String, Object> mParams;
  public static HttpUtil api = new HttpUtil();
  public static String PreferenceName = "news";

  public HttpUtil() { }

  /**
   * @desc 创建 request client
   * @param url
   * @param params
   * @return
   */
  public static HttpUtil config(String url, HashMap<String, Object> params) {
    client = new OkHttpClient.Builder()
      .build();
    requestUrl = ApiConfig.BASE_URl + url;
    mParams = params;
    return api;
  }

  /**
   * @desc POST request
   * @param context
   * @param callback
   */
  public void postRequest(Context context, final ApiCallback callback) {
    SharedPreferences sp = context.getSharedPreferences(PreferenceName, MODE_PRIVATE);
    String token = sp.getString("token", "");

    // 1. 创建请求体
    JSONObject jsonObject = new JSONObject(mParams);
    String jsonStr = jsonObject.toString();
    MediaType mediaType =  MediaType.parse("application/json;charset=utf-8");
    RequestBody requestBodyJson = RequestBody.create(mediaType, jsonStr);

    // 2. 创建 request
    Request request = new Request.Builder()
      .url(requestUrl)
      .addHeader("contentType", "application/json;charset=UTF-8")
      .addHeader("token", token)
      .post(requestBodyJson)
      .build();

    // 3. 创建call回调对象
    final Call call = client.newCall(request);

    // 4. 发起请求
    call.enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        Log.e("onFailure", e.getMessage());
        callback.onFailure(e);
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        final String result = response.body().string();
        callback.onSuccess(result);
      }
    });
  }

  /**
   * @desc Get Request
   * @param context
   * @param callback
   */
  public void getRequest(Context context, final ApiCallback callback) {
    SharedPreferences sp = context.getSharedPreferences(PreferenceName, MODE_PRIVATE);
    String token = sp.getString("token", "");
    String url = getAppendUrl(requestUrl, mParams);

    Request request = new Request.Builder()
      .url(url)
      .addHeader("token", token)
      .get()
      .build();
    Call call = client.newCall(request);

    call.enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        Log.e("onFailure", e.getMessage());
        callback.onFailure(e);
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        final String result = response.body().string();
        try {
          JSONObject jsonObject = new JSONObject(result);
          String code = jsonObject.getString("code");
          if (code.equals("401")) {
            Log.e("login failed", url + code);
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
        callback.onSuccess(result);
      }
    });
  }

  /**
   * @desc encode url
   * @param url
   * @param map
   * @return
   */
  private String getAppendUrl(String url, Map<String, Object> map) {
    if (map != null && !map.isEmpty()) {
      StringBuffer buffer = new StringBuffer();
      Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<String, Object> entry = iterator.next();
        if (StringUtil.isEmpty(buffer.toString())) {
          buffer.append("?");
        } else {
          buffer.append("&");
        }
        buffer.append(entry.getKey()).append("=").append(entry.getValue());
      }
      url += buffer.toString();
    }
    return url;
  }
}
