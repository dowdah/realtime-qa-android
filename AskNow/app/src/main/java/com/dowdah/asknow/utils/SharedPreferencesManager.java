package com.dowdah.asknow.utils;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class SharedPreferencesManager {
    private static final String PREF_NAME = "AskNowPrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    
    private final SharedPreferences preferences;
    
    @Inject
    public SharedPreferencesManager(@ApplicationContext Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public void saveToken(String token) {
        preferences.edit().putString(KEY_TOKEN, token).apply();
    }
    
    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }
    
    public void saveUserId(long userId) {
        preferences.edit().putLong(KEY_USER_ID, userId).apply();
    }
    
    public long getUserId() {
        return preferences.getLong(KEY_USER_ID, -1);
    }
    
    public void saveUsername(String username) {
        preferences.edit().putString(KEY_USERNAME, username).apply();
    }
    
    public String getUsername() {
        return preferences.getString(KEY_USERNAME, null);
    }
    
    public void saveRole(String role) {
        preferences.edit().putString(KEY_ROLE, role).apply();
    }
    
    public String getRole() {
        return preferences.getString(KEY_ROLE, null);
    }
    
    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }
    
    public void clear() {
        preferences.edit().clear().apply();
    }
}

