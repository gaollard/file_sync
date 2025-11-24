package com.example.test_filesync.util;

public interface ApiCallback {
  void onSuccess(String res);

  void onFailure(Exception e);
}
