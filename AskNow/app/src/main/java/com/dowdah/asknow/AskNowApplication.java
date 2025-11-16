package com.dowdah.asknow;

import android.app.Application;

import com.dowdah.asknow.utils.SharedPreferencesManager;
import com.dowdah.asknow.utils.WebSocketManager;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class AskNowApplication extends Application {
    
    @Inject
    WebSocketManager webSocketManager;
    
    @Inject
    SharedPreferencesManager prefsManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Connect WebSocket if user is logged in
        if (prefsManager.isLoggedIn()) {
            webSocketManager.connect();
        }
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        webSocketManager.cleanup();
    }
}

