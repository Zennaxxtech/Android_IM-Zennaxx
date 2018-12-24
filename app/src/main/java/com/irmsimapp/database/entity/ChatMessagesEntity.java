/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irmsimapp.database.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;

import com.irmsimapp.utils.DeliveryState;

@Entity(tableName = "chat_message_table", indices = @Index(value = {"MsgId"}, unique = true))
public class ChatMessagesEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "MsgId")
    private String MsgId;
    private String MsgFrom;
    private String MsgTo;
    private String Message;
    private long MsgTime;
    private String SenderProfileUrl;
    private String MediaPath;
    private String MsgType;
    private String ChatType;
    private String groupId;
    private String MsgFromGroupMember;
    private String MediaUrl;
    private String ThumbnailUrl;
    private String fullName;
    private boolean isUploadingFile;
    @TypeConverters(DeliveryStateConverter.class)
    private DeliveryState deliveryStatus = DeliveryState.DeliveryStateFailure;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isUploadingFile() {
        return isUploadingFile;
    }

    public DeliveryState getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(DeliveryState deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public void setUploadingFile(boolean uploadingFile) {
        isUploadingFile = uploadingFile;
    }

    public String getMsgId() {
        return MsgId;
    }

    public void setMsgId(String msgId) {
        MsgId = msgId;
    }

    public String getMsgFrom() {
        return MsgFrom;
    }

    public void setMsgFrom(String msgFrom) {
        MsgFrom = msgFrom;
    }

    public String getMsgTo() {
        return MsgTo;
    }

    public void setMsgTo(String msgTo) {
        MsgTo = msgTo;
    }

    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }

    public long getMsgTime() {
        return MsgTime;
    }

    public void setMsgTime(long msgTime) {
        MsgTime = msgTime;
    }

    public String getSenderProfileUrl() {
        return SenderProfileUrl;
    }

    public void setSenderProfileUrl(String senderProfileUrl) {
        SenderProfileUrl = senderProfileUrl;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getMediaPath() {
        return MediaPath;
    }

    public void setMediaPath(String mediaPath) {
        MediaPath = mediaPath;
    }

    public String getMsgType() {
        return MsgType;
    }

    public void setMsgType(String msgType) {
        MsgType = msgType;
    }

    public String getChatType() {
        return ChatType;
    }

    public void setChatType(String chatType) {
        ChatType = chatType;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getMsgFromGroupMember() {
        return MsgFromGroupMember;
    }

    public void setMsgFromGroupMember(String msgFromGroupMember) {
        MsgFromGroupMember = msgFromGroupMember;
    }

    public String getMediaUrl() {
        return MediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        MediaUrl = mediaUrl;
    }

    public String getThumbnailUrl() {
        return ThumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        ThumbnailUrl = thumbnailUrl;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    private boolean isRead;


}
