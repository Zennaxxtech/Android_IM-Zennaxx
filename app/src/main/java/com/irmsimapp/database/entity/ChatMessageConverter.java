package com.irmsimapp.database.entity;/*
 *
 * Copyright (c) by ElluminatiInc 2018 All rights reserved.
 *
 * Created by ElluminatiInc on 20/02/2018 15:19.
 *
 */

import android.arch.persistence.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class ChatMessageConverter {
    @TypeConverter
    public static ChatMessagesEntity stringToChatMessagesEntity(String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<ChatMessagesEntity>() {}.getType();
        return gson.fromJson(json, type);
    }

    @TypeConverter
    public static String chatMessagesEntityToString(ChatMessagesEntity list) {
        Gson gson = new Gson();
        Type type = new TypeToken<ChatMessagesEntity>() {}.getType();
        return gson.toJson(list, type);
    }
}
