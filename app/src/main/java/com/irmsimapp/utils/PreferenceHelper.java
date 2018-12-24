/*
 * Copyright 2017 ElluminatiInc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irmsimapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.irmsimapp.BuildConfig;

import java.util.HashSet;


public class PreferenceHelper {

    /**
     * Preference Const
     */
    private static final String PREF_NAME = BuildConfig.APPLICATION_ID;
    private static final String USERNAME = "username";
    private static final String OPENFIREUSERNAME = "openfire_username";
    private static final String OPENFIRE_SENDER_ID = "openfire_sender_id";
    private static final String LOGINNAME = "loginname";
    private static final String PASSWORD = "password";
    private static final String USERTYPE = "usertype";
    private static final String PROFILEPICTURE = "profile_picture";
    private static final String FULLNAME = "fullname";
    private static final String IS_REMEMBER_ME = "is_remember_me";
    private static final String IS_LOGIN = "is_login";
    private static final String GROUP_LIST = "group_list";
    private static final String IS_DONE = "is_done";
    private static final String DEVICE_TOKEN = "device_token";
    private static final String USER_EMAIL = "user_email";
    private static final String USER_PHONE = "user_phone";
    private static final String USER_MOBILE = "user_mobile";
    private static final String MPRT = "manufacture_permission_request_time";

    private static final String OPENFIRE_HTTP_ROOT = "OpenfireHttpRoot";
    private static final String OPENFIRE_HOST = "OpenfireHost";
    private static final String OPENFIRE_XMPP_PORT = "OpenfireXmppPort";
    private static final String OPENFIRE_HTTP_SECRET = "OpenfireHttpSecret";
    private static final String OPENFIRE_SITE_TYPE = "OpenFireSiteType";
    private static final String OPENFIRE_JIDSUFFIX = "OpenFireJIDSuffix";
    private static final String OPENFIRE_CONFERENCE_SERVICE = "OpenFireConferenceService";
    private static final String COMPANY_NAME = "company_name";


    private static SharedPreferences app_preferences;
    private static PreferenceHelper preferenceHelper;


    private PreferenceHelper(Context context) {
        app_preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static PreferenceHelper getInstance() {
        if (preferenceHelper == null) {
            preferenceHelper = new PreferenceHelper(MyApp.getContext());
        }
        return preferenceHelper;
    }

    public String getDeviceToken() {
        return app_preferences.getString(DEVICE_TOKEN, null);
    }


    public void putDeviceToken(String fcm_token) {
        app_preferences.edit().putString(DEVICE_TOKEN, fcm_token).apply();
    }

    public long getMPRT() {
        return app_preferences.getLong(MPRT, 0);
    }


    public void putMPRT(long mprt) {
        app_preferences.edit().putLong(MPRT, mprt).apply();
    }

    public String getUsername() {
        return app_preferences.getString(USERNAME, null);
    }

    public String getUserPhone() {
        return app_preferences.getString(USER_PHONE, null);
    }

    public String getUserEmail() {
        return app_preferences.getString(USER_EMAIL, null);
    }

    public String getUserMobile() {
        return app_preferences.getString(USER_MOBILE, null);
    }

    public String getOpenfireusername() {
        return app_preferences.getString(OPENFIREUSERNAME, null);
    }

    public String getOpenfireSenderId() {
        return app_preferences.getString(OPENFIREUSERNAME, null);
    }

    public String getFullName() {
        return app_preferences.getString(FULLNAME, null);
    }

    public boolean isDone() {
        return app_preferences.getBoolean(IS_DONE, false);
    }


    public void putIsDone(boolean isDone) {
        app_preferences.edit().putBoolean(IS_DONE, isDone).apply();
    }

    public void putUserName(String username) {
        app_preferences.edit().putString(USERNAME, username).apply();
    }

    public void putUserEmail(String email) {
        app_preferences.edit().putString(USER_EMAIL, email).apply();
    }

    public void putUserPhone(String phone) {
        app_preferences.edit().putString(USER_PHONE, phone).apply();
    }

    public void putUserMobile(String mobile) {
        app_preferences.edit().putString(USER_MOBILE, mobile).apply();
    }

    public void putOpenFireUserName(String username) {
        app_preferences.edit().putString(OPENFIREUSERNAME, username).apply();
    }

    public void putOpenfireSenderID(String username) {
        app_preferences.edit().putString(OPENFIRE_SENDER_ID, username).apply();
    }


    public void putRememberMe(boolean b) {
        app_preferences.edit().putBoolean(IS_REMEMBER_ME, b).apply();

    }

    public boolean isRememberMe() {
        return app_preferences.getBoolean(IS_REMEMBER_ME, false);
    }

    public void putIsLogin(boolean b) {
        app_preferences.edit().putBoolean(IS_LOGIN, b).apply();

    }

    public boolean isLogin() {
        return app_preferences.getBoolean(IS_LOGIN, false);
    }


    public void putFullName(String fullname) {
        SharedPreferences.Editor editor = app_preferences.edit();
        editor.putString(FULLNAME, fullname);
        editor.apply();
    }

    public void putPassword(String password) {
        app_preferences.edit().putString(PASSWORD, password).apply();

    }

    public void putUserType(String userType) {
        app_preferences.edit().putString(USERTYPE, userType).apply();

    }

    public void putProfilePicture(String profilePicture) {
        app_preferences.edit().putString(PROFILEPICTURE, profilePicture).apply();
    }

    public void putLoginName(String loginname) {
        app_preferences.edit().putString(LOGINNAME, loginname).apply();
    }

    public String getLoginName() {
        return app_preferences.getString(LOGINNAME, null);
    }

    public String getPassword() {
        return app_preferences.getString(PASSWORD, null);
    }

    public String getUserType() {
        return app_preferences.getString(USERTYPE, null);
    }

    public String getProfilePicture() {
        return app_preferences.getString(PROFILEPICTURE, null);
    }

    public HashSet<String> getGroups() {
        return (HashSet<String>) app_preferences.getStringSet(GROUP_LIST, null);
    }

    public void setGroups(HashSet<String> groups) {
        app_preferences.edit().putStringSet(GROUP_LIST, groups).apply();
    }

    public void putOpenfireHttpRoot(String OpenfireHttpRoot) {
        app_preferences.edit().putString(OPENFIRE_HTTP_ROOT, OpenfireHttpRoot).apply();
    }

    public String getOpenfireHttpRoot() {
        return app_preferences.getString(OPENFIRE_HTTP_ROOT, null);
    }

    public void putOpenfireHost(String OpenfireHost) {
        app_preferences.edit().putString(OPENFIRE_HOST, OpenfireHost).apply();
    }

    public String getOpenfireHost() {
        return app_preferences.getString(OPENFIRE_HOST, null);
    }

    public void putOpenfireXmppPort(String OpenfireXmppPort) {
        app_preferences.edit().putString(OPENFIRE_XMPP_PORT, OpenfireXmppPort).apply();
    }

    public String getOpenfireXmppPort() {
        return app_preferences.getString(OPENFIRE_XMPP_PORT, null);
    }

    public void putOpenfireHttpSecret(String OpenfireHttpSecret) {
        app_preferences.edit().putString(OPENFIRE_HTTP_SECRET, OpenfireHttpSecret).apply();
    }

    public String getOpenfireHttpSecret() {
        return app_preferences.getString(OPENFIRE_HTTP_SECRET, null);
    }

    public void putOpenFireSiteType(String OpenFireSiteType) {
        app_preferences.edit().putString(OPENFIRE_SITE_TYPE, OpenFireSiteType).apply();
    }

    public String getOpenFireSiteType() {
        return app_preferences.getString(OPENFIRE_SITE_TYPE, null);
    }
    public void putOpenFireJIDSuffix(String OpenFireJIDSuffix) {
        app_preferences.edit().putString(OPENFIRE_JIDSUFFIX, OpenFireJIDSuffix).apply();
    }

    public String getOpenFireJIDSuffix() {
        return app_preferences.getString(OPENFIRE_JIDSUFFIX, null);
    }

    public void putOpenFireConferenceService(String OpenFireJIDSuffix) {
        app_preferences.edit().putString(OPENFIRE_CONFERENCE_SERVICE, OpenFireJIDSuffix).apply();
    }

    public String getOpenFireConferenceService() {
        return app_preferences.getString(OPENFIRE_CONFERENCE_SERVICE, null);
    }

    public void putCompanyName(String companyName) {
        app_preferences.edit().putString(COMPANY_NAME, companyName).apply();
    }

    public String getCompanyName() {
        return app_preferences.getString(COMPANY_NAME, null);
    }

    public void logoutUser() {
        if (!isRememberMe()) {
            preferenceHelper.clear(PreferenceHelper.USERNAME);
            preferenceHelper.clear(PreferenceHelper.USERTYPE);
            preferenceHelper.clear(PreferenceHelper.PROFILEPICTURE);
            preferenceHelper.clear(PreferenceHelper.FULLNAME);
            preferenceHelper.clear(PreferenceHelper.USER_MOBILE);
            preferenceHelper.clear(PreferenceHelper.USER_PHONE);
            preferenceHelper.clear(PreferenceHelper.USER_EMAIL);
            preferenceHelper.clear(PreferenceHelper.GROUP_LIST);
        }
        preferenceHelper.putIsLogin(false);
    }

    private void clear(String key) {
        app_preferences.edit().remove(key).apply();
    }

}
