package com.dowdah.asknow.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * SharedPreferences管理器
 * 统一管理应用的本地存储
 */
@Singleton
public class SharedPreferencesManager {
    private static final String PREF_NAME = "AskNowPrefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    
    private final SharedPreferences preferences;
    
    @Inject
    public SharedPreferencesManager(@NonNull @ApplicationContext Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * 保存认证令牌
     * 
     * @param token 认证令牌
     */
    public void saveToken(@NonNull String token) {
        preferences.edit().putString(KEY_TOKEN, token).apply();
    }
    
    /**
     * 获取认证令牌
     * 
     * @return 认证令牌，可能为null
     */
    @Nullable
    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }
    
    /**
     * 保存用户ID
     * 
     * @param userId 用户ID
     */
    public void saveUserId(long userId) {
        preferences.edit().putLong(KEY_USER_ID, userId).apply();
    }
    
    /**
     * 获取用户ID
     * 
     * @return 用户ID，未设置时返回-1
     */
    public long getUserId() {
        return preferences.getLong(KEY_USER_ID, -1);
    }
    
    /**
     * 保存用户名
     * 
     * @param username 用户名
     */
    public void saveUsername(@NonNull String username) {
        preferences.edit().putString(KEY_USERNAME, username).apply();
    }
    
    /**
     * 获取用户名
     * 
     * @return 用户名，可能为null
     */
    @Nullable
    public String getUsername() {
        return preferences.getString(KEY_USERNAME, null);
    }
    
    /**
     * 保存用户角色
     * 
     * @param role 用户角色
     */
    public void saveRole(@NonNull String role) {
        preferences.edit().putString(KEY_ROLE, role).apply();
    }
    
    /**
     * 获取用户角色
     * 
     * @return 用户角色，可能为null
     */
    @Nullable
    public String getRole() {
        return preferences.getString(KEY_ROLE, null);
    }
    
    /**
     * 检查用户是否已登录
     * 
     * @return true表示已登录
     */
    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }
    
    /**
     * 清除所有保存的数据
     */
    public void clear() {
        preferences.edit().clear().apply();
    }
}

