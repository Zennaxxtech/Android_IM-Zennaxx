package com.irmsimapp.Model.AppVersion;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AppVersion {

    @SerializedName("Status")
    private String status;

    @SerializedName("Msg")
    private String msg;

    @SerializedName("Data")
    private List<DataItem> data;

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setData(List<DataItem> data) {
        this.data = data;
    }

    public List<DataItem> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "AppVersion{" + "status = '" + status + '\'' + ",msg = '" + msg + '\'' + ",data = '" + data + '\'' + "}";
    }
}