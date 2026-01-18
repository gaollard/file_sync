package com.example.test_filesync.api;

public class ApiConfig {
  public static final int PAGE_SIZE = 10;
  public static final String BASE_URl = "https://83c4fa6933a0.ngrok-free.app/api";
  public static final String LOGIN = "/app/login"; //登录
  public static final String REGISTER = "/app/register";//注册

  public static final String user_userInfo = "/app/account/get_user_info";  // 获取用户信息
  public static final String report_location = "/app/report/report_location";  // 上报位置信息
}
