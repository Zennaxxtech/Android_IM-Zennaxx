package com.irmsimapp.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.irmsimapp.Adapter.GroupListAdapter;
import com.irmsimapp.ApiClient.ApiHandler;
import com.irmsimapp.BuildConfig;
import com.irmsimapp.Model.GroupUsers.UserListItem;
import com.irmsimapp.R;
import com.irmsimapp.components.CircularImageView;
import com.irmsimapp.database.entity.GroupModelEntity;
import com.irmsimapp.datamodel.BadKeyWordsModel;
import com.irmsimapp.interfaces.OnGroupClickListeners;
import com.irmsimapp.interfaces.XMPPListener;
import com.irmsimapp.utils.AppLog;
import com.irmsimapp.utils.Const;
import com.irmsimapp.utils.PreferenceHelper;
import com.irmsimapp.utils.Utils;
import com.irmsimapp.viewmodel.GroupListViewModel;
import com.irmsimapp.xmpp.XMPPConfiguration;
import com.irmsimapp.xmpp.XMPPService;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import me.leolin.shortcutbadger.ShortcutBadger;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.irmsimapp.BuildConfig.OPENFIRE_HOST_SERVER_CONFERENCE_SERVICE;
import static com.irmsimapp.BuildConfig.OPENFIRE_HOST_SERVER_KEY;
import static com.irmsimapp.BuildConfig.OPENFIRE_HOST_SERVER_RESOURCE;
import static com.irmsimapp.BuildConfig.OPENFIRE_HOST_SERVER_SERVICE;

public class GroupListActivity extends BaseActivity implements OnGroupClickListeners {
    private ExpandableListView expandableGroupListView;
    private GroupListAdapter groupListAdapter;
    private List<GroupModelEntity> groupModelList;
    private List<GroupModelEntity> entities;
    private SwipeRefreshLayout swGroupList;

    private XMPPListener xmppListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate() method is called");
        setContentView(R.layout.activity_group_list);
        groupModelList = new ArrayList<>();
        entities = new ArrayList<>();
        setUpToolbar();
        if (preferenceHelper.getDeviceToken() != null)
            RegisterForRemoteNotification();
    }

    @Override
    void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbarTitle = toolbar.findViewById(R.id.tvToolbarTitle);
        swGroupList = findViewById(R.id.swGroupList);
        toolbarTitle.setText(R.string.groups);
        CircularImageView toolbarIconLeft = toolbar.findViewById(R.id.ivToolbarIconLeft);
        CircularImageView toolbarIconRight = toolbar.findViewById(R.id.ivToolbarIconRight);
        ImageView ivUserStatus = toolbar.findViewById(R.id.ivUserStatus);
        ivUserStatus.setVisibility(View.VISIBLE);
        toolbarIconRight.setVisibility(View.GONE);
        String photoUrl = preferenceHelper.getProfilePicture();
        if (StringUtils.isNotEmpty(photoUrl)) {
            if (!photoUrl.startsWith("http")) {
                photoUrl = "http://" + photoUrl;
            }
            Picasso.with(toolbarIconLeft.getContext()).load(photoUrl).placeholder(R.drawable.default_user_icon).into(toolbarIconLeft);
        } else {
            Picasso.with(toolbarIconLeft.getContext()).load(R.drawable.default_user_icon).placeholder(R.drawable.default_user_icon).into(toolbarIconLeft);
        }
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbarIconLeft.setOnClickListener(view -> {

            if (Utils.isInternetConnected()) {
                Intent intent = new Intent(GroupListActivity.this, UserProfileActivity.class);
                GroupListActivity.this.startActivity(intent);
            } else {
                Utils.showToast(getString(R.string.msg_no_internet_connect));
            }
        });
        if (XMPPService.XMPPConnection != null) {
            if (XMPPService.XMPPConnection.isConnected()) {
                ivUserStatus.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_round_mic));
                setUpViewAndClickAction();
            } else {
                ivUserStatus.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_round_red));
                connectXmpp();
            }

        } else {
            ivUserStatus.setBackground(AppCompatResources.getDrawable(this, R.drawable.bg_round_red));
            connectXmpp();
        }
    }

    private void connectXmpp() {
        Utils.showCustomProgressDialog(GroupListActivity.this, false);
        xmppListener = new XMPPListener() {
            @Override
            public void authenticatedSuccessfully() {
                Utils.hideCustomProgressDialog();
                setUpViewAndClickAction();
            }

            @Override
            public void authenticateFailed() {
                Utils.hideCustomProgressDialog();
                Utils.showToast("authenticate Failed");
            }

            @Override
            public void authenticationError(String s) {
                Utils.hideCustomProgressDialog();
            }
        };
        GroupListActivity.this.stopService(new Intent(GroupListActivity.this, XMPPService.class));
        GroupListActivity.this.startService(new Intent(GroupListActivity.this, XMPPService.class));
        XMPPConfiguration.getInstance().setXMPPInterface(xmppListener);
        BadKeyWordsModel.getBadWordsFromServer();
        if (XMPPService.XMPPConnection != null) {
            XMPPConfiguration.getInstance().setXmppErrorListener(xmppErrorListener);
        }
    }

    @Override
    void setUpViewAndClickAction() {
        expandableGroupListView = findViewById(R.id.expandableGroupListView);
        EditText edt_search = findViewById(R.id.edt_search);
        edt_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                groupListAdapter.getFilter().filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        groupListAdapter = new GroupListAdapter(dataRepository, groupModelList, this);
        expandableGroupListView.setAdapter(groupListAdapter);
        GroupListViewModel groupListViewModel = ViewModelProviders.of(this).get(GroupListViewModel.class);
        updateGroupListUi(groupListViewModel);

        swGroupList.postDelayed(() -> {
            if (!preferenceHelper.isRememberMe() || groupModelList.size() == 0) {
                swGroupList.setRefreshing(true);
                groupListViewModel.loadGroupList(swGroupList);
            }
        }, 3000);

        swGroupList.setOnRefreshListener(() -> {
            swGroupList.setRefreshing(true);
            groupListViewModel.loadGroupList(swGroupList);
        });
    }

    private void updateGroupListUi(GroupListViewModel groupListViewModel) {

        groupListViewModel.getMessage().observe(this, chatMessagesEntity -> {
            if (chatMessagesEntity != null && groupModelList != null) {
                for (GroupModelEntity groupModelEntity : groupModelList) {
                    if (chatMessagesEntity.getChatType().equalsIgnoreCase("groupchat") && groupModelEntity.getGroupNo().equalsIgnoreCase(chatMessagesEntity.getGroupId())) {
                        groupModelEntity.setChatMessagesEntity(chatMessagesEntity);
                        groupModelEntity.setUnreadCount(groupModelEntity.getUnreadCount() + 1);
                        dataRepository.updateGroupModelEntity(groupModelEntity);
                        break;
                    } else if (chatMessagesEntity.getChatType().equalsIgnoreCase("chat")) {
                        for (UserListItem userListItem : groupModelEntity.getUserList()) {
                            if (userListItem.getLoginName().equalsIgnoreCase(chatMessagesEntity.getMsgFrom())) {
                                userListItem.setChatMessagesEntity(chatMessagesEntity);
                                userListItem.setUnreadCount(userListItem.getUnreadCount() + 1);
                                dataRepository.updateGroupModelEntity(groupModelEntity);
                            }
                        }
                    }
                }
            }
        });


        groupListViewModel.getAllGroupModels().observe(this, groupModelEntities -> {
            if (groupModelEntities != null && groupModelEntities.size() > 0) {
                groupModelList.clear();
                entities.clear();
                groupModelList.addAll(groupModelEntities);
                for (GroupModelEntity entity : groupModelList) {
                    GroupModelEntity groupModelEntity = new GroupModelEntity();
                    groupModelEntity.setChatMessagesEntity(entity.getChatMessagesEntity());
                    groupModelEntity.setGroupMember(entity.isGroupMember());
                    groupModelEntity.setUnreadCount(entity.getUnreadCount());
                    groupModelEntity.setGroupNo(entity.getGroupNo());
                    groupModelEntity.setGroupName(entity.getGroupName());
                    groupModelEntity.setPhotoUrl(entity.getPhotoUrl());
                    groupModelEntity.setContactTitle(entity.getContactTitle());
                    groupModelEntity.setContactName(entity.getContactName());
                    groupModelEntity.setOpen(entity.isOpen());

                    ArrayList<UserListItem> userListItems = new ArrayList<>(entity.getUserList());
                    for (int i = 0; i < entity.getUserList().size(); i++) {
                        if (entity.getUserList().get(i).getLoginName().equalsIgnoreCase(preferenceHelper.getLoginName())) {
                            userListItems.remove(entity.getUserList().get(i));
                            break;
                        }
                    }



                    /*Collections.sort(userListItems, (lhs, rhs) -> {

                        if (lhs.getChatMessagesEntity() == null) {
                            return (rhs.getChatMessagesEntity() == null) ? 0 : -1;
                        }
                        if (rhs.getChatMessagesEntity() == null) {
                            return 1;
                        }
                        return Long.compare(rhs.getChatMessagesEntity().getMsgTime(), lhs.getChatMessagesEntity().getMsgTime());

                    });*/

                    groupModelEntity.setUserList(userListItems);
                    entities.add(groupModelEntity);
                }

                groupListAdapter.setGroupModelList(entities);
            }
        });
    }

    public void JoinGroup(GroupModelEntity groupModelEntity) {
//        /*if (groupModelEntity.isGroupMember()) {
//            goToGroupChatActivity(groupModelEntity, false);
//        } else {
//            checkChatRoom(groupModelEntity);
//        }*/

        goToGroupChatActivity(groupModelEntity,true);
    }

    private void goToGroupChatActivity(GroupModelEntity groupModelEntity, boolean needUpdate) {
        Intent intent = new Intent(GroupListActivity.this, GroupChatActivity.class);
        intent.putExtra(Const.intentKey.GROUP_ID, groupModelEntity.getGroupNo());
        intent.putExtra(Const.intentKey.GROUP_NAME, groupModelEntity.getGroupName());
        intent.putExtra(Const.intentKey.CONTACT_TITLE, groupModelEntity.getContactTitle());
        intent.putExtra(Const.intentKey.CONTACT_NAME, groupModelEntity.getContactName());
        ArrayList<UserListItem> userListItems = new ArrayList<>(groupModelEntity.getUserList());
        //userListItems.addAll(groupModelEntity.getUserList());
        for (UserListItem userListItem : userListItems) {
            userListItem.setChatMessagesEntity(null);
        }
        intent.putExtra(Const.intentKey.GROUP_MEMBERS, userListItems);
        intent.putExtra(Const.intentKey.GROUP_IMAGE_URL, groupModelEntity.getPhotoUrl());
        if (needUpdate) {
            groupModelEntity.setGroupMember(true);
            dataRepository.updateGroupModelEntity(groupModelEntity);
        }
        startActivity(intent);
    }

    public void checkChatRoom(GroupModelEntity groupModelEntity) {
        if (Utils.isInternetConnected()) {
            Utils.showCustomProgressDialog(GroupListActivity.this, false);
            String groupname = BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT + groupModelEntity.getGroupNo().toLowerCase().trim();
            Call<ResponseBody> responseBodyCall = ApiHandler.getOpenfireApiService().checkGroupOnOpenFire(PreferenceHelper.getInstance().getOpenfireHttpSecret(), PreferenceHelper.getInstance().getOpenFireConferenceService(), groupname);
            responseBodyCall.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                    Utils.hideCustomProgressDialog();
                    if (response.code() == 200) {
                        try {
                            String json = response.body().string();
                            JSONObject jsonObject = new JSONObject(json);
                            String username = preferenceHelper.getOpenfireusername() + PreferenceHelper.getInstance().getOpenFireJIDSuffix();
                            if (!jsonObject.isNull("members")) {
                                boolean isMember = false;
                                JSONObject members = jsonObject.getJSONObject("members");
                                Object member = members.get("member");
                                if (member instanceof String) {
                                    String s = (String) member;
                                    if (s.equalsIgnoreCase(username)) {
                                        goToGroupChatActivity(groupModelEntity, true);
                                    }

                                } else if (member instanceof JSONArray) {
                                    JSONArray jsonArray = (JSONArray) member;
                                    if (jsonArray.length() == 0) {
                                        Utils.showToast(getString(R.string.msg_no_member));
                                        return;
                                    }

                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        if (jsonArray.get(i).toString().equalsIgnoreCase(username)) {
                                            goToGroupChatActivity(groupModelEntity, true);
                                            isMember = true;
                                            break;
                                        }
                                        isMember = false;
                                    }
                                    if (!isMember) {
                                        Utils.showToast(getString(R.string.msg_no_member));
                                    }
                                } else {
                                    Utils.showToast(getString(R.string.msg_no_member));
                                }

                            } else {
                                Utils.showToast(getString(R.string.msg_no_member));
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Utils.showToast(getString(R.string.msg_no_member));
                        }

                    } else if (response.code() == 404) {
                        Utils.showToast(getString(R.string.msg_group_not_exist));
                    } else {
                        Utils.showToast(getString(R.string.msg_unable_to_add_user_openfire));
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Utils.hideCustomProgressDialog();
                    Utils.showToast(t.getMessage());
                }
            });

        } else {
            Utils.showToast(getString(R.string.msg_no_internet_connect));
        }
    }


    @Override
    public void closedOnError() {
        super.closedOnError();
        logoutToServer(GroupListActivity.this);
    }

    @Override
    public void closedOnConflict() {
        super.closedOnConflict();
        runOnUiThread(() -> openLogoutConflictDialog(GroupListActivity.this));
    }


    @Override
    protected void onResume() {
        super.onResume();
        dataRepository.getTotalUnreadCounter(new SingleObserver<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(Integer integer) {
                ShortcutBadger.applyCount(GroupListActivity.this, integer);
            }

            @Override
            public void onError(Throwable e) {
                ShortcutBadger.applyCount(GroupListActivity.this, 0);
            }
        });
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

   /* public void RegisterForRemoteNotification() {
        Call<ResponseBody> responseBodyCall = ApiHandler.getOpenfireApiService().callChatLogsApi(Utils.encrypt("2"), Utils.encrypt(preferenceHelper.getLoginName()), Utils.encrypt(preferenceHelper.getOpenfireSenderId() + "@" + OPENFIRE_HOST_SERVER_SERVICE), Utils.encrypt(preferenceHelper.getDeviceToken()), Utils.encrypt(OPENFIRE_HOST_SERVER_RESOURCE), Utils.encrypt("Login"));
        AppLog.Log("url", ApiHandler.getOpenfireApiService().toString());
        responseBodyCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.body() != null) {
                        AppLog.Log("RegisterForPush", Utils.decrypt(response.body().string()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppLog.Log("RegisterForPush", t.getMessage());
            }
        });
    }*/

    public void RegisterForRemoteNotification() {
        Call<ResponseBody> responseBodyCall = ApiHandler.getOpenfireApiService().callChatLogsApi(Utils.encrypt("2"), Utils.encrypt(preferenceHelper.getLoginName()), Utils.encrypt(preferenceHelper.getOpenfireSenderId() + PreferenceHelper.getInstance().getOpenFireJIDSuffix()), Utils.encrypt(preferenceHelper.getDeviceToken()), Utils.encrypt(OPENFIRE_HOST_SERVER_RESOURCE), Utils.encrypt("Login"));
        AppLog.Log("url", ApiHandler.getOpenfireApiService().toString());
        responseBodyCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.body() != null) {
                        AppLog.Log("RegisterForPush", Utils.decrypt(response.body().string()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                AppLog.Log("RegisterForPush", t.getMessage());
            }
        });
    }

    @Override
    public void onGroupParentClick(View view, int groupPosition) {
        switch (view.getId()) {
            case R.id.layout_right:
                if (expandableGroupListView.isGroupExpanded(groupPosition)) {
                    expandableGroupListView.collapseGroup(groupPosition);
                } else {
                    expandableGroupListView.expandGroup(groupPosition);
                }
                break;
            default:
                for (GroupModelEntity dataItem : groupModelList) {
                    if (dataItem.getGroupNo().equalsIgnoreCase(groupListAdapter.getFilteredList().get(groupPosition).getGroupNo())) {
                        JoinGroup(dataItem);
                        break;
                    }
                }
                break;
        }
    }

    @Override
    public void onGroupChildClick(View view, int groupPosition, int childPosition) {
        UserListItem usersList = groupListAdapter.getFilteredList().get(groupPosition).getUserList().get(childPosition);
        UserListItem userListItem = new UserListItem();
        userListItem.setChatMessagesEntity(null);
        userListItem.setUnreadCount(0);
        userListItem.setEmail(usersList.getEmail());
        userListItem.setPhone(usersList.getPhone());
        userListItem.setMobile(usersList.getMobile());
        userListItem.setFullName(usersList.getFullName());
        userListItem.setLoginName(usersList.getLoginName());
        userListItem.setPhotoUrl(usersList.getPhotoUrl());
        userListItem.setUserName(usersList.getUserName());
        userListItem.setUserType(usersList.getUserType());
        Intent intent = new Intent(GroupListActivity.this, IndividualChatActivity.class);
        intent.putExtra(Const.intentKey.USER_DATA, userListItem);
        startActivity(intent);
    }
}
