/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irmsimapp.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;

import com.irmsimapp.ApiClient.ApiHandler;
import com.irmsimapp.BuildConfig;
import com.irmsimapp.Model.GroupUsers.DataItem;
import com.irmsimapp.Model.GroupUsers.GroupUser;
import com.irmsimapp.database.entity.ChatMessagesEntity;
import com.irmsimapp.database.entity.GroupModelEntity;
import com.irmsimapp.utils.AppLog;
import com.irmsimapp.utils.Const;
import com.irmsimapp.utils.MyApp;
import com.irmsimapp.utils.PreferenceHelper;
import com.irmsimapp.utils.Utils;
import com.irmsimapp.xmpp.XMPPService;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.MucEnterConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import retrofit2.Callback;
import retrofit2.Response;

import static com.irmsimapp.BuildConfig.OPENFIRE_HOST_SERVER_CONFERENCE_SERVICE;

public class GroupListViewModel extends AndroidViewModel {

    private final MediatorLiveData<ChatMessagesEntity> mObservableChatMessage;
    private final MediatorLiveData<List<GroupModelEntity>> mObservableGroupModels;
    private String TAG = "GroupListViewModel";


    public GroupListViewModel(Application application) {
        super(application);
        mObservableChatMessage = new MediatorLiveData<>();
        mObservableGroupModels = new MediatorLiveData<>();
        mObservableChatMessage.setValue(null);
        mObservableGroupModels.setValue(null);
        getMessages();
        getGroupModels();
    }

    private void getMessages() {
        LiveData<ChatMessagesEntity> message = ((MyApp) getApplication()).getRepository().getLatestMessage();
        mObservableChatMessage.addSource(message, mObservableChatMessage::setValue);
    }

    private void getGroupModels() {
        LiveData<List<GroupModelEntity>> allGroupModels = ((MyApp) getApplication()).getRepository().getAllGroupModels();
        mObservableGroupModels.addSource(allGroupModels, mObservableGroupModels::setValue);
    }


    public LiveData<ChatMessagesEntity> getMessage() {
        return mObservableChatMessage;
    }

    public LiveData<List<GroupModelEntity>> getAllGroupModels() {
        return mObservableGroupModels;
    }


    public void loadGroupList(SwipeRefreshLayout swipeRefreshLayout) {
        if (Utils.isInternetConnected()) {
            PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
            HashMap<String, String> group_user_param = new HashMap<>();
            group_user_param.put(Const.intentKey.USER_NAME, Utils.encrypt(preferenceHelper.getUsername()));
            group_user_param.put(Const.intentKey.USER_TYPE, Utils.encrypt(preferenceHelper.getUserType()));
            group_user_param.put("AppsType", Utils.encrypt(BuildConfig.APPSTYPE));
            retrofit2.Call<GroupUser> groupUserCall;
            if (BuildConfig.COMPANYNAME.equalsIgnoreCase("innoways")) {
                groupUserCall = ApiHandler.getCommonApiService().getGroupUsersAspx(group_user_param);
            } else {
                groupUserCall = ApiHandler.getCommonApiService().getGroupUsers(group_user_param);
            }
            groupUserCall.enqueue(new Callback<GroupUser>() {
                @Override
                public void onResponse(retrofit2.Call<GroupUser> call, Response<GroupUser> response) {
                    GroupUser groupUser = response.body();
                    swipeRefreshLayout.setRefreshing(false);
                    if (XMPPService.XMPPConnection == null) {
                        return;
                    }
                    final MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(XMPPService.XMPPConnection);
                    if (groupUser != null && groupUser.getStatus().equalsIgnoreCase("1")) {
                        HashSet<String> groupsSet = new HashSet<>();
                        // here clear all group from database
                        ((MyApp) getApplication()).getRepository().deleteAllGroupModels();

                        Observable<GroupModelEntity> groupModelEntityObservable = Observable.create(emitter -> {
                            for (DataItem dataItem : groupUser.getData()) {
                                groupsSet.add(dataItem.getGroupNo());
                                GroupModelEntity groupModelEntity = new GroupModelEntity();
                                groupModelEntity.setGroupMember(false);
                                groupModelEntity.setGroupNo(dataItem.getGroupNo());
                                groupModelEntity.setGroupName(dataItem.getGroupName());
                                groupModelEntity.setPhotoUrl(dataItem.getPhotoUrl());
                                groupModelEntity.setOpen(false);
                                groupModelEntity.setContactName(dataItem.getContactName());
                                groupModelEntity.setContactTitle(dataItem.getContactTitle());
                                groupModelEntity.setUserList(dataItem.getUserList());
                                ((MyApp) getApplication()).getRepository().getLastMessageInGroupChat(dataItem.getGroupNo().toLowerCase(), new SingleObserver<ChatMessagesEntity>() {
                                    @Override
                                    public void onSubscribe(Disposable d) {

                                    }

                                    @Override
                                    public void onSuccess(ChatMessagesEntity chatMessagesEntity) {
                                        AppLog.Log(TAG, " onSuccess " + chatMessagesEntity.getMessage());
                                        groupModelEntity.setChatMessagesEntity(chatMessagesEntity);
                                        String groupname = BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT + dataItem.getGroupNo().toLowerCase().trim();
                                        EntityBareJid jid = null;

                                        try {
//                                            jid = JidCreate.entityBareFrom(groupname + "@" + OPENFIRE_HOST_SERVER_CONFERENCE_SERVICE);
                                            jid = JidCreate.entityBareFrom(groupname + "@" + PreferenceHelper.getInstance().getOpenFireConferenceService());
                                            if (jid != null) {

                                                Log.e(TAG, "");
                                                MultiUserChat mMultiUserChat = manager.getMultiUserChat(jid);
//                                                mMultiUserChat.grantMembership(jid);
                                                Date date = new Date();
                                                date.setTime(chatMessagesEntity.getMsgTime());
                                                final MucEnterConfiguration.Builder builder = mMultiUserChat.getEnterConfigurationBuilder(Resourcepart.from(PreferenceHelper.getInstance().getOpenfireusername().replace("@", "#")))
                                                        .requestHistorySince(date)
                                                        .withPresence(new Presence(Presence.Type.available));
                                                mMultiUserChat.join(builder.build());
                                            }else{
                                                AppLog.Log(TAG, " onSuccess jid is null ");
                                            }
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
                                        } catch (XmppStringprepException e) {
                                            e.printStackTrace();
                                        } catch (NullPointerException e) {
                                            e.printStackTrace();
                                        }
                                        emitter.onNext(groupModelEntity);

                                    }

                                    @Override
                                    public void onError(Throwable e1) {
                                        String groupname = BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT + dataItem.getGroupNo().toLowerCase().trim();
                                        EntityBareJid jid = null;

                                        try {

//                                            jid = JidCreate.entityBareFrom(groupname + "@" + OPENFIRE_HOST_SERVER_CONFERENCE_SERVICE);
                                            jid = JidCreate.entityBareFrom(groupname + "@" + PreferenceHelper.getInstance().getOpenFireConferenceService());
                                            if (jid != null) {
                                                MultiUserChat mMultiUserChat = manager.getMultiUserChat(jid);
//                                                mMultiUserChat.grantMembership(jid);
                                                Date date = new Date();
                                                date.setTime(date.getTime() - (24 * 60 * 60 * 1000));
                                                final MucEnterConfiguration.Builder builder = mMultiUserChat.getEnterConfigurationBuilder(Resourcepart.from(PreferenceHelper.getInstance().getOpenfireusername().replace("@", "#"))).requestHistorySince(date).withPresence(new Presence(Presence.Type.available));
                                                mMultiUserChat.join(builder.build());
                                            }else{
                                                AppLog.Log(TAG, " onError jid is null ");
                                            }
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
                                        } catch (XmppStringprepException e) {
                                            e.printStackTrace();
                                        } catch (NullPointerException e) {
                                            e.printStackTrace();
                                        }
                                        groupModelEntity.setChatMessagesEntity(new ChatMessagesEntity());
                                        emitter.onNext(groupModelEntity);
                                    }
                                });

                            }
                            preferenceHelper.setGroups(groupsSet);
                        });

                        groupModelEntityObservable.subscribe(new Observer<GroupModelEntity>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(GroupModelEntity groupModelEntity) {
                                ((MyApp) getApplication()).getRepository().insertGroupModel(groupModelEntity);
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onComplete() {
                                AppLog.Log("groupModelEntityObservable", " onComplete");

                            }
                        });

                        Utils.hideCustomProgressDialog();
                    } else {
                        Utils.hideCustomProgressDialog();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<GroupUser> call, Throwable t) {
                    swipeRefreshLayout.setRefreshing(false);
                    Utils.hideCustomProgressDialog();
                    Utils.showToast(t.toString());
                }
            });
        }
    }


}
