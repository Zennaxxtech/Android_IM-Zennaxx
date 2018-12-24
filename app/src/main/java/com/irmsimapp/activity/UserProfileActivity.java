package com.irmsimapp.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.irmsimapp.ApiClient.ApiHandler;
import com.irmsimapp.BuildConfig;
import com.irmsimapp.Model.GroupUsers.UserListItem;
import com.irmsimapp.Model.UserProfile.UserProfile;
import com.irmsimapp.R;
import com.irmsimapp.components.CircularImageView;
import com.irmsimapp.utils.Const;
import com.irmsimapp.utils.Utils;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class UserProfileActivity extends BaseActivity implements View.OnClickListener {
    private CircularImageView ivUserImg;
    private TextView tvUserName, tvProfileFullName, tvProfileEmail, tvProfilePhone, tvProfileMobile, tvProfileLogout;
    private ImageView ivCallMobile, ivCallPhone;
    private final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 210;
    private boolean isFromChat = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        Intent intent = getIntent();
        if (intent.hasExtra(Const.intentKey.IS_FROM_CHAT)) {
            isFromChat = intent.getBooleanExtra(Const.intentKey.IS_FROM_CHAT, false);
        }

        setUpToolbar();
        setUpViewAndClickAction();
    }

    @Override
    void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView toolbarTitle = toolbar.findViewById(R.id.tvToolbarTitle);
        toolbarTitle.setText(getString(R.string.profile));
        CircularImageView toolbarIconLeft = toolbar.findViewById(R.id.ivToolbarIconLeft);
        CircularImageView toolbarIconRight = toolbar.findViewById(R.id.ivToolbarIconRight);
        toolbarIconLeft.setVisibility(View.GONE);
        toolbarIconRight.setVisibility(View.GONE);
        toolbar.setNavigationIcon(R.drawable.left_aerrow);
        setSupportActionBar(toolbar);


        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
    }

    @Override
    void setUpViewAndClickAction() {
        ivUserImg = findViewById(R.id.ivUserImg);
        tvUserName = findViewById(R.id.tvUserName);
        tvProfileFullName = findViewById(R.id.tvProfileFullName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);
        tvProfileMobile = findViewById(R.id.tvProfileMobile);
        tvProfileLogout = findViewById(R.id.tvProfileLogout);
        ivCallMobile = findViewById(R.id.ivCallMobile);
        ivCallPhone = findViewById(R.id.ivCallPhone);
        tvProfileLogout.setOnClickListener(this);
        ivCallPhone.setOnClickListener(this);
        ivCallMobile.setOnClickListener(this);
        if (isFromChat) {
            UserListItem userListItem = (UserListItem) getIntent().getSerializableExtra(Const.intentKey.USER_DATA);
            updateUi(true, userListItem.getUserName(), userListItem.getPhotoUrl(), userListItem.getFullName(), userListItem.getEmail(), userListItem.getPhone(), userListItem.getMobile());
        } else {
            if (TextUtils.isEmpty(preferenceHelper.getUserPhone()) && TextUtils.isEmpty(preferenceHelper.getUserMobile()) && TextUtils.isEmpty(preferenceHelper.getUserEmail())) {
                getUserProfileDetail();
            } else {
                updateUi(false, preferenceHelper.getUsername(), preferenceHelper.getProfilePicture(), preferenceHelper.getFullName(), preferenceHelper.getUserEmail(), preferenceHelper.getUserPhone(), preferenceHelper.getUserMobile());
            }
        }

    }

    private void updateUi(boolean isFromChat, String userName, String photoUrl, String fullName, String email, String phone, String mobile) {

        if (isFromChat) {
            tvProfileLogout.setVisibility(View.GONE);
        } else {
            tvProfileLogout.setVisibility(View.VISIBLE);
        }

        String userImageUrl = photoUrl;
        if (StringUtils.isNotEmpty(userImageUrl)) {
            if (!userImageUrl.startsWith("http")) {
                userImageUrl = "http://" + userImageUrl;
            }
            Picasso.with(ivUserImg.getContext()).load(userImageUrl).placeholder(R.drawable.default_user_icon).error(R.drawable.default_user_icon).into(ivUserImg);
        }
        tvProfileFullName.setText(fullName);
        tvProfilePhone.setText(phone);
        tvProfileMobile.setText(mobile);
        tvUserName.setText(userName);
        tvProfileEmail.setText(email);
        if (TextUtils.isEmpty(tvProfilePhone.getText().toString())) {
            ivCallPhone.setVisibility(View.GONE);

        }
        if (TextUtils.isEmpty(tvProfileMobile.getText().toString())) {
            ivCallMobile.setVisibility(View.GONE);
        }
    }


    private void getUserProfileDetail() {
        if (Utils.isInternetConnected()) {
            Utils.showCustomProgressDialog(UserProfileActivity.this, false);

            tvProfileLogout.setVisibility(View.VISIBLE);
            HashMap<String, String> user_Profile_param = new HashMap<>();
            user_Profile_param.put("AppsName", Utils.encrypt(BuildConfig.APPSTYPE));
            user_Profile_param.put("UserName", Utils.encrypt(preferenceHelper.getUsername()));
            user_Profile_param.put("UserType", Utils.encrypt(preferenceHelper.getUserType()));
            Call<UserProfile> userProfileCall;
            if (BuildConfig.COMPANYNAME.equalsIgnoreCase("innoways")) {
                userProfileCall = ApiHandler.getCommonApiService().UserProfileAspx(user_Profile_param);
            } else {
                userProfileCall = ApiHandler.getCommonApiService().UserProfile(user_Profile_param);
            }
            userProfileCall.enqueue(new Callback<UserProfile>() {
                @Override
                public void onResponse(@NonNull Call<UserProfile> call, @NonNull Response<UserProfile> response) {
                    Utils.hideCustomProgressDialog();
                    UserProfile userProfile = response.body();
                    if (userProfile != null && userProfile.getStatus().equalsIgnoreCase("1")) {
                        List<UserProfile.Datum> data = userProfile.getData();
                        UserProfile.Datum datum = data.get(0);
                        preferenceHelper.putUserEmail(datum.getEmail());
                        preferenceHelper.putUserPhone(datum.getPhone());
                        preferenceHelper.putUserMobile(datum.getMobile());
                        updateUi(false, datum.getUserName(), datum.getPhotoUrl(), datum.getFullName(), datum.getEmail(), datum.getPhone(), datum.getMobile());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UserProfile> call, @NonNull Throwable t) {
                    Utils.hideCustomProgressDialog();
                    Utils.showToast(t.toString());
                    finish();
                }
            });

        } else {
            Utils.showToast(getString(R.string.msg_no_internet_connect));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tvProfileLogout:
                logoutToServer(UserProfileActivity.this);
                break;
            case R.id.ivCallPhone:
                if (TextUtils.isEmpty(tvProfilePhone.getText().toString())) {
                    return;
                }
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                } else if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED) {
                    Utils.showToast(getString(R.string.msg_allow_permission_contact));
                } else if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + tvProfilePhone.getText().toString()));
                    startActivity(callIntent);
                }
                break;
            case R.id.ivCallMobile:
                if (TextUtils.isEmpty(tvProfileMobile.getText().toString())) {
                    return;
                }
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                } else if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED) {
                    Utils.showToast(getString(R.string.msg_allow_permission_contact));
                } else if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + tvProfileMobile.getText().toString()));
                    startActivity(callIntent);
                }
                break;
        }
    }

    @Override
    public void closedOnError() {
        super.closedOnError();
        logoutToServer(UserProfileActivity.this);
    }

    @Override
    public void closedOnConflict() {
        super.closedOnConflict();
        runOnUiThread(() -> openLogoutConflictDialog(UserProfileActivity.this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        }
    }


}
