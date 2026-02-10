package com.example.test_filesync.util;

import android.content.Context;
import com.example.test_filesync.StudentApplication;
import com.example.test_filesync.api.ApiCallback;
import com.example.test_filesync.api.ApiConfig;
import com.example.test_filesync.api.dto.UserInfo;
import com.google.gson.Gson;
import java.util.HashMap;

public class PullConfig {
    private static UserInfo userInfo;
    private static final String TAG = "PullConfig";

    public interface Callback {
        void onSuccess(UserInfo userInfo);
        void onFailure(Exception e);
    }

    // callback 为可选参数，如果不需要回调，可以传null
    public static void pullConfig(Context context, Callback callback) {
        HttpUtil.config(ApiConfig.user_userInfo, new HashMap<String, Object>())
                .postRequest(context.getApplicationContext(), new ApiCallback() {
                    @Override
                    public void onSuccess(String res) {
                        if (res != null && res.startsWith("{")) {
                            LogUtils.d(context, " pull_config success: " + res);
                            Gson gson = new Gson();
                            UserInfo userInfo = gson.fromJson(res, UserInfo.class);
                            ((StudentApplication) context.getApplicationContext()).setUserInfo(userInfo);
                            PullConfig.userInfo = userInfo;
                            if (callback != null) {
                                callback.onSuccess(userInfo);
                            }
                        } else {
                            LogUtils.d(context, " pull_config error: " + res);
                            if (callback != null) {
                                callback.onFailure(new Exception(res));
                            }
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        LogUtils.d(context, " pull_config failure: " + e.getMessage());
                        if (callback != null) {
                            callback.onFailure(e);
                        }
                    }
                });
    }
    public static UserInfo getUserInfo() {
        return userInfo;
    }
}
