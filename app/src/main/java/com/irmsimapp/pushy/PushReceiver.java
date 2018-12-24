package com.irmsimapp.pushy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.irmsimapp.BuildConfig;
import com.irmsimapp.R;
import com.irmsimapp.activity.LoginActivity;
import com.irmsimapp.utils.AppLog;
import com.irmsimapp.utils.MyApp;
import com.irmsimapp.utils.PreferenceHelper;
import com.irmsimapp.utils.Utils;
import com.irmsimapp.xmpp.XMPPService;

import java.util.Date;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import me.leolin.shortcutbadger.ShortcutBadger;
import me.pushy.sdk.Pushy;

/**
 * Copyright (c) by ElluminatiInc 2018 All rights reserved.
 * <p>
 * Created by ElluminatiInc on 24/01/2018 10:21.
 **/
public class PushReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String notificationText = "";
        if (intent.getStringExtra("message") != null) {
            notificationText = intent.getStringExtra("message");
            AppLog.Log("PushReceiver ", notificationText);
        }

        AppLog.Log("PushReceiver ", "isLogin = " + PreferenceHelper.getInstance().isLogin() + " isForeground = " + Utils.isForeground(MyApp.getContext(), BuildConfig.APPLICATION_ID));
        if (PreferenceHelper.getInstance().isLogin() && !Utils.isForeground(MyApp.getContext(), BuildConfig.APPLICATION_ID)) {

            sendNotification(notificationText, context);
        }
    }

    private void sendNotification(String message, Context context) {
        // Prepare a notification with vibration, sound and lights
        ((MyApp) MyApp.getContext()).getRepository().getTotalUnreadCounter(new SingleObserver<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(Integer integer) {
                ShortcutBadger.applyCount(context, integer);
            }

            @Override
            public void onError(Throwable e) {
                ShortcutBadger.applyCount(context, 0);
            }
        });

        // Prepare a notification with vibration, sound and lights
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message).setBigContentTitle(context.getResources().getString(R.string.app_name)))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getResources().getString(R.string.app_name))
                .setContentText(message)
                .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, LoginActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));

        // Automatically configure a Notification Channel for devices running Android O+
        Pushy.setNotificationChannel(builder, context);

        // Get an instance of the NotificationManager service
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

        // Build the notification and display it
        notificationManager.notify(1, builder.build());

        if (XMPPService.XMPPConnection == null && PreferenceHelper.getInstance().isLogin()) {
            /* context.startService(new Intent(context, XMPPService.class));*/
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, XMPPService.class));
            } else {
                context.startService(new Intent(context, XMPPService.class));
            }
        }
    }

}