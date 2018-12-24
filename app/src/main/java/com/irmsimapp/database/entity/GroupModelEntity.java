package com.irmsimapp.database.entity;/*
 *
 * Copyright (c) by ElluminatiInc 2018 All rights reserved.
 *
 * Created by ElluminatiInc on 20/02/2018 15:11.
 *
 */

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;

import com.irmsimapp.Model.GroupUsers.UserListItem;

import java.util.List;

@Entity(tableName = "group_data_table", indices = @Index(value = {"groupNo"}, unique = true))
public class GroupModelEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "groupNo")
    private String groupNo;

    private String groupName;

    private String contactName;

    private String contactTitle;

    private String photoUrl;

    @TypeConverters(UserTypeConverter.class)
    private List<UserListItem> userList;

    private int unreadCount;

    private boolean isOpen;

    private boolean isGroupMember;

    @TypeConverters(ChatMessageConverter.class)
    private ChatMessagesEntity chatMessagesEntity;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGroupNo() {
        return groupNo;
    }

    public void setGroupNo(String groupNo) {
        this.groupNo = groupNo;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactTitle() {
        return contactTitle;
    }

    public void setContactTitle(String contactTitle) {
        this.contactTitle = contactTitle;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public List<UserListItem> getUserList() {
        return userList;
    }

    public void clearUserList() {
         userList.clear();
    }


    public void setUserList(List<UserListItem> userList) {
        this.userList = userList;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    public boolean isGroupMember() {
        return isGroupMember;
    }

    public void setGroupMember(boolean groupMember) {
        isGroupMember = groupMember;
    }

    public ChatMessagesEntity getChatMessagesEntity() {
        return chatMessagesEntity;
    }

    public void setChatMessagesEntity(ChatMessagesEntity chatMessagesEntity) {
        this.chatMessagesEntity = chatMessagesEntity;
    }



}
