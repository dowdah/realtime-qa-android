package com.dowdah.asknow.ui.auth;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dowdah.asknow.R;
import com.dowdah.asknow.base.BaseViewModel;
import com.dowdah.asknow.data.api.ApiService;
import com.dowdah.asknow.data.model.LoginRequest;
import com.dowdah.asknow.data.model.LoginResponse;
import com.dowdah.asknow.data.model.RegisterRequest;
import com.dowdah.asknow.data.model.RegisterResponse;
import com.dowdah.asknow.utils.SharedPreferencesManager;
import com.dowdah.asknow.utils.WebSocketManager;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AuthViewModel - 认证相关的ViewModel
 * 
 * 主要功能：
 * - 用户登录
 * - 用户注册
 * - WebSocket连接管理
 * 
 * 优化改进：
 * - 继承 BaseViewModel，复用线程池和错误处理
 * - 提取统一的认证逻辑（performAuthentication）
 */
@HiltViewModel
public class AuthViewModel extends BaseViewModel {
    private static final String TAG = "AuthViewModel";
    
    private final ApiService apiService;
    private final SharedPreferencesManager prefsManager;
    private final WebSocketManager webSocketManager;
    
    private final MutableLiveData<AuthResult> loginResult = new MutableLiveData<>();
    private final MutableLiveData<AuthResult> registerResult = new MutableLiveData<>();
    
    @Inject
    public AuthViewModel(
        @NonNull Application application, 
        ApiService apiService, 
        SharedPreferencesManager prefsManager, 
        WebSocketManager webSocketManager
    ) {
        super(application);
        this.apiService = apiService;
        this.prefsManager = prefsManager;
        this.webSocketManager = webSocketManager;
    }
    
    public LiveData<AuthResult> getLoginResult() {
        return loginResult;
    }
    
    public LiveData<AuthResult> getRegisterResult() {
        return registerResult;
    }
    
    /**
     * 用户登录
     * 
     * @param username 用户名
     * @param password 密码
     */
    public void login(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            loginResult.setValue(new AuthResult(false, getApplication().getString(R.string.username_password_required), null));
            return;
        }
        
        setLoading(true);
        LoginRequest request = new LoginRequest(username, password);
        
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                handleLoginResponse(response, response.body(), loginResult);
            }
            
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                handleAuthFailure(t, loginResult, "Login");
            }
        });
    }
    
    /**
     * 用户注册
     * 
     * @param username 用户名
     * @param password 密码
     * @param role 用户角色
     */
    public void register(String username, String password, String role) {
        if (username.isEmpty() || password.isEmpty()) {
            registerResult.setValue(new AuthResult(false, getApplication().getString(R.string.username_password_required), null));
            return;
        }
        
        if (role.isEmpty()) {
            role = "student"; // Default role
        }
        
        setLoading(true);
        RegisterRequest request = new RegisterRequest(username, password, role);
        
        apiService.register(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                handleRegisterResponse(response, response.body(), registerResult);
            }
            
            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                handleAuthFailure(t, registerResult, "Registration");
            }
        });
    }
    
    /**
     * 处理登录响应
     */
    private void handleLoginResponse(
        Response<LoginResponse> response,
        LoginResponse loginResponse,
        MutableLiveData<AuthResult> resultLiveData
    ) {
        setLoading(false);
        
        if (response.isSuccessful() && loginResponse != null) {
            if (loginResponse.isSuccess()) {
                saveUserDataAndConnect(
                    loginResponse.getToken(),
                    loginResponse.getUser().getId(),
                    loginResponse.getUser().getUsername(),
                    loginResponse.getUser().getRole()
                );
                
                resultLiveData.setValue(new AuthResult(true, loginResponse.getMessage(), loginResponse.getUser().getRole()));
                Log.d(TAG, "Login successful: " + loginResponse.getUser().getUsername());
            } else {
                resultLiveData.setValue(new AuthResult(false, loginResponse.getMessage(), null));
            }
        } else {
            resultLiveData.setValue(new AuthResult(false, getApplication().getString(R.string.login_failed, response.message()), null));
        }
    }
    
    /**
     * 处理注册响应
     */
    private void handleRegisterResponse(
        Response<RegisterResponse> response,
        RegisterResponse registerResponse,
        MutableLiveData<AuthResult> resultLiveData
    ) {
        setLoading(false);
        
        if (response.isSuccessful() && registerResponse != null) {
            if (registerResponse.isSuccess()) {
                saveUserDataAndConnect(
                    registerResponse.getToken(),
                    registerResponse.getUser().getId(),
                    registerResponse.getUser().getUsername(),
                    registerResponse.getUser().getRole()
                );
                
                resultLiveData.setValue(new AuthResult(true, registerResponse.getMessage(), registerResponse.getUser().getRole()));
                Log.d(TAG, "Registration successful: " + registerResponse.getUser().getUsername());
            } else {
                resultLiveData.setValue(new AuthResult(false, registerResponse.getMessage(), null));
            }
        } else {
            resultLiveData.setValue(new AuthResult(false, getApplication().getString(R.string.registration_failed, response.message()), null));
        }
    }
    
    /**
     * 保存用户数据并连接WebSocket（登录和注册共用）
     */
    private void saveUserDataAndConnect(String token, long userId, String username, String role) {
        prefsManager.saveToken(token);
        prefsManager.saveUserId(userId);
        prefsManager.saveUsername(username);
        prefsManager.saveRole(role);
        
        // Connect WebSocket
        webSocketManager.connect();
    }
    
    /**
     * 统一处理认证失败（登录和注册）
     */
    private void handleAuthFailure(
        Throwable t,
        MutableLiveData<AuthResult> resultLiveData,
        String operationType
    ) {
        setLoading(false);
        resultLiveData.setValue(new AuthResult(false, getApplication().getString(R.string.network_error, t.getMessage()), null));
        Log.e(TAG, operationType + " error", t);
    }
    
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final String role;
        
        public AuthResult(boolean success, String message, String role) {
            this.success = success;
            this.message = message;
            this.role = role;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getRole() {
            return role;
        }
    }
}

