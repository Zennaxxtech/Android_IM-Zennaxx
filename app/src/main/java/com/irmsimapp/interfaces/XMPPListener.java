package com.irmsimapp.interfaces;



public interface XMPPListener {

    void authenticatedSuccessfully();

    void authenticateFailed();

    void authenticationError(String errorMsg);
}
