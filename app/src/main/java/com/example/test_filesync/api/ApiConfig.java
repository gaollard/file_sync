package com.example.test_filesync.api;

public class ApiConfig {
  public static final int PAGE_SIZE = 10;
  // public static final String BASE_URl = "https://7de27a4c312b.ngrok-free.app/api";

  public static final String BASE_URl = "https://admin.airtlab.com/api";
  public static final String LOGIN = "/app/login"; //登录
  public static final String REGISTER = "/app/register";//注册
  public static final String user_userInfo = "/app/account/get_user_info";  // 获取用户信息
  public static final String report_location = "/app/report/report_location";  // 上报位置信息
  public static final String report_installed_app = "/app/report/report_installed_app";  // 上报已安装应用列表
  public static final String report_screenshot = "/app/report/report_screenshot";  // 上报截图
}
