package com.irmsimapp.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.irmsimapp.ApiClient.ApiHandler;
import com.irmsimapp.BuildConfig;
import com.irmsimapp.Model.ForgotPassword.ForgotPassword;
import com.irmsimapp.R;
import com.irmsimapp.components.CircularImageView;
import com.irmsimapp.utils.Utils;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends BaseActivity implements View.OnClickListener {

    private AppCompatEditText edtForgottenUserName;
    private InputMethodManager inputMethodManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        setUpToolbar();
        setUpViewAndClickAction();
    }

    @Override
    void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbarTitle = toolbar.findViewById(R.id.tvToolbarTitle);
        toolbarTitle.setText(getString(R.string.forgotten_password));
        toolbarTitle.setTextColor(ContextCompat.getColor(this, R.color.colorWhite));
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorYellowBg));
        CircularImageView toolbarIconLeft = toolbar.findViewById(R.id.ivToolbarIconLeft);
        CircularImageView toolbarIconRight = toolbar.findViewById(R.id.ivToolbarIconRight);
        toolbarIconLeft.setVisibility(View.GONE);
        toolbarIconRight.setVisibility(View.GONE);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
    }


    @Override
    void setUpViewAndClickAction() {
        edtForgottenUserName = findViewById(R.id.edtForgottenUserName);
        findViewById(R.id.tvForgottonSubmit).setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tvForgottonSubmit:
                hideKeyboard();
                if (TextUtils.isEmpty(edtForgottenUserName.getText().toString().trim())) {
                    Toast.makeText(ForgotPasswordActivity.this, getString(R.string.please_enter_login_name), Toast.LENGTH_SHORT).show();
                } else {
                    if (Utils.isInternetConnected()) {
                        forgotPassword(edtForgottenUserName.getText().toString().trim());
                    } else {
                        Utils.showToast(getString(R.string.msg_no_internet_connect));
                    }
                }
                break;
        }
    }

    private void hideKeyboard() {
        if (getWindow().getAttributes().softInputMode != WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN) {
            if (getCurrentFocus() != null)
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void forgotPassword(String userName) {
        Utils.showCustomProgressDialog(ForgotPasswordActivity.this, false);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("username", Utils.encrypt(userName));
        Call<ForgotPassword> responseBodyCall;

        if (BuildConfig.COMPANYNAME.equalsIgnoreCase("innoways")) {
            responseBodyCall = ApiHandler.getCommonApiService().getForgotPasswordAspx(params);
        } else {
            responseBodyCall = ApiHandler.getCommonApiService().getForgotPassword(params);
        }

        responseBodyCall.enqueue(new Callback<ForgotPassword>() {
            @Override
            public void onResponse(@NonNull Call<ForgotPassword> call, @NonNull Response<ForgotPassword> response) {
                Utils.hideCustomProgressDialog();
                ForgotPassword forgotPassword = response.body();
                edtForgottenUserName.setText("");
                if (forgotPassword != null && !TextUtils.isEmpty(forgotPassword.getStatus())) {
                    Utils.showToast(forgotPassword.getMsg());
                }
            }

            @Override
            public void onFailure(Call<ForgotPassword> call, Throwable t) {
                Utils.hideCustomProgressDialog();
                edtForgottenUserName.setText("");
                Utils.showToast(t.getMessage());
            }
        });
    }
}