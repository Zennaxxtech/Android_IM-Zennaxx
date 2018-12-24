package com.irmsimapp.ApiClient;

import com.irmsimapp.utils.AppLog;
import com.irmsimapp.utils.Utils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;

/*
 *
 * Copyright (c) by ElluminatiInc 2017 All rights reserved.
 *
 * Created by ElluminatiInc on 11/12/2017 12:20.
 *
 */
public class DecryptResponseInterceptorForOcean implements Interceptor {

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        final String content = Utils.decrypt(response.body().string());
        AppLog.Log("intercept", content + "");
        return response.newBuilder().body(ResponseBody.create(MediaType.parse("application/json; charset=utf-8"), content)).build();
    }
}
