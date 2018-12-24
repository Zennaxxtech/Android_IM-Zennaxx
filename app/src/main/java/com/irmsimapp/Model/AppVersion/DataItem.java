package com.irmsimapp.Model.AppVersion;

import com.google.gson.annotations.SerializedName;


public class DataItem {

    @SerializedName("IOSVersion")
    private String iOSVersion;

    @SerializedName("VersionNo")
    private String versionNo;

    @SerializedName("AndroidVersion")
    private String androidVersion;

    public void setIOSVersion(String iOSVersion) {
        this.iOSVersion = iOSVersion;
    }

    public String getIOSVersion() {
        return iOSVersion;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public void setAndroidVersion(String androidVersion) {
        this.androidVersion = androidVersion;
    }

    public String getAndroidVersion() {
        return androidVersion;
    }

    @Override
    public String toString() {
        return "DataItem{" + "iOSVersion = '" + iOSVersion + '\'' + ",versionNo = '" + versionNo + '\'' + ",androidVersion = '" + androidVersion + '\'' + "}";
    }
}