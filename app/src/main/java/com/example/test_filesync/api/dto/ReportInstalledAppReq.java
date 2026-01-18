package com.example.test_filesync.api.dto;

import java.util.List;

public class ReportInstalledAppReq {

    public ReportInstalledAppReq() {}

    private List<AppItem> apps;

    public List<AppItem> getApps() {
        return apps;
    }

    public void setApps(List<AppItem> apps) {
        this.apps = apps;
    }

    public class AppItem {
        private String appName;
        private String packageName;

        public AppItem() {}

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }
    }
}
