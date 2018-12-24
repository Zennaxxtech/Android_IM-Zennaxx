package com.irmsimapp.xmpp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.StrictMode;

import com.irmsimapp.ApiClient.DownloadFileTask;
import com.irmsimapp.BuildConfig;
import com.irmsimapp.activity.GroupListActivity;
import com.irmsimapp.database.DataRepository;
import com.irmsimapp.database.entity.ChatMessagesEntity;
import com.irmsimapp.datamodel.BadKeyWordsModel;
import com.irmsimapp.interfaces.XMPPErrorListener;
import com.irmsimapp.interfaces.XMPPListener;
import com.irmsimapp.utils.AppLog;
import com.irmsimapp.utils.MyApp;
import com.irmsimapp.utils.PreferenceHelper;
import com.irmsimapp.utils.Utils;

import org.apache.commons.lang3.StringUtils;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.muc.MucEnterConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;

import static com.irmsimapp.BuildConfig.OPENFIRE_HOST_SERVER_RESOURCE;


public class XMPPConfiguration implements PingFailedListener {

    private static XMPPTCPConnectionConfiguration config;
    private XMPPListener xmppListener;
    private String TAG = "XMPPConfiguration :";
    private static XMPPConfiguration instance;
    private XMPPErrorListener xmppErrorListener;


    private XMPPConfiguration() {
    }

    public static synchronized XMPPConfiguration getInstance() {
        if (instance == null) {
            instance = new XMPPConfiguration();
        }
        return instance;
    }

    void initConnection() {
        if (XMPPService.XMPPConnection == null) {
            new OpenFireLoginTask().execute();
        }
    }


    @Override
    public void pingFailed() {

    }

    public class OpenFireLoginTask extends AsyncTask<String, String, String> {

        OpenFireLoginTask() {
        }


        @Override
        protected String doInBackground(String... strings) {
            try {

                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);

                InetAddress address = InetAddress.getByName(PreferenceHelper.getInstance().getOpenfireHost());
                HostnameVerifier verifier = (hostname, session) -> false;
                DomainBareJid serviceName = JidCreate.domainBareFrom(PreferenceHelper.getInstance().getOpenFireJIDSuffix().replace("@", ""));
                XMPPConfiguration.config = XMPPTCPConnectionConfiguration.builder()
                        .setUsernameAndPassword(PreferenceHelper.getInstance().getOpenfireusername().toLowerCase(), PreferenceHelper.getInstance().getPassword())
                        .setHost(PreferenceHelper.getInstance().getOpenfireHost())
                        .setResource(OPENFIRE_HOST_SERVER_RESOURCE)
                        .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                        .setXmppDomain(serviceName)
                        .setHostAddress(address)
                        .setPort(Integer.parseInt(PreferenceHelper.getInstance().getOpenfireXmppPort()))
                        .setCompressionEnabled(false)
                        .setSendPresence(true)
                        .setHostnameVerifier(verifier)
                        .setDebuggerEnabled(true)// to view what's happening in detail
                        .setConnectTimeout(100000)
                        .build();

                XMPPService.XMPPConnection = new XMPPTCPConnection(config);
//                XMPPService.XMPPConnection.setPacketReplyTimeout(100000);
                XMPPService.XMPPConnection.addConnectionListener(new XMPPConnectionListener());
                XMPPTCPConnection.setUseStreamManagementResumptionDefault(true);
                XMPPTCPConnection.setUseStreamManagementDefault(true);

                XMPPService.XMPPConnection.connect();
                XMPPService.XMPPConnection.login(PreferenceHelper.getInstance().getOpenfireusername().toLowerCase(), PreferenceHelper.getInstance().getPassword());
                AppLog.Log("doInBackground getOpenfireusername :", PreferenceHelper.getInstance().getOpenfireusername().toLowerCase());
                AppLog.Log("doInBackground password :", PreferenceHelper.getInstance().getPassword());
            } catch (XmppStringprepException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (SASLErrorException e) {
                e.printStackTrace();
                return "authenticationFailed";
            } catch (IOException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (SmackException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (XMPPException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return e.getMessage();
            }
            return "";
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Utils.hideCustomProgressDialog();
            if (StringUtils.equals(s, "authenticationFailed")) {
                AppLog.Log(TAG, s);
                if (xmppListener != null) {
                    xmppListener.authenticateFailed();
                }
            } else if (StringUtils.isNotEmpty(s)) {
                if (xmppListener != null) {
                    xmppListener.authenticationError(s);
                }
            } else {
                if (XMPPService.XMPPConnection.isAuthenticated()) {
                    XMPPService.reconnectionManager = ReconnectionManager.getInstanceFor(XMPPService.XMPPConnection);
                    XMPPService.reconnectionManager.setFixedDelay(60);
                    XMPPService.reconnectionManager.enableAutomaticReconnection();
                }
            }
        }
    }


    public class XMPPConnectionListener implements ConnectionListener {

        @Override
        public void connected(org.jivesoftware.smack.XMPPConnection connection) {

        }

        @Override
        public void authenticated(org.jivesoftware.smack.XMPPConnection connection, boolean resumed) {
            try {
                if (xmppListener != null) {
                    xmppListener.authenticatedSuccessfully();
                }

                StanzaFilter filter = new StanzaTypeFilter(Message.class);
                connection.addAsyncStanzaListener(packet -> {
                    Message message = (Message) packet;
                    if (message.getBody() != null) {
                        saveMessagesInDataBase(message.getFrom().asEntityBareJidIfPossible(), message);
                    }
                }, filter);

                if (Utils.isForeground(MyApp.getContext(), BuildConfig.APPLICATION_ID)) {
                    BadKeyWordsModel.getBadWordsFromServer();
                    Intent intent = new Intent(MyApp.getContext(), GroupListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    MyApp.getContext().startActivity(intent);
                }

                final MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(XMPPService.XMPPConnection);
                HashSet<String> groups = PreferenceHelper.getInstance().getGroups();
                if (groups != null) {
                    for (String groupName : groups) {
                        String groupname = PreferenceHelper.getInstance().getCompanyName() + BuildConfig.ENVIROMENT + groupName.toLowerCase().trim();
                        AppLog.Log("authenticated groupname :", groupname);
                        EntityBareJid jid = null;
                        try {
                            jid = JidCreate.entityBareFrom(groupname + "@" + PreferenceHelper.getInstance().getOpenFireConferenceService());
                            AppLog.Log("authenticated jid :", jid.toString());
                            MultiUserChat mMultiUserChat = manager.getMultiUserChat(jid);
//                            mMultiUserChat.grantMembership(jid);
                            Date date = new Date();
                            date.setTime(date.getTime() - (24 * 60 * 60 * 1000));
                            AppLog.Log("authenticated date :", date.toString());
                            final MucEnterConfiguration.Builder builder = mMultiUserChat.getEnterConfigurationBuilder(Resourcepart.from(PreferenceHelper.getInstance().getOpenfireusername().replace("@", "@"))).requestHistorySince(date).withPresence(new Presence(Presence.Type.available));
                            AppLog.Log("authenticated mMultiUserChat.getEnterConfigurationBuilder() :", builder.toString());
                            if (mMultiUserChat != null && XMPPService.XMPPConnection.isAuthenticated()) {
                                try {
                                    mMultiUserChat.join(builder.build());
                                } catch (XMPPException.XMPPErrorException e) {
                                    e.printStackTrace();
                                    AppLog.Error(TAG, e.getMessage());
                                } catch (SmackException.NoResponseException e) {
                                    AppLog.Error(TAG, e.getMessage());
                                    e.printStackTrace();
                                } catch (SmackException.NotConnectedException e) {
                                    AppLog.Error(TAG, e.getMessage());
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    AppLog.Error(TAG, e.getMessage());
                                    e.printStackTrace();
                                } catch (MultiUserChatException.NotAMucServiceException e) {
                                    AppLog.Error(TAG, e.getMessage());
                                    e.printStackTrace();
                                } catch (NullPointerException e) {
                                    AppLog.Error(TAG, e.getMessage());
                                    e.printStackTrace();
                                } catch (Exception e) {
                                    AppLog.Error(TAG, e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        } catch (XmppStringprepException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        @Override
        public void connectionClosed() {
            if (xmppErrorListener != null) {
                xmppErrorListener.connectionClosedOnError();
            }
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            boolean error = StringUtils.containsAny(e.getMessage(), "stream:error (conflict)", "conflict", "<stream:error><conflict");
            if (error) {
                xmppErrorListener.connectionClosedOnConflict();
            } else {
                xmppErrorListener.connectionClosedOnError();
            }

            try {
                XMPPService.XMPPConnection.disconnect(new Presence(Presence.Type.unavailable));
            } catch (SmackException.NotConnectedException e1) {
                e1.printStackTrace();
            }

        }

        @Override
        public void reconnectionSuccessful() {
            AppLog.Log(TAG, " reconnectionSuccessful");

        }

        @Override
        public void reconnectingIn(int seconds) {
            AppLog.Log(TAG, " reconnectingIn " + seconds);
            try {
                XMPPService.XMPPConnection.login(PreferenceHelper.getInstance().getOpenfireusername().toLowerCase(), PreferenceHelper.getInstance().getPassword());
            } catch (XMPPException e) {
                e.printStackTrace();
            } catch (SmackException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void reconnectionFailed(Exception e) {
            AppLog.Log(TAG, " reconnectionFailed" + e.getMessage());
            AppLog.Log(TAG, " reconnectionFailed" + e.getLocalizedMessage());
            try {
                XMPPService.XMPPConnection.login(PreferenceHelper.getInstance().getOpenfireusername().toLowerCase(), PreferenceHelper.getInstance().getPassword());
            } catch (XMPPException e1) {
                e1.printStackTrace();
            } catch (SmackException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

    }

    public void setXMPPInterface(XMPPListener xmppInterface) {
        this.xmppListener = xmppInterface;
    }


    public void setXmppErrorListener(XMPPErrorListener xmppErrorListener) {
        this.xmppErrorListener = xmppErrorListener;
    }


    private void saveMessagesInDataBase(EntityBareJid from, Message message) {
        AppLog.Log(TAG, " saveMessagesInDataBase() EntityBareJid :" + from + ", Message :" + message);

        try {
            DataRepository dataRepository = ((MyApp) MyApp.getContext()).getRepository();
            StandardExtensionElement sender_jid = message.getExtension("sender_jid", "urn:xmpp:sender_jid");
            final ChatMessagesEntity entity = new ChatMessagesEntity();
            final StandardExtensionElement message_data = message.getExtension("message_data", "urn:xmpp:message_data");
            entity.setMessage(message.getBody());
            entity.setMsgTime(Long.parseLong(message_data.getAttributeValue("msg_time")));
            DelayInformation inf = null;
            inf = message.getExtension(DelayInformation.ELEMENT, DelayInformation.NAMESPACE);
            if (inf != null) {
                Date date = inf.getStamp();
                entity.setMsgTime(date.getTime());
            }
            entity.setSenderProfileUrl(message_data.getAttributeValue("sender_image_url"));
            entity.setFullName(message_data.getAttributeValue("sender_fullname"));
            entity.setMsgId(message.getStanzaId());
            entity.setChatType(message.getType().toString());
            if (message.getType() == Message.Type.groupchat) {
                entity.setMsgFromGroupMember(sender_jid.getAttributeValue("sender_id").replace(PreferenceHelper.getInstance().getOpenFireJIDSuffix(), "").replace(PreferenceHelper.getInstance().getCompanyName() + BuildConfig.ENVIROMENT, "").replace("#", "@"));
                entity.setGroupId(from.getLocalpart().toString().replace("#", "@").replace(PreferenceHelper.getInstance().getCompanyName() + BuildConfig.ENVIROMENT, ""));
                entity.setMsgTo(message.getFrom().asBareJid().getLocalpartOrThrow().toString().replace("#", "@").replace(PreferenceHelper.getInstance().getCompanyName() + BuildConfig.ENVIROMENT, ""));
            } else {
                entity.setMsgTo(PreferenceHelper.getInstance().getLoginName());
                entity.setMsgFromGroupMember("");
            }
            entity.setMsgFrom(from.getLocalpart().toString().replace("#", "@").replace(PreferenceHelper.getInstance().getCompanyName() + BuildConfig.ENVIROMENT, ""));
            entity.setMediaPath("");
            entity.setMsgType("chat");
            entity.setRead(false);
            if (message_data.getAttributeValue("type_of_chat").equalsIgnoreCase("image") || message_data.getAttributeValue("type_of_chat").equalsIgnoreCase("video") || message_data.getAttributeValue("type_of_chat").equalsIgnoreCase("audio")) {
                String fileURLThumb = "";
                String[] fileUrl = message_data.getAttributeValue("content_url").split(",");
                if (!fileUrl[0].startsWith("http")) {
                    fileURLThumb = "http://" + fileUrl[0];
                } else {
                    fileURLThumb = fileUrl[0];
                }
                entity.setMediaUrl(message_data.getAttributeValue("content_url"));
                entity.setThumbnailUrl(fileURLThumb);
                entity.setMsgType(message_data.getAttributeValue("type_of_chat"));
                if (message_data.getAttributeValue("type_of_chat").equalsIgnoreCase("audio") || message_data.getAttributeValue("type_of_chat").equalsIgnoreCase("video")) {
                    new DownloadFileTask(response -> {
                        entity.setMediaPath(response);
                        dataRepository.insertChatMessage(entity);
                    }, MyApp.getContext()).executeOnExecutor(Executors.newSingleThreadExecutor(), fileURLThumb);
                } else {
                    dataRepository.insertChatMessage(entity);
                }
            } else {
                dataRepository.insertChatMessage(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fileTransfer() {

        // Create the file transfer manager
        final FileTransferManager manager = FileTransferManager.getInstanceFor(XMPPService.XMPPConnection);
        // Create the listener
        manager.addFileTransferListener(request -> {

            // Check to see if the request should be accepted
            if (true) {
                // Accept it
                IncomingFileTransfer transfer = request.accept();
                AppLog.Log(TAG, request.getFileName());
                AppLog.Log(TAG, request.getMimeType());
                AppLog.Log(TAG, request.getDescription());
                AppLog.Log(TAG, request.getRequestor().toString());
                try {
                    transfer.recieveFile(new File(request.getFileName()));
                } catch (SmackException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // Reject it
                try {
                    request.reject();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
