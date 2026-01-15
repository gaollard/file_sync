package com.example.test_filesync.api.dto;

public class UserInfo {
    // {"id":1,"username":"123","unique_id":"123","password":"123","email":null,"phone":null,"status":1,"created_at":"2026-01-15T00:33:35.063Z","updated_at":"2026-01-15T00:33:35.063Z","config":{"id":1,"user_id":1,"is_monitor":0,"created_at":"2026-01-15T00:33:49.567Z","updated_at":"2026-01-15T00:33:49.567Z"}}
    private int id;
    private String username;
    private String unique_id;
    private String password;
    private String email;
    private String phone;
    private int status;
    private String created_at;
    private String updated_at;
    private Config config;

    public class Config {
        private int id;
        private int user_id;
        private int is_monitor;
        private String created_at;
        private String updated_at;
    }

    public UserInfo() {
    }

   public void setId(int id) {
    this.id = id;
   }

   public void setUsername(String username) {
    this.username = username;
   }

   public void setUniqueId(String unique_id) {
    this.unique_id = unique_id;
   }

   public void setPassword(String password) {
    this.password = password;
   }

   public void setEmail(String email) {
    this.email = email;
   }

   public void setPhone(String phone) {
    this.phone = phone;
   }

   public void setStatus(int status) {
    this.status = status;
   }

   public void setCreatedAt(String created_at) {
    this.created_at = created_at;
   }

   public void setUpdatedAt(String updated_at) {
    this.updated_at = updated_at;
   }

   public void setConfig(Config config) {
    this.config = config;
   }

    public int getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getUniqueId() {
        return this.unique_id;
    }

}
