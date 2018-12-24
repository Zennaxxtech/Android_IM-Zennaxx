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
import com.irmsimapp.Model.GroupUsers.UserListItem;

import java.lang.reflect.Type;
import java.util.List;

public class UserTypeConverter {
    @TypeConverter
    public static List<UserListItem> stringToUserListItem(String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<List<UserListItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    @TypeConverter
    public static String userListItemToString(List<UserListItem> list) {
        Gson gson = new Gson();
        Type type = new TypeToken<List<UserListItem>>() {}.getType();
        return gson.toJson(list, type);
    }
}
