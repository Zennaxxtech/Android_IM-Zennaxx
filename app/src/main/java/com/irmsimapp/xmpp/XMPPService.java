package com.irmsimapp.xmpp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ReconnectionManager;

public class XMPPService extends Service {
    private String TAG = "XMPPService";
    private XMPPConfiguration xmppConfiguration;
    public static AbstractXMPPConnection XMPPConnection;
    public static ReconnectionManager reconnectionManager;

    public XMPPService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        xmppConfiguration = XMPPConfiguration.getInstance();
        xmppConfiguration.initConnection();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        return Service.START_STICKY;
    }


    @Override
    public void onDestroy() {

        if (XMPPConnection != null) {
            if (XMPPConnection.isConnected()) {
                XMPPConnection.disconnect();
            }
            xmppConfiguration = null;
        }
        super.onDestroy();
    }
}
