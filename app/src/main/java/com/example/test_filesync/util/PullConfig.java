package com.example.test_filesync.util;

import android.content.Context;
import com.example.test_filesync.StudentApplication;
import com.example.test_filesync.api.ApiCallback;
import com.example.test_filesync.api.ApiConfig;
import com.example.test_filesync.api.dto.UserInfo;
import com.google.gson.Gson;
import java.util.HashMap;

public class PullConfig {
    private static final String TAG = "PullConfig";
    public static void pullConfig(Context context) {
        HttpUtil.config(ApiConfig.user_userInfo, new HashMap<String, Object>())
                .postRequest(context.getApplicationContext(), new ApiCallback() {
                    @Override
                    public void onSuccess(String res) {
                        if (res != null && res.startsWith("{")) {
                            LogUtils.d(context, " pull_config success: " + res);
                            Gson gson = new Gson();
                            UserInfo userInfo = gson.fromJson(res, UserInfo.class);
                            ((StudentApplication) context.getApplicationContext()).setUserInfo(userInfo);
                        } else {
                            LogUtils.d(context, " pull_config error: " + res);
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        LogUtils.d(context, " pull_config failure: " + e.getMessage());
                    }
                });
    }
}
