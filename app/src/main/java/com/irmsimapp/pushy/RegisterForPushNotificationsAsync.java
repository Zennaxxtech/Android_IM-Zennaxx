package com.irmsimapp.pushy;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.irmsimapp.utils.PreferenceHelper;

import me.pushy.sdk.Pushy;

/**
 * Copyright (c) by ElluminatiInc 2018 All rights reserved.
 * <p>
 * Created by ElluminatiInc on 24/01/2018 10:13.
 **/
public class RegisterForPushNotificationsAsync extends AsyncTask<Context, Void, Exception> {
    private Context context;

    protected Exception doInBackground(Context... params) {

        try {
            // Assign a unique token to this device
            this.context = params[0];
            String deviceToken = Pushy.register(params[0]);
            PreferenceHelper.getInstance().putDeviceToken(deviceToken);
           // Toast.makeText(context, deviceToken, Toast.LENGTH_LONG).show();

        } catch (Exception exc) {
            // Return exc to onPostExecute
            return exc;
        }

        // Success
        return null;
    }

    @Override
    protected void onPostExecute(Exception exc) {
        // Failed?
        if (exc != null) {
            // Show error as toast message
           // Toast.makeText(context, exc.toString(), Toast.LENGTH_LONG).show();
            Log.d("RegisterForPushNoti", "onPostExecute: " + exc.toString());
            return;
        }
        PreferenceHelper.getInstance().putIsDone(true);
        // Succeeded, do something to alert the user
    }
}