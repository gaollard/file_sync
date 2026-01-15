package com.example.test_filesync.api;

public interface ApiCallback {
  void onSuccess(String res);

  void onFailure(Exception e);
}
