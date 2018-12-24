package com.irmsimapp.Model.GroupUsers;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.irmsimapp.database.entity.ChatMessagesEntity;

import java.io.Serializable;
import java.util.List;

public class DataItem implements Serializable {

    @SerializedName("GroupNo")
    @Expose
    private String groupNo;

    @SerializedName("GroupName")
    @Expose
    private String groupName;

    @SerializedName("ContactTitle")
    @Expose
    private String contactTitle;

    @SerializedName("ContactName")
    @Expose
    private String contactName;



    @SerializedName("PhotoUrl")
    @Expose
    private String photoUrl;

    @SerializedName("UserList")
    @Expose
    private List<UserListItem> userList;

    private long lastMsgTime;
    private String lastMsgSenderName;
    private String lastMessage;
    private ChatMessagesEntity chatMessagesEntity;

    public ChatMessagesEntity getChatMessagesEntity() {
        return chatMessagesEntity;
    }

    public void setChatMessagesEntity(ChatMessagesEntity chatMessagesEntity) {
        this.chatMessagesEntity = chatMessagesEntity;
    }
    public String getContactTitle() {
        return contactTitle;
    }

    public void setContactTitle(String contactTitle) {
        this.contactTitle = contactTitle;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMsgSenderName() {
        return lastMsgSenderName;
    }

    public void setLastMsgSenderName(String lastMsgSenderName) {
        this.lastMsgSenderName = lastMsgSenderName;
    }


    public long getLastMsgTime() {
        return lastMsgTime;
    }

    public void setLastMsgTime(long lastMsgTime) {
        this.lastMsgTime = lastMsgTime;
    }

    public void setGroupNo(String groupNo) {
        this.groupNo = groupNo;
    }

    public String getGroupNo() {
        return groupNo;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }



    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setUserList(List<UserListItem> userList) {
        this.userList = userList;
    }

    public List<UserListItem> getUserList() {
        return userList;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}