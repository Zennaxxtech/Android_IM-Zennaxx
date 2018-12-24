package com.irmsimapp.database.entity;/*
 *
 * Copyright (c) by ElluminatiInc 2018 All rights reserved.
 *
 * Created by ElluminatiInc on 20/02/2018 15:19.
 *
 */

import android.arch.persistence.room.TypeConverter;

import com.irmsimapp.utils.DeliveryState;

public class DeliveryStateConverter {
    @TypeConverter
    public static DeliveryState toStatus(int ordinal) {
        return DeliveryState.values()[ordinal];
    }

    @TypeConverter
    public static Integer toOrdinal(DeliveryState status) {
        return status.ordinal();
    }
}
