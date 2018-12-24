package com.irmsimapp.database;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.arch.lifecycle.LiveData;
import android.arch.paging.DataSource;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.irmsimapp.BuildConfig;
import com.irmsimapp.R;
import com.irmsimapp.activity.LoginActivity;
import com.irmsimapp.database.entity.ChatMessagesEntity;
import com.irmsimapp.database.entity.GroupModelEntity;
import com.irmsimapp.utils.AppExecutors;
import com.irmsimapp.utils.AppLog;
import com.irmsimapp.utils.MyApp;
import com.irmsimapp.utils.PreferenceHelper;
import com.irmsimapp.utils.Utils;

import java.util.Date;
import java.util.List;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import me.leolin.shortcutbadger.ShortcutBadger;

/**
 * Repository handling the work with products and comments.
 */
public class DataRepository {

    private static DataRepository sInstance;

    private final AppDatabase mDatabase;
    private String TAG = "DataRepository";

    private DataRepository(final AppDatabase database) {
        mDatabase = database;
    }

    public static DataRepository getInstance(final AppDatabase database) {
        if (sInstance == null) {
            synchronized (DataRepository.class) {
                if (sInstance == null) {
                    sInstance = new DataRepository(database);
                }
            }
        }
        return sInstance;
    }

    public void insertChatMessage(ChatMessagesEntity chatMessagesEntity) {

        new AppExecutors().diskIO().execute(() -> mDatabase.runInTransaction(() -> {
            if (mDatabase.chatMessagesDao().insertChatMessage(chatMessagesEntity) >= 0 && !chatMessagesEntity.getMsgFrom().equalsIgnoreCase(PreferenceHelper.getInstance().getLoginName())) {
                if (!Utils.isForeground(MyApp.getContext(), BuildConfig.APPLICATION_ID)) {
                    //  sendNotification(chatMessagesEntity, MyApp.getContext());
                }
            }
        }));
    }

    public void setGroupMessageRead(String groupId) {
        new AppExecutors().diskIO().execute(() -> mDatabase.runInTransaction(() -> mDatabase.chatMessagesDao().setGroupMessageRead(groupId)));
    }

    public void setPersonalMessageRead(String from) {
        new AppExecutors().diskIO().execute(() -> mDatabase.runInTransaction(() -> mDatabase.chatMessagesDao().setPersonalMessageRead(from)));
    }

    public void getTotalUnreadCounter(SingleObserver<Integer> SingleObserver) {
        mDatabase.chatMessagesDao().getTotalUnreadCounter().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(SingleObserver);
    }

    public void getGroupUnreadCounter(String groupId, SingleObserver<Integer> observer) {
        mDatabase.chatMessagesDao().getGroupUnreadCounter(groupId).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
    }

    public void getPersonalUnreadCounter(String from, SingleObserver<Integer> observer) {
        mDatabase.chatMessagesDao().getPersonalUnreadCounter(from).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
    }

    public void getLastMessageInGroupChat(String groupid, SingleObserver<ChatMessagesEntity> observer) {
        mDatabase.chatMessagesDao().getLastMessageInGroupChat(groupid).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
    }

    public void getLastMessageInIndividualChat(String from, String to, SingleObserver<ChatMessagesEntity> observer) {
        mDatabase.chatMessagesDao().getLastMessageInIndividualChat(from, to).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(observer);
    }

    public DataSource.Factory<Integer, ChatMessagesEntity> getMoreIndividualChatMessages(String from, String to) {
        return mDatabase.chatMessagesDao().getMoreIndividualChatMessages(from, to);
    }

    public DataSource.Factory<Integer, ChatMessagesEntity> getLoadMoreGroupChatMessagesFromDB(String groupid) {
        return mDatabase.chatMessagesDao().getMoreGroupChatMessages(groupid);
    }

    public LiveData<ChatMessagesEntity> getLatestMessage() {
        return mDatabase.chatMessagesDao().getLatestMessage();
    }

    public LiveData<List<GroupModelEntity>> getAllGroupModels() {
        return mDatabase.groupModelsDao().getAllGroupModels();
    }

    public void deleteAllGroupModels() {
        new AppExecutors().diskIO().execute(() -> mDatabase.runInTransaction(() -> mDatabase.groupModelsDao().clearAllGroupModels()));
    }

    public void setUploadFileMessage(String MsgId, long MsgTime) {
        new AppExecutors().diskIO().execute(() -> mDatabase.runInTransaction(() -> mDatabase.chatMessagesDao().setUploadFileMessage(MsgId, MsgTime)));
    }

    public void updateChatMessagesEntity(ChatMessagesEntity chatMessagesEntity) {
        new AppExecutors().diskIO().execute(() -> mDatabase.runInTransaction(() -> AppLog.Log(TAG + "update chat message", "" + mDatabase.chatMessagesDao().updateChatMessagesEntity(chatMessagesEntity))));
    }

    public void updateGroupModelEntity(GroupModelEntity groupModelEntity) {
        new AppExecutors().diskIO().execute(() -> mDatabase.runInTransaction(() -> mDatabase.groupModelsDao().updateGroupModel(groupModelEntity)));
    }

    public void insertGroupModel(GroupModelEntity groupModelEntity) {
        new AppExecutors().diskIO().execute(() -> mDatabase.runInTransaction(() -> mDatabase.groupModelsDao().insertGroupModel(groupModelEntity)));
    }

    private void sendNotification(ChatMessagesEntity message, Context context) {
        if (PreferenceHelper.getInstance().isLogin()) {
            getTotalUnreadCounter(new SingleObserver<Integer>() {
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
        }

        int notificationId = (int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE);
        String NOTIFICATION_CHANNEL_ID = context.getResources().getString(R.string.app_name);
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID).setPriority(Notification.PRIORITY_MAX).setDefaults(Notification.DEFAULT_ALL).setStyle(new NotificationCompat.BigTextStyle().bigText(message.getMessage()).setBigContentTitle(context.getResources().getString(R.string.app_name))).setContentTitle(context.getResources().getString(R.string.app_name)).setContentText(message.getMsgFrom() + " : " + message.getMessage()).setAutoCancel(true).setSmallIcon(R.mipmap.ic_launcher).setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(context.getPackageName(), notificationId, notificationBuilder.build());
        }
    }
}
