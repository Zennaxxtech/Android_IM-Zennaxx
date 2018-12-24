package com.irmsimapp.Model.Login;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;


public class LoginAPI {


    @SerializedName("Status")
    @Expose
    private String status;
    @SerializedName("Msg")
    @Expose
    private String msg;
    @SerializedName("Data")
    @Expose
    private List<Datum> data = null;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<Datum> getData() {
        return data;
    }

    public void setData(List<Datum> data) {
        this.data = data;
    }

    public class Datum {

        @SerializedName("PhotoUrl")
        @Expose
        private String PhotoUrl;

        @SerializedName("UserName")
        @Expose
        private String userName;

        @SerializedName("FullName")
        @Expose
        private String FullName;

        @SerializedName("SiteType")
        @Expose
        private String SiteType;

        public String getFullName() {
            return FullName;
        }

        public void setFullName(String fullName) {
            FullName = fullName;
        }

        @SerializedName("LoginName")
        @Expose
        private String loginName;

        @SerializedName("UserType")
        @Expose
        private String userType;

        @SerializedName("Password")
        @Expose
        private String Password;

        @SerializedName("CompanyName")
        @Expose
        private String CompanyName;

        @SerializedName("CompanyLogoUrl")
        @Expose
        private String CompanyLogoUrl;

        @SerializedName("IMInfo")
        @Expose
        private List<IMInfo> iMInfo = null;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getLoginName() {
            return loginName;
        }

        public void setLoginName(String loginName) {
            this.loginName = loginName;
        }

        public String getUserType() {
            return userType;
        }

        public void setUserType(String userType) {
            this.userType = userType;
        }


        public String getPhotoUrl() {
            return PhotoUrl;
        }

        public void setPhotoUrl(String photoUrl) {
            PhotoUrl = photoUrl;
        }

        public String getSiteType() {
            return SiteType;
        }

        public void setSiteType(String siteType) {
            SiteType = siteType;
        }

        public String getPassword() {
            return Password;
        }

        public void setPassword(String password) {
            Password = password;
        }

        public List<IMInfo> getIMInfo() {
            return iMInfo;
        }

        public void setIMInfo(List<IMInfo> iMInfo) {
            this.iMInfo = iMInfo;
        }

        public String getCompanyName() {
            return CompanyName;
        }

        public void setCompanyName(String companyName) {
            CompanyName = companyName;
        }

        public String getCompanyLogoUrl() {
            return CompanyLogoUrl;
        }

        public void setCompanyLogoUrl(String companyLogoUrl) {
            CompanyLogoUrl = companyLogoUrl;
        }
    }

    public class IMInfo {

        @SerializedName("OpenfireHost")
        @Expose
        private String openfireHost;
        @SerializedName("OpenfireXmppPort")
        @Expose
        private String openfireXmppPort;
        @SerializedName("OpenfireHttpRoot")
        @Expose
        private String openfireHttpRoot;
        @SerializedName("OpenfireHttpSecret")
        @Expose
        private String openfireHttpSecret;
        @SerializedName("OpenFireSiteType")
        @Expose
        private String openFireSiteType;
        @SerializedName("OpenFireJIDSuffix")
        @Expose
        private String openFireJIDSuffix;

        public String getOpenfireHost() {
            return openfireHost;
        }

        public void setOpenfireHost(String openfireHost) {
            this.openfireHost = openfireHost;
        }

        public String getOpenfireXmppPort() {
            return openfireXmppPort;
        }

        public void setOpenfireXmppPort(String openfireXmppPort) {
            this.openfireXmppPort = openfireXmppPort;
        }

        public String getOpenfireHttpRoot() {
            return openfireHttpRoot;
        }

        public void setOpenfireHttpRoot(String openfireHttpRoot) {
            this.openfireHttpRoot = openfireHttpRoot;
        }

        public String getOpenfireHttpSecret() {
            return openfireHttpSecret;
        }

        public void setOpenfireHttpSecret(String openfireHttpSecret) {
            this.openfireHttpSecret = openfireHttpSecret;
        }

        public String getOpenFireSiteType() {
            return openFireSiteType;
        }

        public void setOpenFireSiteType(String openFireSiteType) {
            this.openFireSiteType = openFireSiteType;
        }

        public String getOpenFireJIDSuffix() {
            return openFireJIDSuffix;
        }

        public void setOpenFireJIDSuffix(String openFireJIDSuffix) {
            this.openFireJIDSuffix = openFireJIDSuffix;
        }

    }

}