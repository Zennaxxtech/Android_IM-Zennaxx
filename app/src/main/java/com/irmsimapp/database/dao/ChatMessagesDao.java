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

package com.irmsimapp.database.dao;


import android.arch.lifecycle.LiveData;
import android.arch.paging.DataSource;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.irmsimapp.database.entity.ChatMessagesEntity;

import io.reactivex.Single;


@Dao
public interface ChatMessagesDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertChatMessage(ChatMessagesEntity chatMessagesEntity);

    @Query("SELECT * FROM chat_message_table  where ( lower(MsgFrom) = lower(:from) AND lower(MsgTo) =lower(:to)) OR (lower(MsgFrom) = lower(:to) AND lower(MsgTo) =lower(:from))  ORDER BY MsgTime ASC")
    DataSource.Factory<Integer, ChatMessagesEntity> getMoreIndividualChatMessages(String from, String to);

    @Query("SELECT * FROM chat_message_table  where lower(groupId) = lower(:groupid) ORDER BY MsgTime ASC")
    DataSource.Factory<Integer, ChatMessagesEntity> getMoreGroupChatMessages(String groupid);

    @Query("select * from chat_message_table where MsgTime=(select max(MsgTime) from chat_message_table where lower(groupId)=lower(:groupid)) limit 1")
    Single<ChatMessagesEntity> getLastMessageInGroupChat(String groupid);

    @Query("select * from chat_message_table where MsgTime=(select max(MsgTime) from chat_message_table where (lower(MsgFrom)=lower(:from) AND lower(MsgTo)=lower (:to)) OR (lower(MsgFrom)=lower(:to) AND lower(MsgTo)=lower (:from))) limit 1")
    Single<ChatMessagesEntity> getLastMessageInIndividualChat(String from, String to);

    @Query("UPDATE chat_message_table SET isRead = 1 where lower(groupId) = lower(:groupId)")
    void setGroupMessageRead(String groupId);

    @Query("UPDATE chat_message_table SET isRead = 1 where lower(MsgFrom) = lower(:from)")
    void setPersonalMessageRead(String from);

    @Query("UPDATE chat_message_table SET isUploadingFile = 0,MsgTime=:MsgTime where MsgId = :MsgId")
    void setUploadFileMessage(String MsgId, long MsgTime);

    @Query("select COUNT (*) as count from chat_message_table where  lower(MsgFrom)=lower(:from) and isRead = 0")
    Single<Integer> getPersonalUnreadCounter(String from);

    @Query("select COUNT (*) as count from chat_message_table where isRead = 0")
    Single<Integer> getTotalUnreadCounter();

    @Query("select COUNT (*) as count from chat_message_table where lower(groupId) = lower(:groupId) and isRead= 0")
    Single<Integer> getGroupUnreadCounter(String groupId);

    @Query("select * from chat_message_table where MsgTime=(select max(MsgTime) from chat_message_table)")
    LiveData<ChatMessagesEntity> getLatestMessage();

    @Update
    int updateChatMessagesEntity(ChatMessagesEntity chatMessagesEntity);

}
