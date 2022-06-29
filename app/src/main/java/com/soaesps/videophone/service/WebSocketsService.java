package com.soaesps.videophone.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.soaesps.videophone.listener.ListenerI;
import com.soaesps.videophone.utils.BaseBinder;

public class WebSocketsService extends Service implements ListenerI {
    public WebSocketsService() {

    }

    @NonNull
    @Override
    public IBinder onBind(Intent intent) {
        return new BaseBinder<>(this, WebSocketsService.class);
    }

    @Override
    public void onConnect() {

    }

    @Override
    public void onMessage(String message) {

    }

    @Override
    public void onMessage(byte[] data) {

    }

    @Override
    public void onDisconnect(int code, String reason) {

    }

    @Override
    public void onError(Exception error) {

    }
}