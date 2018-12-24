package com.irmsimapp.ApiClient;

import com.google.gson.JsonObject;
import com.irmsimapp.Model.AppVersion.AppVersion;
import com.irmsimapp.Model.BadKeyWord.BadKeyWord;
import com.irmsimapp.Model.CheckUserOnOpenFire;
import com.irmsimapp.Model.ForgotPassword.ForgotPassword;
import com.irmsimapp.Model.GroupUsers.GroupUser;
import com.irmsimapp.Model.Login.LoginAPI;
import com.irmsimapp.Model.SaveMessage.SaveMessage;
import com.irmsimapp.Model.UserProfile.UserProfile;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;


public interface Webservices {

    @FormUrlEncoded
    @POST("Common/Login")
    Call<LoginAPI> getLogin(@FieldMap Map<String, String> map);

    @GET("Common/Login.aspx")
    Call<LoginAPI> getLoginAspx(@QueryMap Map<String, String> map);

    @FormUrlEncoded
    @POST("sso/Login")
    Call<LoginAPI> getSSOLogin(@FieldMap Map<String, String> map);

    @FormUrlEncoded
    @POST("Common/LoginedInfo")
    Call<LoginAPI> getLoginInfo(@FieldMap Map<String, String> map);

    @GET("Common/Version")
    Call<AppVersion> checkVersionServer(@QueryMap Map<String, String> map);

    @GET("Common/Version.aspx")
    Call<AppVersion> checkVersionServerAspx(@QueryMap Map<String, String> map);

    @POST("Common/PushToken")
    Call<ResponseBody> sendDeviceToken(@QueryMap Map<String, String> map);

    @POST("Common/PushToken.aspx")
    Call<ResponseBody> sendDeviceTokenAspx(@QueryMap Map<String, String> map);

    @GET("Common/UserProfile")
    Call<UserProfile> UserProfile(@QueryMap Map<String, String> map);

    @GET("Common/UserProfile.aspx")
    Call<UserProfile> UserProfileAspx(@QueryMap Map<String, String> map);

    @GET("InstantMessaging/GroupUserMap")
    Call<GroupUser> getGroupUsers(@QueryMap Map<String, String> map);

    @GET("InstantMessaging/GroupUserMap.aspx")
    Call<GroupUser> getGroupUsersAspx(@QueryMap Map<String, String> map);

    @Headers("Content-Type: application/json")
    @POST("/plugins/restapi/v1/users")
    Call<ResponseBody> createUserOnOpenfire(@Header("Authorization") String auth, @Body JsonObject jsonBody);

    @Headers("Accept: application/json")
    @GET("/plugins/restapi/v1/users/{id}")
    Call<CheckUserOnOpenFire> checkUserOnOpenFire(@Header("Authorization") String auth, @Path("id") String Username);

    @Headers("Accept: application/json")
    @POST("/plugins/restapi/v1/chatrooms")
    Call<ResponseBody> createOrJoinChatRoom(@Header("Authorization") String auth, @Body String body);

    @Headers("Accept: application/json")
    @POST("/plugins/restapi/v1/chatrooms/{roomname}/members/{username}")
    Call<ResponseBody> addUserToOpenFireRoom(@Header("Authorization") String auth, @Path("roomname") String room_name, @Path("username") String user_name);

    @GET("InstantMessaging/BadFilter")
    Call<BadKeyWord> checkBadKeyWords(@QueryMap Map<String, String> map);

    @GET("InstantMessaging/BadFilter.aspx")
    Call<BadKeyWord> checkBadKeyWordsAspx(@QueryMap Map<String, String> map);

    @Headers("Accept: application/json")
    @GET("/plugins/restapi/v1/chatrooms/{roomname}")
    Call<ResponseBody> checkGroupOnOpenFire(@Header("Authorization") String auth, @Header("servicename") String servicename, @Path("roomname") String room_name);

    @Headers("Accept: application/json")
    @DELETE("chatrooms/{roomname}")
    Call<ResponseBody> deleteGroupOnOpenFire(@Header("Authorization") String auth, @Path("roomname") String room_name);

    @GET("InstantMessaging/SaveMessage")
    Call<SaveMessage> saveTextMessageOnServer(@QueryMap Map<String, String> map);

    @POST("InstantMessaging/SaveMessage.aspx")
    @FormUrlEncoded
    Call<SaveMessage> saveTextMessageOnServerAspx(@FieldMap Map<String, String> map);

    @Multipart
    @POST("InstantMessaging/SaveMessage")
    Call<SaveMessage> saveFileMessageOnServer(@QueryMap Map<String, String> map, @Part MultipartBody.Part file);

    @Multipart
    @POST("InstantMessaging/SaveMediaMessage.aspx")
    Call<SaveMessage> saveFileMessageOnServerAspx(@QueryMap Map<String, String> map, @Part MultipartBody.Part file);

    @GET("/plugins/restapi/v1/userservice")
    Call<ResponseBody> updateUseronOpenfireServer(@QueryMap Map<String, String> map);

    @GET("Client/Common/ForgotPassword")
    Call<ForgotPassword> getForgotPassword(@QueryMap Map<String, String> map);

    @GET("Client/Common/ForgotPassword.aspx")
    Call<ForgotPassword> getForgotPasswordAspx(@QueryMap Map<String, String> map);

    @POST("/plugins/chatlogs")
    Call<ResponseBody> callChatLogsApi(@QueryMap  Map<String, String> map);

    @FormUrlEncoded
    @POST("/plugins/chatlogs")
    @Headers("Content-Type:application/x-www-form-urlencoded; charset=utf-8")
    Call<ResponseBody> callChatLogsApi(@Field("apiType") String apiType,@Field("UserName") String UserName,@Field("Jid") String Jid,@Field("deviceToken") String deviceToken,@Field("deviceType") String deviceType,@Field("type") String type);

    @FormUrlEncoded
    @POST("/plugins/chatlogs")
    @Headers("Content-Type:application/x-www-form-urlencoded; charset=utf-8")
    Call<ResponseBody> callChatLogsApi(@Field("apiType") String apiType,@Field("Jid") String Jid,@Field("type") String type);

    @POST("/plugins/chatlogs")
    Call<ResponseBody> sendUnReadMessageCountToService(@QueryMap(encoded = true) Map<String, String> map);


}
