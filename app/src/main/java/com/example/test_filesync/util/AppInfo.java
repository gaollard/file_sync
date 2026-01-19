package com.example.test_filesync.util;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.provider.Settings;

import com.example.test_filesync.service.MyAccessibilityService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AppInfo {
    public static String uuid(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return androidId;
    }

    // 获取应用的 SHA1 签名
    public static String getSHA1Signature(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNATURES);
            Signature[] signatures = packageInfo.signatures;
            if (signatures != null && signatures.length > 0) {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                md.update(signatures[0].toByteArray());
                byte[] digest = md.digest();

                // 转换为十六进制字符串
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02X", b));
                }
                return sb.toString();
            }
            return null;
        } catch (Exception e) {
            LogUtils.e(context, "获取SHA1签名失败", e);
            return null;
        }
    }
}
