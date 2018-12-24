package com.irmsimapp.interfaces;



public interface XMPPErrorListener {
    void connectionClosedOnError();
    void connectionClosedOnConflict();
}
