package com.irmsimapp.ApiClient;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.irmsimapp.utils.PreferenceHelper;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.irmsimapp.BuildConfig.BASE_URL;
import static com.irmsimapp.BuildConfig.BASE_URL_OPENFIRE;

public class ApiHandler {
    private static final String TAG = "[ApiHandler]";
    private static final int CONNECTION_TIMEOUT = 60; //seconds
    private static final int READ_TIMEOUT = 50; //seconds
    private static final int WRITE_TIMEOUT = 50;//seconds
    private static Webservices apiServiceCommon, apiService, apiServiceUploadFile, apiServiceOpenFire;
    private static Gson gson = new GsonBuilder().setLenient().create();

    public static Webservices getCommonApiService() {

        Log.e(TAG, "BASE_URL : " + BASE_URL);
        if (apiServiceCommon == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                    .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .addInterceptor(new RequestParamInterceptor())
                    .addInterceptor(new DecryptResponseInterceptor())
                    .addInterceptor(interceptor)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .client(okHttpClient)
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            apiServiceCommon = retrofit.create(Webservices.class);
            return apiServiceCommon;
        } else {
            return apiServiceCommon;
        }
    }

    public static Webservices getApiService() {
        Log.e(TAG, "BASE_URL : " + BASE_URL);

        if (apiService == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                    .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .addInterceptor(new DecryptResponseInterceptorForOcean())
                    .addInterceptor(interceptor)
                    .build();
            Retrofit retrofit = new Retrofit.Builder()
                    .client(okHttpClient)
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            apiService = retrofit.create(Webservices.class);
            return apiService;
        } else {
            return apiService;
        }
    }


    //this is for increase time for read and write time out to upload file in chat.
    public static Webservices getUploadFileApiService() {

        if (apiServiceUploadFile == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
            OkHttpClient okHttpClient = new OkHttpClient().newBuilder().connectTimeout(120, TimeUnit.SECONDS).retryOnConnectionFailure(true).readTimeout(110, TimeUnit.SECONDS).writeTimeout(110, TimeUnit.SECONDS).addInterceptor(new RequestParamInterceptor()).addInterceptor(new DecryptResponseInterceptor()).addInterceptor(interceptor).build();
            Retrofit retrofit = new Retrofit.Builder().client(okHttpClient).baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create(gson)).build();
            apiServiceUploadFile = retrofit.create(Webservices.class);
            return apiServiceUploadFile;
        } else {
            return apiServiceUploadFile;
        }
    }


    public static Webservices getOpenfireApiService() {
        if (apiServiceOpenFire == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient okHttpClient = new OkHttpClient().newBuilder().connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS).retryOnConnectionFailure(true).readTimeout(READ_TIMEOUT, TimeUnit.SECONDS).writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS).addInterceptor(interceptor).build();
            Retrofit retrofit = new Retrofit.Builder().client(okHttpClient).baseUrl(PreferenceHelper.getInstance().getOpenfireHttpRoot()+"/").addConverterFactory(GsonConverterFactory.create(gson)).build();
            apiServiceOpenFire = retrofit.create(Webservices.class);
            return apiServiceOpenFire;
        } else {
            return apiServiceOpenFire;
        }
    }

    @NonNull
    public static MultipartBody.Part makeMultipartRequestBody(Uri imageUri, String fileType) {
        File file = new File(imageUri.getPath());
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        return MultipartBody.Part.createFormData(fileType/*"Imgparameter"*/, file.getName(), requestFile);
    }
}