package com.irmsimapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.irmsimapp.Adapter.ChatAdapter;
import com.irmsimapp.Adapter.RecyclerSectionItemDecoration;
import com.irmsimapp.ApiClient.ApiHandler;
import com.irmsimapp.BuildConfig;
import com.irmsimapp.Model.GroupUsers.UserListItem;
import com.irmsimapp.Model.SaveMessage.SaveMessage;
import com.irmsimapp.R;
import com.irmsimapp.components.CircularImageView;
import com.irmsimapp.components.CustomDialogEnable;
import com.irmsimapp.database.entity.ChatMessagesEntity;
import com.irmsimapp.interfaces.OnRecyclerViewClickListener;
import com.irmsimapp.utils.AppLog;
import com.irmsimapp.utils.Const;
import com.irmsimapp.utils.GalleryUtil;
import com.irmsimapp.utils.PreferenceHelper;
import com.irmsimapp.utils.Utils;
import com.irmsimapp.viewmodel.GroupChatViewModel;
import com.irmsimapp.xmpp.XMPPService;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smackx.muc.MucEnterConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatException;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.irmsimapp.BuildConfig.OPENFIRE_HOST_SERVER_CONFERENCE_SERVICE;

public class GroupChatActivity extends BaseActivity implements View.OnClickListener, OnRecyclerViewClickListener, View.OnTouchListener {
    private MultiUserChatManager manager;
    private MultiUserChat mMultiUserChat;
    private String groupId, groupName, contactName, contactTitle, groupImageUrl;
    private ArrayList<UserListItem> groupmembers;
    private ChatAdapter chatAdapter;
    private RecyclerView rvGroupChat;
    private EditText etEnterMsg;
    private EntityBareJid jid;

    private FrameLayout flGroupAttachment;
    private final int PERMISSION_FOR_IMAGE = 456, PERMISSION_RECORD_AUDIO = 678, PERMISSION_FOR_SETTING = 567, PERMISSION_CAMERA_CAPTURE = 345;
    private CustomDialogEnable customDialogEnable;
    private static final int GALLERY_PICK_IMAGE = 200, CAMERA_CAPTURE = 500;
    private Uri mImageCaptureUri;
    private MediaRecorder mediaRecorder;
    private File voiceFile = null;
    private InputMethodManager inputMethodManager;
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private SensorEventListener proximitySensorListener;
    private RecyclerSectionItemDecoration sectionItemDecoration;
    private TextView tvGroupChatSendButton;
    private static boolean isInitFresco;
    private long timeWhenStopped;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        /*Fresco.initialize(this);*/

        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        groupId = getIntent().getStringExtra(Const.intentKey.GROUP_ID);
        groupName = getIntent().getStringExtra(Const.intentKey.GROUP_NAME);
        contactName = getIntent().getStringExtra(Const.intentKey.CONTACT_NAME);
        contactTitle = getIntent().getStringExtra(Const.intentKey.CONTACT_TITLE);
        groupImageUrl = getIntent().getStringExtra(Const.intentKey.GROUP_IMAGE_URL);
        groupmembers = (ArrayList<UserListItem>) getIntent().getSerializableExtra(Const.intentKey.GROUP_MEMBERS);
        setUpToolbar();
        setUpViewAndClickAction();
        setupChat();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        proximitySensor = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) : null;
        if (proximitySensor == null) {
            Log.e(TAG, "Proximity sensor not available.");
        } else {
            proximitySensorListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    if (sensorEvent.values[0] < proximitySensor.getMaximumRange()) {
                        // its near to ear
                        if (chatAdapter != null)
                            chatAdapter.getAudioChangeListener().isPlayingSpeakerMode(false);
                    } else {
                        //its far to ear..
                        if (chatAdapter != null)
                            chatAdapter.getAudioChangeListener().isPlayingSpeakerMode(true);
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {
                }
            };
        }
        dataRepository.setGroupMessageRead(groupId.toLowerCase());
    }


    @Override
    void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbarTitle = toolbar.findViewById(R.id.tvToolbarTitle);
        toolbarTitle.setText(groupName);
        CircularImageView toolbarIconLeft = toolbar.findViewById(R.id.ivToolbarIconLeft);
        CircularImageView toolbarIconRight = toolbar.findViewById(R.id.ivToolbarIconRight);
        toolbarIconLeft.setVisibility(View.GONE);
        toolbarIconRight.setVisibility(View.VISIBLE);
        if (StringUtils.isNotEmpty(groupImageUrl)) {
            if (!groupImageUrl.startsWith("http")) {
                groupImageUrl = "http://" + groupImageUrl;
            }
            Picasso.with(toolbarIconRight.getContext()).load(groupImageUrl).placeholder(R.drawable.group_icon_round).resize(1200, 800).onlyScaleDown().into(toolbarIconRight);
        } else {
            Picasso.with(toolbarIconRight.getContext()).load(R.drawable.group_icon_round).placeholder(R.drawable.group_icon_round).into(toolbarIconRight);
        }
        toolbarIconRight.setOnClickListener(this);
        toolbar.setNavigationIcon(R.drawable.left_aerrow);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());

    }

    @Override
    protected void onResume() {
        super.onResume();

        hideKeyboard();
        if (proximitySensor != null) {
            sensorManager.registerListener(proximitySensorListener, proximitySensor, 2 * 1000 * 1000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (proximitySensor != null) {
            sensorManager.unregisterListener(proximitySensorListener);
        }

    }

    @Override
    void setUpViewAndClickAction() {
        findViewById(R.id.ivAttachment).setOnClickListener(this);
        findViewById(R.id.ivGroupSendVoice).setOnClickListener(this);
        findViewById(R.id.ivGroupSendImage).setOnClickListener(this);
        findViewById(R.id.ivGroupSendCamera).setOnClickListener(this);
        findViewById(R.id.tvGroupChatSendButton).setOnClickListener(this);
        tvGroupChatSendButton = findViewById(R.id.tvGroupChatSendButton);
        rvGroupChat = findViewById(R.id.rvGroupChat);
        etEnterMsg = findViewById(R.id.etEnterMsg);
        flGroupAttachment = findViewById(R.id.flGroupAttachment);
        flGroupAttachment.setVisibility(View.GONE);
        chatAdapter = new ChatAdapter(true, diffCallback, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvGroupChat.setLayoutManager(layoutManager);
        rvGroupChat.setAdapter(chatAdapter);
        rvGroupChat.setOnTouchListener(this);
        rvGroupChat.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> rvGroupChat.scrollToPosition(chatAdapter.getItemCount() - 1));
        // chatAdapter.setRecyclerViewClickListener(this);

        GroupChatViewModel groupChatViewModel = ViewModelProviders.of(this).get(GroupChatViewModel.class);
        groupChatViewModel.setGroupName(groupName);
//        updateChatUi(groupChatViewModel);
        getGroupChatMessage(groupChatViewModel);

    }

    private void getGroupChatMessage(GroupChatViewModel groupChatViewModel) {
        new Handler().postDelayed(() -> runOnUiThread(() -> {
            updateChatUi(groupChatViewModel);
        }), 2000);
    }

    private void updateChatUi(GroupChatViewModel viewModel) {
        viewModel
                .getMessageList()
                .observe(this, chatMessagesEntityPagedList -> {

                    if (chatMessagesEntityPagedList != null && chatMessagesEntityPagedList.size() > 0) {
                        chatAdapter.submitList(chatMessagesEntityPagedList);
                        if (sectionItemDecoration != null)
                            rvGroupChat.removeItemDecoration(sectionItemDecoration);
                        sectionItemDecoration = new RecyclerSectionItemDecoration(getResources().getDimensionPixelSize(R.dimen.recycler_section_header_height), true, getSectionCallback(chatMessagesEntityPagedList));
                        rvGroupChat.addItemDecoration(sectionItemDecoration);
                        new Handler().postDelayed(() -> rvGroupChat.scrollToPosition(chatMessagesEntityPagedList.size() - 1), 700);
                    } else {
                        if (chatAdapter.getCurrentList() != null && chatAdapter.getCurrentList().size() > 0) {
                            reJoinGroupToGetHistory(chatAdapter.getCurrentList().get(0).getMsgTime() - 1000 * 60 * 60 * 24 * 10);
                        } else {
                            reJoinGroupToGetHistory(0);
                        }
                    }

                });
    }

    private void reJoinGroupToGetHistory(long MsgTime) {

        try {
            jid = JidCreate.entityBareFrom(BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT + groupId + "@" + PreferenceHelper.getInstance().getOpenFireConferenceService());
            MultiUserChat mMultiUserChat = manager.getMultiUserChat(jid);
            Date date = new Date();

            final MucEnterConfiguration.Builder builder = mMultiUserChat.getEnterConfigurationBuilder(Resourcepart.from(PreferenceHelper.getInstance().getOpenfireusername().replace("@", "#"))).withPresence(new Presence(Presence.Type.available));
            if (MsgTime == 0) {
                builder.requestMaxStanzasHistory(5);
                AppLog.Log(TAG, "getting 50 messages");
            } else {
                date.setTime(MsgTime - 1000 * 60 * 60 * 24 * 10);
                builder.requestHistorySince(date);
            }
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
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }


    private RecyclerSectionItemDecoration.SectionCallback getSectionCallback(List<ChatMessagesEntity> chatMessages) {
        return new RecyclerSectionItemDecoration.SectionCallback() {
            @Override
            public boolean isSection(int position) {
                if (position >= chatMessages.size()) {
                    position = position - 1;
                }
                Date date1 = new Date();
                Date date2 = new Date();

                try {
                    date1.setTime(chatMessages.get(position).getMsgTime());
                    date2.setTime(chatMessages.get(position > 0 ? position - 1 : position).getMsgTime());
                } catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return position == 0 || !DateUtils.isSameDay(date1, date2);
            }

            @Override
            public CharSequence getSectionHeader(int position) {

                if (position >= chatMessages.size()) {
                    position = position - 1;
                }
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
                Date date = new Date();
                try {
                    date.setTime(chatMessages.get(position).getMsgTime());
                } catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (DateUtils.isSameDay(date, new Date())) {
                    return getString(R.string.txt_today);
                } else if (DateUtils.isSameDay(date, new Date(System.currentTimeMillis() - DateUtils.MILLIS_PER_DAY))) {
                    return getString(R.string.txt_yesterday);
                } else {
                    return simpleDateFormat.format(date).trim();
                }
            }
        };
    }

    public void setupChat() {
        try {
//            jid = JidCreate.entityBareFrom(BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT + groupId + "@" + OPENFIRE_HOST_SERVER_CONFERENCE_SERVICE);
            jid = JidCreate.entityBareFrom(BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT + groupId + "@" + PreferenceHelper.getInstance().getOpenFireConferenceService());
            manager = MultiUserChatManager.getInstanceFor(XMPPService.XMPPConnection);
            mMultiUserChat = manager.getMultiUserChat(jid);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tvGroupChatSendButton:
                if (etEnterMsg.getText().toString().trim().isEmpty()) {
                    Utils.showToast(getString(R.string.please_enter_message));
                } else if (!Utils.isInternetConnected()) {
                    Utils.showToast(getString(R.string.msg_no_internet_connect));
                } else {
                    sendText();
                }
                break;
            case R.id.ivAttachment:
                flGroupAttachment.setVisibility(View.VISIBLE);
                break;
            case R.id.ivGroupSendVoice:
                if (Utils.isInternetConnected()) {
                    if (Utils.isNeedPermission()) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            createVoiceFile();
                        } else {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, PERMISSION_RECORD_AUDIO);
                            return;
                        }
                    } else {
                        createVoiceFile();
                    }
                } else {
                    Utils.showToast(getString(R.string.msg_no_internet_connect));
                }

                break;
            case R.id.ivGroupSendImage:
                if (Utils.isInternetConnected()) {
                    if (Utils.isNeedPermission()) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            pickImageFromGallery();
                        } else {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_FOR_IMAGE);
                        }
                    } else {
                        pickImageFromGallery();
                    }
                } else {
                    Utils.showToast(getString(R.string.msg_no_internet_connect));
                }
                break;
            case R.id.ivGroupSendCamera:
                if (Utils.isInternetConnected()) {
                    if (Utils.isNeedPermission()) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            captureImage();
                        } else {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, PERMISSION_CAMERA_CAPTURE);
                        }
                    } else {
                        captureImage();
                    }
                } else {
                    Utils.showToast(getString(R.string.msg_no_internet_connect));
                }
                break;
            case R.id.ivToolbarIconRight:
                Intent intent = new Intent(GroupChatActivity.this, GroupProfileActivity.class);
                intent.putExtra(Const.intentKey.GROUP_ID, groupId);
                intent.putExtra(Const.intentKey.GROUP_NAME, groupName);
                intent.putExtra(Const.intentKey.CONTACT_NAME, contactName);
                intent.putExtra(Const.intentKey.CONTACT_TITLE, contactTitle);
                intent.putExtra(Const.intentKey.GROUP_MEMBERS, groupmembers);
                intent.putExtra(Const.intentKey.GROUP_IMAGE_URL, groupImageUrl);
                GroupChatActivity.this.startActivity(intent);
                break;
            default:
                break;
        }

    }

    @Override
    public void onRecyclerViewClick(View view, int position) {
        switch (view.getId()) {
            case R.id.ivChatUser:
                String senderName = chatAdapter.getCurrentList().get(position).getMsgFromGroupMember();
                if (senderName == null) {
                    break;
                } else {
                    if (senderName.equalsIgnoreCase(preferenceHelper.getLoginName())) {
                        break;
                    }
                }
                for (UserListItem usersList : groupmembers) {
                    if (usersList.getLoginName().equalsIgnoreCase(senderName)) {
                        Intent intent = new Intent(GroupChatActivity.this, IndividualChatActivity.class);
                        intent.putExtra(Const.intentKey.USER_DATA, usersList);
                        startActivity(intent);
                        break;
                    }
                }
                break;
            case R.id.flChatContentImg:

                openChatImageView(GroupChatActivity.this, chatAdapter.getCurrentList().get(position));

                break;
            case R.id.flChatContentVideo:

                openChatVideoView(GroupChatActivity.this, chatAdapter.getCurrentList().get(position));

                break;

        }
    }

    private void pickImageFromGallery() {
        Intent gallery_Intent = new Intent(getApplicationContext(), GalleryUtil.class);
        startActivityForResult(gallery_Intent, GALLERY_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case CAMERA_CAPTURE:
                if (resultCode == Activity.RESULT_OK) {
                    sendFile("image", mImageCaptureUri);
                }
                break;
            case GALLERY_PICK_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri picUri = Uri.fromFile(new File(data.getStringExtra("picturePath")));
                    if (picUri.toString().toLowerCase().contains("mp4")) {
                        sendFile("video", picUri);
                    } else {
                        sendFile("image", picUri);
                    }
                }
                break;
            case PERMISSION_FOR_SETTING:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
                    Utils.showToast(getString(R.string.please_provide_permission_to_app_from_setting));
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            switch (requestCode) {
                case PERMISSION_FOR_IMAGE:
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        pickImageFromGallery();
                    } else {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_FOR_IMAGE);
                        } else {
                            openPermissionNotifyDialog(getResources().getString(R.string.msg_permission_write_storage_notification), getResources().getString(R.string.permission_external_storage_deny_message));
                        }
                    }
                    break;
                case PERMISSION_CAMERA_CAPTURE:
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        captureImage();
                    } else {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, PERMISSION_CAMERA_CAPTURE);
                        } else {
                            openPermissionNotifyDialog(getResources().getString(R.string.msg_permission_image_notification), getResources().getString(R.string.permission_image_deny_message));
                        }
                    }
                    break;
                case PERMISSION_RECORD_AUDIO:
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        createVoiceFile();
                    } else {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, PERMISSION_RECORD_AUDIO);
                        } else {
                            openPermissionNotifyDialog(getResources().getString(R.string.msg_permission_audio_notification), getResources().getString(R.string.permission_audio_deny_message));
                        }
                    }
                    break;
            }
        }
    }

    private void captureImage() {
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mImageCaptureUri = FileProvider.getUriForFile(GroupChatActivity.this, GroupChatActivity.this.getPackageName(), Utils.createImageFile());
            captureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            mImageCaptureUri = Uri.fromFile(Utils.createImageFile());
        }
        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
        startActivityForResult(captureIntent, CAMERA_CAPTURE);
    }

    private void sendText() {
        flGroupAttachment.setVisibility(View.GONE);
        String message = filterBadWords(etEnterMsg.getText().toString().trim());
        if (TextUtils.isEmpty(message)) {
            Utils.showToast(getString(R.string.please_enter_message));
            return;
        }
        if (XMPPService.XMPPConnection.isConnected()) {
            if (XMPPService.XMPPConnection.isAuthenticated()) {
                if (Utils.isInternetConnected() && message.length() > 0) {
                    try {
                        Message msg = new Message();
                        msg.setType(Message.Type.groupchat);
                        msg.setBody(message);
                        msg.setFrom(XMPPService.XMPPConnection.getUser());
                        Map<String, String> msgMap = new HashMap<>();
                        msgMap.put("sender_image_url", preferenceHelper.getProfilePicture());
                        msgMap.put("content_url", "");
                        msgMap.put("sender_fullname", preferenceHelper.getFullName());
                        msgMap.put("type_of_chat", "chat");
                        Map<String, String> senderMap = new HashMap<>();
                        senderMap.put("sender_id", preferenceHelper.getOpenfireSenderId());
                        StandardExtensionElement sender_jid = StandardExtensionElement.builder("sender_jid", "urn:xmpp:sender_jid").addAttributes(senderMap).build();
                        msg.addExtension(sender_jid);

                        ChatMessagesEntity entity = new ChatMessagesEntity();
                        entity.setMessage(message);
                        entity.setMsgId(msg.getStanzaId());

                        entity.setSenderProfileUrl(preferenceHelper.getProfilePicture());
                        entity.setFullName(preferenceHelper.getFullName());
                        entity.setMsgFrom(preferenceHelper.getLoginName());
                        entity.setMsgFromGroupMember(preferenceHelper.getLoginName());
                        entity.setMsgTo(jid.getLocalpart().toString().replace("#", "@").replace(BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT, ""));
                        entity.setGroupId(jid.getLocalpart().toString().replace("#", "@").replace(BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT, ""));
                        entity.setChatType("groupchat");
                        entity.setMediaPath("");
                        entity.setMsgType("chat");
                        entity.setRead(true);

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("GroupFg", "True");
                        jsonObject.put("GroupNo", groupId);
                        jsonObject.put("SendFrom", preferenceHelper.getLoginName());
                        jsonObject.put("SendTo", groupId);
                        jsonObject.put("SendDate", sdfSendServer.format(new Date()));
                        jsonObject.put("FileSize", "");
                        jsonObject.put("FileType", "");
                        jsonObject.put("ItemNo", UUID.randomUUID().toString());
                        jsonObject.put("FileName", "");
                        jsonObject.put("MsgType", "chat");
                        AppLog.Log(TAG + " message unescapeJava", StringEscapeUtils.unescapeJava(message) + " ");
                        AppLog.Log(TAG + " message escapeJava", StringEscapeUtils.escapeJava(message) + " ");
                        jsonObject.put("Content", StringEscapeUtils.unescapeJava(message));


                        Map<String, String> group_save_message_param = new HashMap<>();
                        group_save_message_param.put("UserType", Utils.encrypt(preferenceHelper.getUserType()));
                        group_save_message_param.put("AppsType", Utils.encrypt(BuildConfig.APPSTYPE));
                        group_save_message_param.put("JsonObj", Utils.encrypt(jsonObject.toString()));
                        group_save_message_param.put("UserName", Utils.encrypt(preferenceHelper.getUsername()));
                        tvGroupChatSendButton.setEnabled(false);

                        Date date = new Date();
                        msgMap.put("msg_time", String.valueOf(date != null ? date.getTime() : new Date().getTime()));
                        entity.setMsgTime(date != null ? date.getTime() : new Date().getTime());
                        StandardExtensionElement message_data = StandardExtensionElement.builder("message_data", "urn:xmpp:message_data").addAttributes(msgMap).build();
                        msg.addExtension(message_data);
                        StandardExtensionElement xmppjid = StandardExtensionElement.builder("xmppjid", "urn:xmpp:xmppjid").setText(XMPPService.XMPPConnection.getUser().asBareJid().toString()).build();
                        msg.addExtension(xmppjid);
                        StandardExtensionElement UserName = StandardExtensionElement.builder("UserName", "urn:xmpp:UserName").setText(preferenceHelper.getFullName()).build();
                        msg.addExtension(UserName);
                        StandardExtensionElement typeofchat = StandardExtensionElement.builder("typeofchat", "urn:xmpp:typeofchat").setText("text").build();
                        msg.addExtension(typeofchat);
                        if (date == null) {
                            date = new Date();
                        }
                        SimpleDateFormat dateFormat = new SimpleDateFormat(Const.DateFormatter.CHAT_TIME);
                        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                        StandardExtensionElement chatTime = StandardExtensionElement.builder("chatTime", "urn:xmpp:chatTime").setText(dateFormat.format(date)).build();
                        msg.addExtension(chatTime);
                        dataRepository.insertChatMessage(entity);
                        rvGroupChat.scrollToPosition(rvGroupChat.getAdapter().getItemCount() == 0 ? 0 : rvGroupChat.getAdapter().getItemCount() - 1);
                        etEnterMsg.setText("");

                        try {
                            mMultiUserChat.sendMessage(msg);
                        } catch (SmackException.NotConnectedException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Utils.showToast(getString(R.string.msg_no_internet_connect));
                }
            } else {
                try {
                    XMPPService.XMPPConnection.login();
                    if (XMPPService.XMPPConnection.isAuthenticated()) {
                        sendText();
                    }
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
        }
    }

    private void sendFile(final String fileType, Uri filePath) {
        flGroupAttachment.setVisibility(View.GONE);
        if (XMPPService.XMPPConnection.isConnected()) {
            if (XMPPService.XMPPConnection.isAuthenticated()) {

                File file = new File(filePath.getPath());
                final Message msg = new Message();
                msg.setType(Message.Type.groupchat);
                msg.setFrom(XMPPService.XMPPConnection.getUser());
                final ChatMessagesEntity entity = new ChatMessagesEntity();

                try {
                    Map<String, String> group_save_message_param = new HashMap<>();
                    group_save_message_param.put("UserType", Utils.encrypt(preferenceHelper.getUserType()));
                    group_save_message_param.put("SendFrom", Utils.encrypt(preferenceHelper.getLoginName()));
                    group_save_message_param.put("FileName", Utils.encrypt(file.getName()));
                    group_save_message_param.put("FileType", Utils.encrypt(fileType));
                    group_save_message_param.put("ItemNo", Utils.encrypt(UUID.randomUUID().toString()));


                    msg.setBody("[" + fileType + "]");
                    entity.setMessage("[" + fileType + "]");
                    entity.setMsgId(msg.getStanzaId());
                    entity.setSenderProfileUrl(preferenceHelper.getProfilePicture());
                    entity.setFullName(preferenceHelper.getFullName());
                    entity.setMsgFrom(preferenceHelper.getLoginName());
                    entity.setChatType("groupchat");
                    entity.setMsgFromGroupMember(preferenceHelper.getLoginName());
                    entity.setMsgTo(jid.getLocalpart().toString().replace("#", "@").replace(BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT, ""));
                    entity.setGroupId(jid.getLocalpart().toString().replace("#", "@").replace(BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT, ""));
                    entity.setMediaPath(filePath.getPath());
                    entity.setMsgType(fileType);
                    entity.setRead(true);
                    entity.setMsgTime(new Date().getTime());
                    entity.setUploadingFile(true);
                    dataRepository.insertChatMessage(entity);
                    Call<SaveMessage> saveMessageCall;
                    if (BuildConfig.COMPANYNAME.equalsIgnoreCase("innoways")) {
                        saveMessageCall = ApiHandler.getUploadFileApiService().saveFileMessageOnServerAspx(group_save_message_param, ApiHandler.makeMultipartRequestBody(filePath, fileType));
                    } else {
                        saveMessageCall = ApiHandler.getUploadFileApiService().saveFileMessageOnServer(group_save_message_param, ApiHandler.makeMultipartRequestBody(filePath, fileType));
                    }
                    saveMessageCall.enqueue(new Callback<SaveMessage>() {
                        @Override
                        public void onResponse(Call<SaveMessage> call, Response<SaveMessage> response) {
                            voiceFile = null;
                            SaveMessage saveMessage = response.body();
                            if (saveMessage != null && saveMessage.getStatus().equalsIgnoreCase("1")) {
                                Date date = response.headers().getDate("Date");
                                Map<String, String> stringMap = new HashMap<>();
                                stringMap.put("msg_time", String.valueOf(date != null ? date.getTime() : new Date().getTime()));
                                stringMap.put("sender_image_url", preferenceHelper.getProfilePicture());
                                stringMap.put("sender_fullname", preferenceHelper.getFullName());
                                stringMap.put("content_url", saveMessage.getData().get(0).getFileUrl());
                                stringMap.put("type_of_chat", fileType);
                                StandardExtensionElement message_data = StandardExtensionElement.builder("message_data", "urn:xmpp:message_data").addAttributes(stringMap).build();
                                msg.addExtension(message_data);
                                StandardExtensionElement xmppjid = StandardExtensionElement.builder("xmppjid", "urn:xmpp:xmppjid").setText(XMPPService.XMPPConnection.getUser().asBareJid().toString()).build();
                                msg.addExtension(xmppjid);
                                StandardExtensionElement UserName = StandardExtensionElement.builder("UserName", "urn:xmpp:UserName").setText(preferenceHelper.getFullName()).build();
                                msg.addExtension(UserName);
                                StandardExtensionElement typeofchat = StandardExtensionElement.builder("typeofchat", "urn:xmpp:typeofchat").setText(fileType).build();
                                msg.addExtension(typeofchat);

                                if (fileType.equalsIgnoreCase("audio")) {
                                    StandardExtensionElement audio = StandardExtensionElement.builder("audioLengh", "urn:xmpp:audioLengh").setText(getDuration(file)).build();
                                    msg.addExtension(audio);
                                }
                                if (date == null) {
                                    date = new Date();
                                }
                                SimpleDateFormat dateFormat = new SimpleDateFormat(Const.DateFormatter.CHAT_TIME);
                                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                                StandardExtensionElement chatTime = StandardExtensionElement.builder("chatTime", "urn:xmpp:chatTime").setText(dateFormat.format(date)).build();
                                msg.addExtension(chatTime);
                                entity.setMsgTime(date != null ? date.getTime() : new Date().getTime());
                                Map<String, String> senderMap = new HashMap<>();
                                senderMap.put("sender_id", preferenceHelper.getOpenfireSenderId());
                                StandardExtensionElement sender_jid = StandardExtensionElement.builder("sender_jid", "urn:xmpp:sender_jid").addAttributes(senderMap).build();
                                msg.addExtension(sender_jid);
                            } else {
                                Utils.showToast(getString(R.string.msg_message_not_sent));
                                dataRepository.setUploadFileMessage(entity.getMsgId(), entity.getMsgTime());
                                return;
                            }

                            try {
                                mMultiUserChat.sendMessage(msg);
                            } catch (SmackException.NotConnectedException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            dataRepository.setUploadFileMessage(entity.getMsgId(), entity.getMsgTime());
                            rvGroupChat.scrollToPosition(rvGroupChat.getAdapter().getItemCount() == 0 ? 0 : rvGroupChat.getAdapter().getItemCount() - 1);

                            etEnterMsg.setText("");
                        }

                        @Override
                        public void onFailure(Call<SaveMessage> call, Throwable t) {
                            dataRepository.setUploadFileMessage(entity.getMsgId(), entity.getMsgTime());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                try {
                    XMPPService.XMPPConnection.login();
                    if (XMPPService.XMPPConnection.isAuthenticated()) {
                        sendFile(fileType, filePath);
                    }
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
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        chatAdapter.notifyDataSetChanged();
        rvGroupChat.scrollToPosition(rvGroupChat.getAdapter().getItemCount() == 0 ? 0 : rvGroupChat.getAdapter().getItemCount() - 1);
        hideKeyboard();
    }

    private void openPermissionNotifyDialog(String titleMessage, String disableMessage) {
        if (customDialogEnable != null && customDialogEnable.isShowing()) {
            return;
        }
        customDialogEnable = new CustomDialogEnable(this, titleMessage, getResources().getString(R.string.no), getResources().getString(R.string.yes)) {
            @Override
            public void doWithEnable() {
                closedPermissionDialog();
                startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), PERMISSION_FOR_SETTING);
            }

            @Override
            public void doWithDisable() {
                closedPermissionDialog();
                Utils.showToast(disableMessage);
            }
        };
        customDialogEnable.show();
    }

    private void closedPermissionDialog() {
        if (customDialogEnable != null && customDialogEnable.isShowing()) {
            customDialogEnable.dismiss();
            customDialogEnable = null;
        }
    }


    private void createVoiceFile() {
        final Dialog dialog = new Dialog(GroupChatActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_recording);
        dialog.setCancelable(true);

        final Chronometer tvDialogTimer = dialog.findViewById(R.id.tvDialogTimer);
        final ImageView ivDialogStartAudio = dialog.findViewById(R.id.ivDialogStartAudio);
        final ImageView ivDialogStopAudio = dialog.findViewById(R.id.ivDialogStopAudio);
        TextView tvDialogSendAudio = dialog.findViewById(R.id.tvDialogSendAudio);
        ivDialogStartAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initializeMediaRecord();
                if (mediaRecorder == null) {
                    initializeMediaRecord();
                }
                ivDialogStartAudio.setVisibility(View.GONE);
                ivDialogStopAudio.setVisibility(View.VISIBLE);
                startAudioRecording();
                tvDialogTimer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
                tvDialogTimer.start();
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                stopAudioRecording();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                stopAudioRecording();
            }
        });

        ivDialogStopAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ivDialogStartAudio.setVisibility(View.VISIBLE);
                ivDialogStopAudio.setVisibility(View.GONE);
                stopAudioRecording();
                tvDialogTimer.stop(); // start a chronometer
                timeWhenStopped = tvDialogTimer.getBase() - SystemClock.elapsedRealtime();
            }
        });
        tvDialogSendAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (voiceFile != null) {
                    dialog.dismiss();
                    stopAudioRecording();
                    sendFile("audio", Uri.fromFile(voiceFile));
                } else {
                    stopAudioRecording();
                    Utils.showToast("please click green button to record Audio.");
                }
            }
        });

        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

    private void initializeMediaRecord() {
        voiceFile = Utils.createAudioFile();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(voiceFile.getAbsolutePath());

    }

    private void startAudioRecording() {
        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopAudioRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chatAdapter.stopPlaying();
        dataRepository.setGroupMessageRead(groupId.toLowerCase());
    }

    private void hideKeyboard() {
        if (getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            if (getCurrentFocus() != null)
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        hideKeyboard();
        flGroupAttachment.setVisibility(View.GONE);
        return false;
    }

    @Override
    public void closedOnError() {
        super.closedOnError();
        logoutToServer(GroupChatActivity.this);
    }

    @Override
    public void closedOnConflict() {
        super.closedOnConflict();
        runOnUiThread(() -> openLogoutConflictDialog(GroupChatActivity.this));
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    public static final DiffUtil.ItemCallback<ChatMessagesEntity> diffCallback = new DiffUtil.ItemCallback<ChatMessagesEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessagesEntity oldUser, @NonNull ChatMessagesEntity newUser) {
            // User properties may have changed if reloaded from the DB, but ID is fixed
            return oldUser.getId() == newUser.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessagesEntity oldUser, @NonNull ChatMessagesEntity newUser) {
            // NOTE: if you use equals, your object must properly override Object#equals()
            // Incorrectly returning false here will result in too many animations.
            return oldUser.equals(newUser);
        }
    };


}
