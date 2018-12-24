package com.irmsimapp.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.irmsimapp.ApiClient.ApiHandler;
import com.irmsimapp.BuildConfig;
import com.irmsimapp.Fingerprint.FingerprintAuthenticationDialogFragment;
import com.irmsimapp.Model.AppVersion.AppVersion;
import com.irmsimapp.Model.CheckUserOnOpenFire;
import com.irmsimapp.Model.Login.LoginAPI;
import com.irmsimapp.R;
import com.irmsimapp.components.CustomDialogEnable;
import com.irmsimapp.datamodel.BadKeyWordsModel;
import com.irmsimapp.interfaces.XMPPListener;
import com.irmsimapp.pushy.RegisterForPushNotificationsAsync;
import com.irmsimapp.utils.AppLog;
import com.irmsimapp.utils.BatteryOptimizationUtil;
import com.irmsimapp.utils.EncryptionMethods;
import com.irmsimapp.utils.EncryptionMethodsForOcean;
import com.irmsimapp.utils.PreferenceHelper;
import com.irmsimapp.utils.Utils;
import com.irmsimapp.xmpp.XMPPConfiguration;
import com.irmsimapp.xmpp.XMPPService;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import me.pushy.sdk.Pushy;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.irmsimapp.BuildConfig.OPENFIRE_HOST_SERVER_KEY;

public class LoginActivity extends BaseActivity implements View.OnClickListener, BatteryOptimizationUtil.OnBatteryOptimizationAccepted, BatteryOptimizationUtil.OnBatteryOptimizationCanceled {
    public static final String TAG = "[LoginACtivity]";

    private AppCompatEditText edt_loginname, edt_password;
    private AppCompatCheckBox cbLoginRememberMe;
    private XMPPListener xmppListener;
    private CustomDialogEnable customDialogEnable;
    private static final int PERMISSION_FOR_SETTING = 567, PERMISSION_STORAGE = 345, PERMISSION_FINGER_PRINT_SETTING = 789;
    private static final String DIALOG_FRAGMENT_TAG = "myFragment";
    private static final String SECRET_MESSAGE = "Very secret message";
    public static final String DEFAULT_KEY_NAME = "default_key";
    private SharedPreferences mSharedPreferences;
    private boolean isFirstTimeupdatePaaword = true;
    private LinearLayout flRememberInfo, llSavedDetail;
    private ImageView ivUserImage;
    private TextView tvUserName;
    private String loginNameUser = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Pushy.listen(this);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Request both READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE so that the
            // Pushy SDK will be able to persist the device token in the external storage
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        if (!PreferenceHelper.getInstance().isDone()) {
            new RegisterForPushNotificationsAsync().execute(this);
        } else {
            AppLog.Log("MyApp :", PreferenceHelper.getInstance().getDeviceToken());
        }


        AppLog.Log(TAG, "isLogin : " + preferenceHelper.isLogin());
        if (preferenceHelper.isLogin()) {
            if (isMyServiceRunning(XMPPService.class) && XMPPService.XMPPConnection != null && XMPPService.XMPPConnection.isAuthenticated()) {
                Utils.hideCustomProgressDialog();
                BadKeyWordsModel.getBadWordsFromServer();
                Intent intent = new Intent(LoginActivity.this, GroupListActivity.class);
                LoginActivity.this.startActivity(intent);
                finishAffinity();
            } else {
                Utils.showCustomProgressDialog(LoginActivity.this, false);
                LoginActivity.this.stopService(new Intent(LoginActivity.this, XMPPService.class));
                LoginActivity.this.startService(new Intent(LoginActivity.this, XMPPService.class));
                XMPPConfiguration.getInstance().setXMPPInterface(xmppListener);
                BadKeyWordsModel.getBadWordsFromServer();
                if (XMPPService.XMPPConnection != null) {
                    XMPPConfiguration.getInstance().setXmppErrorListener(xmppErrorListener);
                }
            }
        } else {
            if (XMPPService.XMPPConnection != null) {
                AppLog.Log(TAG, "XMPPService.XMPPConnection is not null");
                if (XMPPService.XMPPConnection.isConnected()) {
                    XMPPService.XMPPConnection.disconnect();
                    AppLog.Log(TAG, "XMPPService.XMPPConnection is disconnected");
                }

                XMPPService.XMPPConnection = null;
                AppLog.Log(TAG, "XMPPService.XMPPConnection is null");
            }
        }
        LoginActivity.this.stopService(new Intent(LoginActivity.this, XMPPService.class));
        AppLog.Log(TAG, "Xmap Service is stopped");
        setContentView(R.layout.activity_login);
        preferenceHelper = PreferenceHelper.getInstance();

        xmppListener = new XMPPListener() {
            @Override
            public void authenticatedSuccessfully() {
                AppLog.Log(TAG, "authenticatedSuccessfully() method is called");
                Utils.hideCustomProgressDialog();
            }

            @Override
            public void authenticateFailed() {
                AppLog.Log(TAG, "authenticateFailed() method is called");
                Utils.hideCustomProgressDialog();
                if (isFirstTimeupdatePaaword) {
                    isFirstTimeupdatePaaword = false;
                    if (isMyServiceRunning(XMPPService.class)) {
                        LoginActivity.this.stopService(new Intent(LoginActivity.this, XMPPService.class));
                    }
                    updateUserOnOpenfire();
                } else {
                    runOnUiThread(() -> Utils.showToast("authenticate Failed"));
                }
            }

            @Override
            public void authenticationError(String s) {
                // Utils.showToast(s);
                AppLog.Log(TAG, "authenticationError() method is called");
                Utils.hideCustomProgressDialog();
            }
        };
        //  TestFairy.begin(LoginActivity.this, LoginActivity.this.getResources().getString(R.string.testfairy_app_token));
        setUpToolbar();
        setUpViewAndClickAction();
        checkPermissionForStorage();

    }

    @Override
    public void closedOnConflict() {
        Utils.hideCustomProgressDialog();
    }

    @Override
    public void closedOnError() {
        Utils.hideCustomProgressDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void checkPermissionForStorage() {
        if (Utils.isNeedPermission()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            switch (requestCode) {
                case PERMISSION_STORAGE:
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                    } else {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
                        } else {
                            openPermissionNotifyDialog(getResources().getString(R.string.msg_permission_write_storage_notification), getResources().getString(R.string.permission_external_storage_deny_message));
                        }
                    }
                    break;
            }
        }


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PERMISSION_FOR_SETTING:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    openPermissionNotifyDialog(getResources().getString(R.string.msg_permission_write_storage_notification), getResources().getString(R.string.permission_external_storage_deny_message));
                }
                break;
            case PERMISSION_STORAGE:
                checkPermissionForStorage();
                break;
            case PERMISSION_FINGER_PRINT_SETTING:
                break;
            default:
                break;
        }
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
                LoginActivity.this.finish();
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


    void setUpToolbar() {
    }


    void setUpViewAndClickAction() {
        edt_password = findViewById(R.id.edt_password);
        edt_loginname = findViewById(R.id.edt_loginname);
        tvUserName = findViewById(R.id.tvUserName);
        ivUserImage = findViewById(R.id.ivUserImage);
        findViewById(R.id.tvLoginForgotten).setOnClickListener(this);
        findViewById(R.id.ivClose).setOnClickListener(this);
        AppCompatTextView tvLoginAppVersion = findViewById(R.id.tvLoginAppVersion);
        flRememberInfo = findViewById(R.id.flRememberInfo);
        llSavedDetail = findViewById(R.id.llSavedDetail);

        tvLoginAppVersion.setText(getString(R.string.text_version, BuildConfig.VERSION_NAME));
        findViewById(R.id.signin).setOnClickListener(this);
        findViewById(R.id.tvLoginUseTouchId).setOnClickListener(this);
        findViewById(R.id.tvLoginSwitchUser).setOnClickListener(this);

        cbLoginRememberMe = findViewById(R.id.cbLoginRememberMe);
        cbLoginRememberMe.setChecked(preferenceHelper.isRememberMe());


        if (preferenceHelper.isRememberMe()) {
            if (StringUtils.isNotEmpty(preferenceHelper.getLoginName()) && StringUtils.isNotEmpty(preferenceHelper.getPassword())) {
                edt_loginname.setVisibility(View.GONE);
                llSavedDetail.setVisibility(View.VISIBLE);
                tvUserName.setText(preferenceHelper.getFullName());
                findViewById(R.id.tvLoginSwitchUser).setVisibility(View.VISIBLE);
                String userUrl = preferenceHelper.getProfilePicture();
                if (StringUtils.isNotEmpty(userUrl)) {
                    if (!userUrl.startsWith("http")) {
                        userUrl = "http://" + userUrl;
                    }
                    Picasso.with(this).load(userUrl).placeholder(getResources().getDrawable(R.drawable.login_user_icon)).into(ivUserImage);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    FingerprintManager fingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
                    if (fingerprintManager.isHardwareDetected()) {
                        findViewById(R.id.tvLoginUseTouchId).setEnabled(true);
                        findViewById(R.id.tvLoginUseTouchId).setOnClickListener(this);
                    } else {
                        // Device doesn't support fingerprint authentication
                        findViewById(R.id.tvLoginUseTouchId).setEnabled(false);
                    }
                }

            }
        } else {
            edt_loginname.setVisibility(View.VISIBLE);
            ivUserImage.setImageResource(R.drawable.login_user_icon);
            findViewById(R.id.tvLoginSwitchUser).setVisibility(View.GONE);
            findViewById(R.id.tvLoginUseTouchId).setEnabled(false);
            llSavedDetail.setVisibility(View.GONE);
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.signin:
                String loginName = edt_loginname.getText().toString().trim();
                String password = edt_password.getText().toString().trim();
                if (preferenceHelper.isRememberMe() && edt_loginname.getVisibility() == View.GONE) {
                    loginName = preferenceHelper.getLoginName();
                }
                if (loginName.isEmpty()) {
                    Utils.showToast(getString(R.string.please_enter_login_name));
                } else if (password.isEmpty()) {
                    Utils.showToast(getString(R.string.please_enter_password));
                } else {
                    if (BatteryOptimizationUtil.isBatteryOptimizationAvailable(this)) {
                        if (!DateUtils.isToday(preferenceHelper.getMPRT())) {
                            preferenceHelper.putMPRT(System.currentTimeMillis());
                            android.support.v7.app.AlertDialog alertDialog = BatteryOptimizationUtil.getBatteryOptimizationDialog(this, this, this);
                            loginNameUser = loginName;
                            if (alertDialog != null)
                                alertDialog.show();
                        } else {
                            loginToServer(loginName, password);
                        }
                    } else {
                        loginToServer(loginName, password);
                    }
                }
                break;
            case R.id.tvLoginForgotten:
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
                break;
            case R.id.tvLoginUseTouchId:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    useTouchIdListener(DEFAULT_KEY_NAME);
                }
                break;
            case R.id.tvLoginSwitchUser:
                edt_loginname.setVisibility(View.VISIBLE);
                llSavedDetail.setVisibility(View.GONE);
                ivUserImage.setImageResource(R.drawable.login_user_icon);
                Animation animation1 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.button_slide);
                edt_loginname.startAnimation(animation1);
                findViewById(R.id.tvLoginSwitchUser).setVisibility(View.GONE);
                break;
            case R.id.ivClose:
                flRememberInfo.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void useTouchIdListener(String keyName) {
        KeyStore mKeyStore;
        KeyGenerator mKeyGenerator;
        Cipher defaultCipher;
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            throw new RuntimeException("Failed to get an instance of KeyStore", e);
        }
        try {
            mKeyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
        }
        try {
            defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get an instance of Cipher", e);
        }
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
        FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);


        if (!keyguardManager.isKeyguardSecure()) {
            // Show a message that the user hasn't set up a fingerprint or lock screen.
            // Toast.makeText(this, "Secure lock screen hasn't set up.\n" + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint", Toast.LENGTH_LONG).show();
            openFingerPrintSetupSettingDialog();
            return;
        }

        // Now the protection level of USE_FINGERPRINT permission is normal instead of dangerous.
        // See http://developer.android.com/reference/android/Manifest.permission.html#USE_FINGERPRINT
        // The line below prevents the false positive inspection from Android Studio
        // noinspection ResourceType
        if (!fingerprintManager.hasEnrolledFingerprints()) {
            // This happens when no fingerprints are registered.
            //   Toast.makeText(this, "Go to 'Settings -> Security -> Fingerprint' and register at least one fingerprint", Toast.LENGTH_LONG).show();
            openFingerPrintSetupSettingDialog();
            return;
        }

        try {
            mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);

            // This is a workaround to avoid crashes on devices whose API level is < 24
            // because KeyGenParameterSpec.Builder#setInvalidatedByBiometricEnrollment is only
            // visible on API level +24.
            // Ideally there should be a compat library for KeyGenParameterSpec.Builder but
            // which isn't available yet.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(true);
            }
            mKeyGenerator.init(builder.build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }

        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(keyName, null);
            defaultCipher.init(Cipher.ENCRYPT_MODE, key);

            FingerprintAuthenticationDialogFragment fragment = new FingerprintAuthenticationDialogFragment();
            fragment.setCryptoObject(new FingerprintManager.CryptoObject(defaultCipher));
            boolean useFingerprintPreference = mSharedPreferences.getBoolean(getString(R.string.use_fingerprint_to_authenticate_key), true);
            if (useFingerprintPreference) {
                fragment.setStage(FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
            } else {
                fragment.setStage(FingerprintAuthenticationDialogFragment.Stage.PASSWORD);
            }
            fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);

        } catch (KeyPermanentlyInvalidatedException e) {
            FingerprintAuthenticationDialogFragment fragment = new FingerprintAuthenticationDialogFragment();
            fragment.setCryptoObject(new FingerprintManager.CryptoObject(defaultCipher));
            fragment.setStage(FingerprintAuthenticationDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
            fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);

        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }


    }


    private HashMap<String, String> loginAPIParameters(String loginname, String password) {
        HashMap<String, String> LoginParam = new HashMap<>();
        LoginParam.put("AppsName", Utils.encrypt(BuildConfig.APPSTYPE));
        LoginParam.put("LoginName", Utils.encrypt(loginname));
        LoginParam.put("Password", Utils.encrypt(getEncryptPassword(password)));
        return LoginParam;

    }

    private HashMap<String, String> loginAPIParametersForOcean(String loginname, String password) {
        HashMap<String, String> LoginParam = new HashMap<>();
        LoginParam.put("AppsPlatform", Utils.encrypt("ANDROID"));
        LoginParam.put("AppsName", Utils.encrypt(BuildConfig.APPSTYPE));
        LoginParam.put("AppsVersionNo", Utils.encrypt(BuildConfig.VERSION_NAME));
        LoginParam.put("Environment", Utils.encrypt(BuildConfig.ENVIROMENT.replace("_", "").replace("-", "")));
        LoginParam.put("Password", Utils.encrypt(getEncryptPasswordForOcean(password)));
        LoginParam.put("SiteType", Utils.encrypt((BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT.replace("_", ""))));
        LoginParam.put("LoginName", Utils.encrypt(loginname));
        LoginParam.put("projectName", BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT.replace("_", ""));

        return LoginParam;
    }

    private HashMap<String, String> ssoLogin(String loginname, String password) {
        HashMap<String, String> LoginParam = new HashMap<>();
        LoginParam.put("AppsName", Utils.encrypt(BuildConfig.APPSTYPE));
        LoginParam.put("Password", Utils.encrypt(getEncryptPassword(password)));
        LoginParam.put("Username", Utils.encrypt(loginname));

        return LoginParam;
    }

    private HashMap<String, String> ssoLoginAPIParameters(String loginname) {
        HashMap<String, String> LoginParam = new HashMap<>();
        LoginParam.put("AppsName", Utils.encrypt(BuildConfig.APPSTYPE));
        LoginParam.put("LoginName", Utils.encrypt(loginname));
        return LoginParam;
    }

    private String getEncryptPassword(String passwordString) {
        String IrregularMd5Encrypted = "", NormalMd5Encrypted = "", EvenOddReversalEncrypted = "";
        try {
            IrregularMd5Encrypted = EncryptionMethods.IrregularMD5(passwordString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        NormalMd5Encrypted = EncryptionMethods.NormalMD5(passwordString);
        // return NormalMd5Encrypted;
        EvenOddReversalEncrypted = EncryptionMethods.EvenOddRevarsal(passwordString, "IRMS");
        return IrregularMd5Encrypted.trim() + "," + NormalMd5Encrypted.trim() + "," + EvenOddReversalEncrypted.trim();

    }

    private String getEncryptPasswordForOcean(String passwordString) {
        String IrregularMd5Encrypted = "", NormalMd5Encrypted = "", EvenOddReversalEncrypted = "";
        try {
            IrregularMd5Encrypted = EncryptionMethodsForOcean.IrregularMD5(passwordString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        NormalMd5Encrypted = EncryptionMethodsForOcean.NormalMD5(passwordString);
        // return NormalMd5Encrypted;
        EvenOddReversalEncrypted = EncryptionMethodsForOcean.EvenOddRevarsal(passwordString, "IRMS");
        return IrregularMd5Encrypted.trim() + "," + NormalMd5Encrypted.trim() + "," + EvenOddReversalEncrypted.trim();

    }

    private void loginToServer(final String loginname, final String password) {

        if (Utils.isInternetConnected()) {
            Utils.showCustomProgressDialog(LoginActivity.this, false);
            final Call<LoginAPI> loginAPICall;
            if (BuildConfig.COMPANYNAME.equalsIgnoreCase("innoways")) {
                loginAPICall = ApiHandler
                        .getCommonApiService()
                        .getLoginAspx(loginAPIParameters(loginname, password));
                loginAPICall.enqueue(new Callback<LoginAPI>() {
                    @Override
                    public void onResponse(@NonNull Call<LoginAPI> call, @NonNull Response<LoginAPI> response) {
                        Utils.hideCustomProgressDialog();LoginAPI loginAPI = response.body();
                        if (loginAPI != null) {
                            if (loginAPI.getStatus().equalsIgnoreCase("1")) {
                                LoginAPI.Datum datum = loginAPI.getData().get(0);

                                preferenceHelper.putPassword(password);
                                preferenceHelper.putCompanyName(BuildConfig.COMPANYNAME);
                                preferenceHelper.putUserType(datum.getUserType().trim());
                                preferenceHelper.putUserName(datum.getUserName().trim());
                                preferenceHelper.putOpenFireUserName(PreferenceHelper.getInstance().getCompanyName() + BuildConfig.ENVIROMENT + datum.getUserName().trim().replace("@", "#"));
                                preferenceHelper.putOpenfireSenderID(preferenceHelper.getOpenfireusername() + PreferenceHelper.getInstance().getOpenFireJIDSuffix());
                                String userUrl = datum.getPhotoUrl();
                                if (StringUtils.isNotEmpty(userUrl)) {
                                    if (!userUrl.startsWith("http")) {
                                        userUrl = "http://" + userUrl;
                                    }
                                }
                                preferenceHelper.putProfilePicture(userUrl);
                                preferenceHelper.putFullName(datum.getFullName());
                                preferenceHelper.putLoginName(datum.getLoginName().trim());

                                preferenceHelper.putRememberMe(cbLoginRememberMe.isChecked());
                                preferenceHelper.putIsLogin(true);

                                preferenceHelper.putOpenfireHttpRoot(datum.getIMInfo().get(0).getOpenfireHttpRoot());
                                preferenceHelper.putOpenfireHost(datum.getIMInfo().get(0).getOpenfireHost());
                                preferenceHelper.putOpenfireXmppPort(datum.getIMInfo().get(0).getOpenfireXmppPort());
                                preferenceHelper.putOpenfireHttpSecret(datum.getIMInfo().get(0).getOpenfireHttpSecret());
                                preferenceHelper.putOpenFireSiteType(datum.getIMInfo().get(0).getOpenFireSiteType());
                                preferenceHelper.putOpenFireJIDSuffix(datum.getIMInfo().get(0).getOpenFireJIDSuffix());
                                preferenceHelper.putOpenFireConferenceService(datum.getIMInfo().get(0).getOpenFireJIDSuffix().replace("@", "conference."));

                                checkUserOnOpenFireForOcean();
                            } else {
                                Utils.showToast(loginAPI.getMsg());
                            }
                        }

                    }

                    @Override
                    public void onFailure(@NonNull Call<LoginAPI> call, @NonNull Throwable t) {
                        Utils.hideCustomProgressDialog();
                        Utils.showToast(t.getMessage());
                    }
                });
            } else if (BuildConfig.COMPANYNAME.equalsIgnoreCase("oceanpark")) {
                //loginAPICall = ApiHandler.getApiService().getLogin(Utils.encrypt("ANDROID"), Utils.encrypt(BuildConfig.APPSTYPE), Utils.encrypt(BuildConfig.VERSION_NAME), Utils.encrypt(BuildConfig.ENVIROMENT.replace("_", "").replace("-", "")), Utils.encrypt(getEncryptPassword(password)), Utils.encrypt((versionName + BuildConfig.ENVIROMENT.replace("_", ""))), Utils.encrypt(loginname), (versionName + BuildConfig.ENVIROMENT.replace("_", "")));
                loginAPICall = ApiHandler.getApiService().getLogin(loginAPIParametersForOcean(loginname, password));
                loginAPICall.enqueue(new Callback<LoginAPI>() {
                    @Override
                    public void onResponse(@NonNull Call<LoginAPI> call, @NonNull Response<LoginAPI> response) {
                        Utils.hideCustomProgressDialog();
                        LoginAPI loginAPI = response.body();
                        if (loginAPI != null) {
                            if (loginAPI.getStatus().equalsIgnoreCase("1")) {
                                LoginAPI.Datum datum = loginAPI.getData().get(0);
                                preferenceHelper.putPassword(password);
                                preferenceHelper.putCompanyName(BuildConfig.COMPANYNAME);
                                preferenceHelper.putUserType(datum.getUserType().trim());
                                preferenceHelper.putUserName(datum.getUserName().trim());
                                preferenceHelper.putOpenFireUserName(PreferenceHelper.getInstance().getCompanyName() + BuildConfig.ENVIROMENT + datum.getUserName().trim().replace("@", "#"));
                                preferenceHelper.putOpenfireSenderID(preferenceHelper.getOpenfireusername() + PreferenceHelper.getInstance().getOpenFireJIDSuffix());
                                String userUrl = datum.getPhotoUrl();
                                if (StringUtils.isNotEmpty(userUrl)) {
                                    if (!userUrl.startsWith("http")) {
                                        userUrl = "http://" + userUrl;
                                    }
                                }
                                preferenceHelper.putProfilePicture(userUrl);
                                preferenceHelper.putFullName(datum.getFullName());
                                preferenceHelper.putLoginName(datum.getLoginName().trim());

                                preferenceHelper.putRememberMe(cbLoginRememberMe.isChecked());
                                preferenceHelper.putIsLogin(true);

                                preferenceHelper.putOpenfireHttpRoot(datum.getIMInfo().get(0).getOpenfireHttpRoot());
                                preferenceHelper.putOpenfireHost(datum.getIMInfo().get(0).getOpenfireHost());
                                preferenceHelper.putOpenfireXmppPort(datum.getIMInfo().get(0).getOpenfireXmppPort());
                                preferenceHelper.putOpenfireHttpSecret(datum.getIMInfo().get(0).getOpenfireHttpSecret());
                                preferenceHelper.putOpenFireSiteType(datum.getIMInfo().get(0).getOpenFireSiteType());
                                preferenceHelper.putOpenFireJIDSuffix(datum.getIMInfo().get(0).getOpenFireJIDSuffix());
                                preferenceHelper.putOpenFireConferenceService(datum.getIMInfo().get(0).getOpenFireJIDSuffix().replace("@", "conference."));

                                checkUserOnOpenFireForOcean();
                            } else {
                                Utils.showToast(loginAPI.getMsg());
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LoginAPI> call, @NonNull Throwable t) {
                        Utils.hideCustomProgressDialog();
                        Utils.showToast(t.getMessage());
                    }
                });

            } else {
                loginAPICall = ApiHandler.getCommonApiService().getSSOLogin(ssoLogin(loginname, password));
                loginAPICall.enqueue(new Callback<LoginAPI>() {
                    @Override
                    public void onResponse(@NonNull Call<LoginAPI> call, @NonNull Response<LoginAPI> response) {
                        Utils.hideCustomProgressDialog();
                        LoginAPI loginAPI = response.body();
                        if (loginAPI != null) {
                            if (loginAPI.getStatus().equalsIgnoreCase("1")) {
                                preferenceHelper.putPassword(password);
                                if (loginAPI.getData().size() == 1) {
                                    //preferenceHelper.putCompanyName(versionName);
                                    getLoginInfo(loginAPI.getData().get(0).getUserName(), password);
                                } else
                                    showCompanySelectDialog(loginAPI);
                            } else {
                                Utils.showToast(loginAPI.getMsg());
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LoginAPI> call, @NonNull Throwable t) {
                        Utils.hideCustomProgressDialog();
                        Utils.showToast(t.getMessage());
                    }
                });
            }

        } else {
            Utils.showToast(getString(R.string.msg_no_internet_connect));
        }
    }

    private void getLoginInfo(String loginname, String password) {

        if (Utils.isInternetConnected()) {
            Utils.showCustomProgressDialog(LoginActivity.this, false);
            final Call<LoginAPI> loginAPICall;

            //loginAPICall = ApiHandler.getApiService().getLoginInfo(Utils.encrypt("ANDROID"), Utils.encrypt(BuildConfig.APPSTYPE), Utils.encrypt(BuildConfig.VERSION_NAME), Utils.encrypt(BuildConfig.ENVIROMENT.replace("_", "").replace("-", "")),  Utils.encrypt((PreferenceHelper.getInstance().getCompanyName() + BuildConfig.ENVIROMENT.replace("_", ""))), Utils.encrypt(loginname), Utils.encrypt(loginname), PreferenceHelper.getInstance().getCompanyName() + BuildConfig.ENVIROMENT.replace("_", ""));
            loginAPICall = ApiHandler.getCommonApiService().getLoginInfo(ssoLoginAPIParameters(loginname));
            loginAPICall.enqueue(new Callback<LoginAPI>() {
                @Override
                public void onResponse(@NonNull Call<LoginAPI> call, @NonNull Response<LoginAPI> response) {
                    Utils.hideCustomProgressDialog();
                    LoginAPI loginAPI = response.body();
                    if (loginAPI != null) {

                        if (loginAPI.getStatus().equalsIgnoreCase("1")) {
                            LoginAPI.Datum datum = loginAPI.getData().get(0);
                            preferenceHelper.putUserType(datum.getUserType().trim());
                            preferenceHelper.putUserName(datum.getUserName().trim());
                            preferenceHelper.putOpenFireUserName(BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT + loginname.trim().replace("@", "#"));
                            preferenceHelper.putOpenfireSenderID(preferenceHelper.getOpenfireusername() + PreferenceHelper.getInstance().getOpenFireJIDSuffix());
                            String userUrl = datum.getPhotoUrl();
                            if (StringUtils.isNotEmpty(userUrl)) {
                                if (!userUrl.startsWith("http")) {
                                    userUrl = "http://" + userUrl;
                                }
                            }
                            preferenceHelper.putProfilePicture(userUrl);
                            preferenceHelper.putFullName(datum.getFullName());
                            preferenceHelper.putLoginName(datum.getLoginName().trim());

                            preferenceHelper.putRememberMe(cbLoginRememberMe.isChecked());
                            preferenceHelper.putIsLogin(true);

                            preferenceHelper.putOpenfireHttpRoot(datum.getIMInfo().get(0).getOpenfireHttpRoot());
                            preferenceHelper.putOpenfireHost(datum.getIMInfo().get(0).getOpenfireHost());
                            preferenceHelper.putOpenfireXmppPort(datum.getIMInfo().get(0).getOpenfireXmppPort());
                            preferenceHelper.putOpenfireHttpSecret(datum.getIMInfo().get(0).getOpenfireHttpSecret());
                            preferenceHelper.putOpenFireSiteType(datum.getIMInfo().get(0).getOpenFireSiteType());
                            preferenceHelper.putOpenFireJIDSuffix(datum.getIMInfo().get(0).getOpenFireJIDSuffix());
                            preferenceHelper.putOpenFireConferenceService(datum.getIMInfo().get(0).getOpenFireJIDSuffix().replace("@", "conference."));

                            checkUserOnOpenFireForOcean();
                        } else {
                            Utils.showToast(loginAPI.getMsg());
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<LoginAPI> call, @NonNull Throwable t) {
                    Utils.hideCustomProgressDialog();
                    Utils.showToast(t.getMessage());
                }
            });

        } else {
            Utils.showToast(getString(R.string.msg_no_internet_connect));
        }
    }

    private void showCompanySelectDialog(final LoginAPI loginRes) {
        final Dialog dialog = new Dialog(LoginActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.company_select_dialog_layout, null, false);

        ImageView logo1 = view.findViewById(R.id.iv_logo_1);
        TextView header1 = view.findViewById(R.id.txt_text_first);
        ImageView logo2 = view.findViewById(R.id.iv_logo_2);
        TextView header2 = view.findViewById(R.id.txt_text_second);

        CardView cardView1 = view.findViewById(R.id.card_view_1);
        CardView cardView2 = view.findViewById(R.id.card_view_2);

        String siteType1 = BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT.replace("_", "");
        String siteType2 = BuildConfig.COMPANYNAME + BuildConfig.ENVIROMENT.replace("_", "");

        if (siteType1.equalsIgnoreCase("atoms")) {
            Glide.with(logo1.getContext())
                    .load(getResources().getDrawable(R.drawable.atoms_logo))
                    .into(logo1);
        } else if (siteType1.equalsIgnoreCase("irms")) {
            Glide.with(logo1.getContext())
                    .load(getResources().getDrawable(R.drawable.trms_logo))
                    .into(logo1);
        } else {
            Glide.with(logo1.getContext())
                    .load(getResources().getDrawable(R.drawable.atc_logo))
                    .into(logo1);
        }

        if (siteType2.equalsIgnoreCase("atoms")) {
            Glide.with(logo2.getContext())
                    .load(getResources().getDrawable(R.drawable.atoms_logo))
                    .into(logo2);
        } else if (siteType2.equalsIgnoreCase("irms")) {
            Glide.with(logo2.getContext())
                    .load(getResources().getDrawable(R.drawable.trms_logo))
                    .into(logo2);
        } else {
            Glide.with(logo2.getContext())
                    .load(getResources().getDrawable(R.drawable.atc_logo))
                    .into(logo2);
        }

        header1.setText(loginRes.getData().get(0).getCompanyName());
        cardView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                preferenceHelper.putCompanyName(loginRes.getData().get(0).getSiteType());
                getLoginInfo(loginRes.getData().get(0).getUserName(), loginRes.getData().get(0).getPassword());
            }
        });

      /*  Glide.with(logo2.getContext())
                .load(loginRes.getData().get(1).getCompanyLogoUrl())
                .into(logo2);*/
        header2.setText(loginRes.getData().get(1).getCompanyName());
        cardView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                preferenceHelper.putCompanyName(loginRes.getData().get(1).getSiteType());
                getLoginInfo(loginRes.getData().get(0).getUserName(), loginRes.getData().get(0).getPassword());
            }
        });

        dialog.setContentView(view);
        final Window window = dialog.getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
        dialog.show();
    }

    public void checkUserOnOpenFireForOcean() {
        AppLog.Log("checkUserOnOpenFireForOcean() called", "");
        try {
            if (Utils.isInternetConnected()) {
                Utils.showCustomProgressDialog(LoginActivity.this, false);
                Call<CheckUserOnOpenFire> checkUserOnOpenFireCall = ApiHandler.getOpenfireApiService().checkUserOnOpenFire(PreferenceHelper.getInstance().getOpenfireHttpSecret(), preferenceHelper.getOpenfireusername().replace("@", "#"));
                checkUserOnOpenFireCall.enqueue(new Callback<CheckUserOnOpenFire>() {
                    @Override
                    public void onResponse(@NonNull Call<CheckUserOnOpenFire> call, @NonNull Response<CheckUserOnOpenFire> response) {
                        Utils.hideCustomProgressDialog();
                        try {
                            if (response.isSuccessful()) {
                                if (isMyServiceRunning(XMPPService.class)) {
                                    Utils.showCustomProgressDialog(LoginActivity.this, false);
                                    LoginActivity.this.stopService(new Intent(LoginActivity.this, XMPPService.class));
                                    LoginActivity.this.startService(new Intent(LoginActivity.this, XMPPService.class));
                                    XMPPConfiguration.getInstance().setXMPPInterface(xmppListener);
                                    if (XMPPService.XMPPConnection != null) {
                                        XMPPConfiguration.getInstance().setXmppErrorListener(xmppErrorListener);
                                    }
                                } else {
                                    Utils.showCustomProgressDialog(LoginActivity.this, false);
                                    LoginActivity.this.startService(new Intent(LoginActivity.this, XMPPService.class));
                                    XMPPConfiguration.getInstance().setXMPPInterface(xmppListener);
                                    if (XMPPService.XMPPConnection != null) {
                                        XMPPConfiguration.getInstance().setXmppErrorListener(xmppErrorListener);
                                    }
                                }
                            } else {
                                addUserInOpenFireForOcean();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<CheckUserOnOpenFire> call, @NonNull Throwable t) {
                        Utils.showToast(t.getMessage());
                        Utils.hideCustomProgressDialog();
                        addUserInOpenFireForOcean();
                        AppLog.Log("checkUserOnOpenFireForOcean() onFailure() called", "");
                    }
                });
            } else {
                Utils.showToast(getString(R.string.msg_no_internet_connect));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   /* public void checkUserOnOpenFire() {
        if (Utils.isInternetConnected()) {
            Utils.showCustomProgressDialog(LoginActivity.this, false);
            Call<CheckUserOnOpenFire> checkUserOnOpenFireCall = ApiHandler.getOpenfireApiService().checkUserOnOpenFire(OPENFIRE_HOST_SERVER_KEY, preferenceHelper.getOpenfireusername().replace("@", "#"));
            checkUserOnOpenFireCall.enqueue(new Callback<CheckUserOnOpenFire>() {
                @Override
                public void onResponse(@NonNull Call<CheckUserOnOpenFire> call, @NonNull Response<CheckUserOnOpenFire> response) {
                    Utils.hideCustomProgressDialog();
                    try {
                        if (response.isSuccessful()) {
                            if (isMyServiceRunning(XMPPService.class)) {
                                Utils.showCustomProgressDialog(LoginActivity.this, false);
                                LoginActivity.this.stopService(new Intent(LoginActivity.this, XMPPService.class));
                                LoginActivity.this.startService(new Intent(LoginActivity.this, XMPPService.class));
                                XMPPConfiguration.getInstance().setXMPPInterface(xmppListener);
                                if (XMPPService.XMPPConnection != null) {
                                    XMPPConfiguration.getInstance().setXmppErrorListener(xmppErrorListener);
                                }
                            } else {
                                Utils.showCustomProgressDialog(LoginActivity.this, false);
                                LoginActivity.this.startService(new Intent(LoginActivity.this, XMPPService.class));
                                XMPPConfiguration.getInstance().setXMPPInterface(xmppListener);
                                if (XMPPService.XMPPConnection != null) {
                                    XMPPConfiguration.getInstance().setXmppErrorListener(xmppErrorListener);
                                }
                            }
                        } else {
                            addUserInOpenFire();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<CheckUserOnOpenFire> call, @NonNull Throwable t) {
                    Utils.showToast(t.getMessage());
                    Utils.hideCustomProgressDialog();
                    addUserInOpenFire();
                }
            });
        } else {
            Utils.showToast(getString(R.string.msg_no_internet_connect));
        }
    }*/

   /* private void addUserInOpenFire() {
        Utils.showCustomProgressDialog(LoginActivity.this, false);
        String loginName = preferenceHelper.getLoginName();
        String password = preferenceHelper.getPassword();
        String fullname = preferenceHelper.getFullName();
        Call<ResponseBody> responseBodyCall = ApiHandler.getOpenfireApiService().createUserOnOpenfire(OPENFIRE_HOST_SERVER_KEY, createUserJsonMap(loginName, password, fullname));
        responseBodyCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Utils.hideCustomProgressDialog();
                if (response.code() == 201) {
                    if (isMyServiceRunning(XMPPService.class)) {
                        Utils.showCustomProgressDialog(LoginActivity.this, false);
                        LoginActivity.this.stopService(new Intent(LoginActivity.this, XMPPService.class));
                        LoginActivity.this.startService(new Intent(LoginActivity.this, XMPPService.class));
                        XMPPConfiguration.getInstance().setXMPPInterface(xmppListener);
                        if (XMPPService.XMPPConnection != null) {
                            XMPPConfiguration.getInstance().setXmppErrorListener(xmppErrorListener);
                        }
                    } else {
                        Utils.showCustomProgressDialog(LoginActivity.this, false);
                        LoginActivity.this.startService(new Intent(LoginActivity.this, XMPPService.class));
                        XMPPConfiguration.getInstance().setXMPPInterface(xmppListener);
                        if (XMPPService.XMPPConnection != null) {
                            XMPPConfiguration.getInstance().setXmppErrorListener(xmppErrorListener);
                        }
                    }
                } else if (response.code() == 409) {
                    try {
                        JSONObject jsonObject = new JSONObject(String.valueOf(response.body()));
                        Utils.showToast(jsonObject.getString("message") + ", User Already Exist.");
                    } catch (JSONException e) {
                        Utils.showToast("User Already Exist.");
                    }
                } else if (response.code() == 500) {
                    Utils.showToast("User Already Exist.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Utils.hideCustomProgressDialog();
                Utils.showToast(t.getMessage());
            }
        });
    }*/

    private void addUserInOpenFireForOcean() {
        AppLog.Log("addUserInOpenFireForOcean() called", "");
        try {
            Utils.showCustomProgressDialog(LoginActivity.this, false);
            String loginName = preferenceHelper.getLoginName();
            String password = preferenceHelper.getPassword();
            String fullname = preferenceHelper.getFullName();
            Call<ResponseBody> responseBodyCall = ApiHandler.getOpenfireApiService().createUserOnOpenfire(PreferenceHelper.getInstance().getOpenfireHttpSecret(), createUserJsonMap(loginName, password, fullname));
            responseBodyCall.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    Utils.hideCustomProgressDialog();
                    if (response.code() == 201) {
                        if (isMyServiceRunning(XMPPService.class)) {
                            Utils.showCustomProgressDialog(LoginActivity.this, false);
                            LoginActivity.this.stopService(new Intent(LoginActivity.this, XMPPService.class));
                            LoginActivity.this.startService(new Intent(LoginActivity.this, XMPPService.class));
                            XMPPConfiguration.getInstance().setXMPPInterface(xmppListener);
                            if (XMPPService.XMPPConnection != null) {
                                XMPPConfiguration.getInstance().setXmppErrorListener(xmppErrorListener);
                            }
                        } else {
                            Utils.showCustomProgressDialog(LoginActivity.this, false);
                            LoginActivity.this.startService(new Intent(LoginActivity.this, XMPPService.class));
                            XMPPConfiguration.getInstance().setXMPPInterface(xmppListener);
                            if (XMPPService.XMPPConnection != null) {
                                XMPPConfiguration.getInstance().setXmppErrorListener(xmppErrorListener);
                            }
                        }
                    } else if (response.code() == 409) {
                        try {
                            JSONObject jsonObject = new JSONObject(String.valueOf(response.body()));
                            Utils.showToast(jsonObject.getString("message") + ", User Already Exist.");
                        } catch (JSONException e) {
                            Utils.showToast("User Already Exist.");
                        }
                    } else if (response.code() == 500) {
                        Utils.showToast("User Already Exist.");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    Utils.hideCustomProgressDialog();
                    Utils.showToast(t.getMessage());

                    AppLog.Log("addUserInOpenFireForOcean() onFailure() called", "");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JsonObject createUserJsonMap(String loginname, String password, String fullname) {
        JsonObject gsonObject = new JsonObject();
        try {
            JSONObject jsonObj_createuser = new JSONObject();
            if (loginname.contains("@")) {
                jsonObj_createuser.put("email", loginname);
            }
            jsonObj_createuser.put("username", preferenceHelper.getOpenfireusername());
            jsonObj_createuser.put("password", password);
            jsonObj_createuser.put("name", fullname);

            JsonParser jsonParser = new JsonParser();
            gsonObject = (JsonObject) jsonParser.parse(jsonObj_createuser.toString());

        } catch (JSONException e) {
        }
        return gsonObject;
    }

    public void updateUserOnOpenfire() {
        AppLog.Log(TAG, "updateUserOnOpenfire() method is called");

        if (Utils.isInternetConnected()) {
            Utils.showCustomProgressDialog(LoginActivity.this, false);
            String loginName = preferenceHelper.getLoginName();
            String password = preferenceHelper.getPassword();
            String email = "";
            if (loginName.contains("@")) {
                email = loginName;
            }
            Map<String, String> update_user_param = new HashMap<>();
            update_user_param.put("type", "update");
            update_user_param.put("secret", PreferenceHelper.getInstance().getOpenfireHttpSecret());
            update_user_param.put("username", preferenceHelper.getOpenfireusername());
            update_user_param.put("password", password);
            if (email.length() > 0) {
                update_user_param.put("email", email);
            }

            Call<ResponseBody> responseBodyCall = ApiHandler.getOpenfireApiService().updateUseronOpenfireServer(update_user_param);
            responseBodyCall.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    Utils.hideCustomProgressDialog();
                    if (response.code() == 200) {
                        try {
                            String s = response.body().string();
                            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                            DocumentBuilder db;
                            try {
                                db = dbf.newDocumentBuilder();
                                InputSource is = new InputSource();
                                is.setCharacterStream(new StringReader(s));
                                try {
                                    Document doc = db.parse(is);
                                    String message = doc.getDocumentElement().getTextContent();
                                    if (message.equalsIgnoreCase("ok")) {
                                        if (isMyServiceRunning(XMPPService.class)) {
                                            Utils.showCustomProgressDialog(LoginActivity.this, false);
                                            LoginActivity.this.stopService(new Intent(LoginActivity.this, XMPPService.class));
                                            LoginActivity.this.startService(new Intent(LoginActivity.this, XMPPService.class));
                                            XMPPConfiguration.getInstance().setXMPPInterface(xmppListener);
                                            if (XMPPService.XMPPConnection != null) {
                                                XMPPConfiguration.getInstance().setXmppErrorListener(xmppErrorListener);
                                            }
                                        } else {
                                            Utils.showCustomProgressDialog(LoginActivity.this, false);
                                            LoginActivity.this.startService(new Intent(LoginActivity.this, XMPPService.class));
                                            XMPPConfiguration.getInstance().setXMPPInterface(xmppListener);
                                            if (XMPPService.XMPPConnection != null) {
                                                XMPPConfiguration.getInstance().setXmppErrorListener(xmppErrorListener);
                                            }
                                        }
                                    }
                                } catch (SAXException e) {
                                    Utils.showToast(e.getMessage());
                                    // handle SAXException
                                } catch (IOException e) {
                                    // handle IOException
                                    Utils.showToast(e.getMessage());
                                }
                            } catch (ParserConfigurationException e1) {
                                // handle ParserConfigurationException
                                Utils.showToast(e1.getMessage());
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                            Utils.showToast(e.getMessage());
                        }
                        //
                    } else {
                        Utils.showToast(LoginActivity.this.getString(R.string.msg_failed_to_update_user_on_openfire));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    Utils.hideCustomProgressDialog();
                    Utils.showToast(t.getMessage());
                }
            });


        } else {
            Utils.showToast(LoginActivity.this.getString(R.string.msg_no_internet_connect));
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.hideCustomProgressDialog();
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        if (preferenceHelper.isRememberMe()) {
            if (StringUtils.isNotEmpty(preferenceHelper.getLoginName()) && StringUtils.isNotEmpty(preferenceHelper.getPassword())) {
                edt_loginname.setVisibility(View.GONE);
                llSavedDetail.setVisibility(View.VISIBLE);
                findViewById(R.id.tvLoginSwitchUser).setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    FingerprintManager fingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
                    assert fingerprintManager != null;
                    if (fingerprintManager.isHardwareDetected()) {
                        // Everything is ready for fingerprint authentication

                        findViewById(R.id.tvLoginUseTouchId).setEnabled(true);
                        findViewById(R.id.tvLoginUseTouchId).setOnClickListener(this);
                    } else {
                        // Device doesn't support fingerprint authentication
                        findViewById(R.id.tvLoginUseTouchId).setEnabled(false);
                    }
                }
            }
        } else {
            edt_loginname.setVisibility(View.VISIBLE);
            llSavedDetail.setVisibility(View.GONE);
            findViewById(R.id.tvLoginSwitchUser).setVisibility(View.GONE);
            findViewById(R.id.tvLoginUseTouchId).setEnabled(false);
        }

    }

    private void openFingerPrintSetupSettingDialog() {
        if (customDialogEnable != null && customDialogEnable.isShowing()) {
            return;
        }
        customDialogEnable = new CustomDialogEnable(this, getResources().getString(R.string.msg_set_up_finger_print), getResources().getString(R.string.no), getResources().getString(R.string.yes)) {
            @Override
            public void doWithEnable() {
                closedPermissionDialog();
                startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), PERMISSION_FINGER_PRINT_SETTING);
            }

            @Override
            public void doWithDisable() {
                closedPermissionDialog();
            }
        };
        customDialogEnable.show();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onPurchased(boolean withFingerprint, @Nullable FingerprintManager.CryptoObject cryptoObject) {
        if (withFingerprint) {
            // If the user has authenticated with fingerprint, verify that using cryptography and
            // then show the confirmation message.
            assert cryptoObject != null;
            tryEncrypt(cryptoObject.getCipher());
        } else {
            // Authentication happened with backup password. Just show the confirmation message.
            showConfirmation(null);
        }
    }

    private void showConfirmation(byte[] encrypted) {
        if (BatteryOptimizationUtil.isBatteryOptimizationAvailable(this)) {
            if (!DateUtils.isToday(preferenceHelper.getMPRT())) {
                preferenceHelper.putMPRT(System.currentTimeMillis());
                android.support.v7.app.AlertDialog alertDialog = BatteryOptimizationUtil.getBatteryOptimizationDialog(this, this, this);
                loginNameUser = preferenceHelper.getLoginName();
                if (alertDialog != null)
                    alertDialog.show();
            } else {
                loginToServer(preferenceHelper.getLoginName(), preferenceHelper.getPassword());
            }
        } else {
            loginToServer(preferenceHelper.getLoginName(), preferenceHelper.getPassword());
        }
    }

    /**
     * Tries to encrypt some data with the generated key in {@link #} which is
     * only works if the user has just authenticated via fingerprint.
     */
    private void tryEncrypt(Cipher cipher) {
        try {
            byte[] encrypted = cipher.doFinal(SECRET_MESSAGE.getBytes());
            showConfirmation(encrypted);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            Toast.makeText(this, "Failed to encrypt the data with the generated key. " + "Retry the purchase", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to encrypt the data with the generated key." + e.getMessage());
        }
    }


    private void checkVersionApi() {
        if (Utils.isInternetConnected()) {
            Utils.showCustomProgressDialog(LoginActivity.this, false);
            Map<String, String> stringStringMap = new HashMap<>();
            stringStringMap.put("AppsType", Utils.encrypt(BuildConfig.APPSTYPE));
            Call<AppVersion> appVersionCall;
            if (BuildConfig.COMPANYNAME.equalsIgnoreCase("innoways")) {
                appVersionCall = ApiHandler.getCommonApiService().checkVersionServerAspx(stringStringMap);
            } else {
                appVersionCall = ApiHandler.getCommonApiService().checkVersionServer(stringStringMap);
            }
            appVersionCall.enqueue(new Callback<AppVersion>() {
                @Override
                public void onResponse(@NonNull Call<AppVersion> call, @NonNull Response<AppVersion> response) {
                    Utils.hideCustomProgressDialog();
                    AppVersion appVersion = response.body();
                    if (appVersion != null) {
                        try {
                            if (!appVersion.getData().get(0).getAndroidVersion().equalsIgnoreCase(BuildConfig.VERSION_NAME)) {
                                openForceUpdateDialog();
                            }
                        } catch (IndexOutOfBoundsException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }

                @Override
                public void onFailure(@NonNull Call<AppVersion> call, @NonNull Throwable t) {
                    Utils.hideCustomProgressDialog();
                    Utils.showToast(t.getMessage());
                }
            });

        } else {
            Utils.showToast(getString(R.string.msg_no_internet_connect));
        }
    }

    private void openForceUpdateDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setCancelable(false);
        alertDialog.setTitle(getResources().getString(R.string.str_title_app_update));
        alertDialog.setMessage(getResources().getString(R.string.str_msg_app_update));
        alertDialog.setPositiveButton(getResources().getString(R.string.str_btn_update), (dialog, which) -> {
            UpdateApk();
            finish();
        });
        alertDialog.setNegativeButton(getResources().getString(R.string.str_btn_quit), (dialog, which) -> {
            dialog.cancel();
            finish();
        });

        alertDialog.show();
    }

    private void UpdateApk() {
        Intent httpIntent = new Intent(Intent.ACTION_VIEW);
        httpIntent.setData(Uri.parse(BuildConfig.UPDATE_APK_URL));
        startActivity(httpIntent);
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }


    @Override
    public void onBatteryOptimizationAccepted() {
    }

    @Override
    public void onBatteryOptimizationCanceled() {
        loginToServer(loginNameUser, edt_password.getText().toString());
    }

}

