package com.example.test_filesync.api.dto;

import androidx.annotation.NonNull;

import java.util.List;

public class UserInfo {
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

  public class AppItem {
        private String app_name;
        private String package_name;

        public AppItem() {
        }

        public String getAppName() {
            return app_name;
        }

        public void setAppName(String app_name) {
            this.app_name = app_name;
        }

        public String getPackageName() {
            return package_name;
        }

        public void setPackageName(String package_name) {
            this.package_name = package_name;
        }
    }

    private List<AppItem> disabled_apps;

  public List<AppItem> getDisabledApps () {
    return disabled_apps;
  }

    public Config getConfig() {
        return this.config;
    }

    public class Config {
        private int id;
        private int user_id;
        private int is_monitor;
        private String created_at;
        private String updated_at;
        private int show_icon;

        public Config() {
        }

        public int getIsMonitor() {
            return this.is_monitor;
        }

        public int getId() {
            return this.id;
        }

        public int getUserId() {
            return this.user_id;
        }

        public String getCreatedAt() {
            return this.created_at;
        }

        public String getUpdatedAt() {
            return this.updated_at;
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setUserId(int user_id) {
            this.user_id = user_id;
        }

        public void setIsMonitor(int is_monitor) {
            this.is_monitor = is_monitor;
        }

        public void setCreatedAt(String created_at) {
            this.created_at = created_at;
        }

        public void setUpdatedAt(String updated_at) {
            this.updated_at = updated_at;
        }
        public int getShowIcon() {
            return this.show_icon;
        }
        public void setShowIcon(int show_icon) {
            this.show_icon = show_icon;
        }
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
