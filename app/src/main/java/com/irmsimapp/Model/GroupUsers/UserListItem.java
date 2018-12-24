package com.irmsimapp.Model.GroupUsers;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.irmsimapp.database.entity.ChatMessagesEntity;

import java.io.Serializable;


public class UserListItem implements Serializable {

    @SerializedName("LoginName")
    @Expose
    private String loginName;

    @SerializedName("UserName")
    @Expose
    private String userName;

    @SerializedName("Email")
    @Expose
    private String email;

    @SerializedName("Phone")
    @Expose
    private String phone;

    @SerializedName("FullName")
    @Expose
    private String fullName;

    @SerializedName("Mobile")
    @Expose
    private String mobile;

    @SerializedName("PhotoUrl")
    @Expose
    private String photoUrl;

    @SerializedName("UserType")
    @Expose
    private String userType;

    private ChatMessagesEntity chatMessagesEntity;
    private int unreadCount;

    public UserListItem() {
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public ChatMessagesEntity getChatMessagesEntity() {
        return chatMessagesEntity;
    }

    public void setChatMessagesEntity(ChatMessagesEntity chatMessagesEntity) {
        this.chatMessagesEntity = chatMessagesEntity;
    }


    protected UserListItem(Parcel in) {
        loginName = in.readString();
        userName = in.readString();
        email = in.readString();
        phone = in.readString();
        fullName = in.readString();
        photoUrl = in.readString();
        userType = in.readString();
        mobile = in.readString();
    }


    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getUserType() {
        return userType;
    }

    @Override
    public String toString() {
        return
                "UserListItem{" +
                        "loginName = '" + loginName + '\'' +
                        ",userName = '" + userName + '\'' +
                        ",email = '" + email + '\'' +
                        ",phone = '" + phone + '\'' +
                        ",fullName = '" + fullName + '\'' +
                        ",photoUrl = '" + photoUrl + '\'' +
                        ",userType = '" + userType + '\'' +
                        "}";
    }


}