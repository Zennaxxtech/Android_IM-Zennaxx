package com.irmsimapp.utils;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.irmsimapp.R;
import com.irmsimapp.database.AppDatabase;
import com.irmsimapp.database.DataRepository;
import com.testfairy.TestFairy;

import org.jivesoftware.smackx.muc.MultiUserChat;

import java.util.HashMap;


public class MyApp extends Application {

    private static Context mContext;
    private String TAG = "MyApp :";
    public HashMap<String, MultiUserChat> mGroupList;


    public static Context getContext() {
        return mContext;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(this);
//        TestFairy.begin(this, getResources().getString(R.string.testfairy_app_token));
        mContext = this.getApplicationContext();
        mGroupList = new HashMap<>();
    }

    private AppDatabase getDatabase() {
        return AppDatabase.getInstance(this);
    }

    public DataRepository getRepository() {
        return DataRepository.getInstance(getDatabase());
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

}
