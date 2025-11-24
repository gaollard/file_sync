package com.example.test_filesync.util;

public class StringUtil {
  public static boolean isEmpty(String str) {
    if (str == null || str.length() <= 0) {
      return true;
    } else {
      return false;
    }
  }
}
