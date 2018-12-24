package com.irmsimapp.ApiClient;

import android.util.Log;

import com.irmsimapp.BuildConfig;
import com.irmsimapp.utils.PreferenceHelper;
import com.irmsimapp.utils.Utils;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/*
 *
 * Copyright (c) by ElluminatiInc 2017 All rights reserved.
 *
 * Created by ElluminatiInc on 11/12/2017 12:20.
 *
 */
public class RequestParamInterceptorForLogin implements Interceptor {
    @Override
    public Response intercept(Chain chain)  {
        Request request = chain.request();
        HttpUrl url = request.url().newBuilder()
                .addQueryParameter("projectName", BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT.replace("_", ""))
                .addQueryParameter("SiteType", Utils.encrypt((BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT.replace("_", ""))))
                .addQueryParameter("AppsPlatform", Utils.encrypt("ANDROID"))
                .addQueryParameter("AppsVersionNo", Utils.encrypt(BuildConfig.VERSION_NAME))
                .addQueryParameter("AppsName", Utils.encrypt(BuildConfig.APPSTYPE))
                .addQueryParameter("Environment", Utils.encrypt(BuildConfig.ENVIROMENT.replace("_", "").replace("-", "")))
                .build();
        request = request.newBuilder().url(url).build();
        try {
            return chain.proceed(request);
        } catch (IOException e) {
            Log.e("RequestParamInterceptor",e.getMessage());
            return null;
        }
    }
}
