package com.dowdah.asknow.ui.auth;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dowdah.asknow.R;
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

@HiltViewModel
public class AuthViewModel extends AndroidViewModel {
    private static final String TAG = "AuthViewModel";
    
    private final ApiService apiService;
    private final SharedPreferencesManager prefsManager;
    private final WebSocketManager webSocketManager;
    
    private final MutableLiveData<AuthResult> loginResult = new MutableLiveData<>();
    private final MutableLiveData<AuthResult> registerResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    
    @Inject
    public AuthViewModel(@NonNull Application application, ApiService apiService, SharedPreferencesManager prefsManager, WebSocketManager webSocketManager) {
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
    
    public LiveData<Boolean> getLoading() {
        return loading;
    }
    
    public void login(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            loginResult.setValue(new AuthResult(false, getApplication().getString(R.string.username_password_required), null));
            return;
        }
        
        loading.setValue(true);
        LoginRequest request = new LoginRequest(username, password);
        
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse.isSuccess()) {
                        // Save user data
                        prefsManager.saveToken(loginResponse.getToken());
                        prefsManager.saveUserId(loginResponse.getUser().getId());
                        prefsManager.saveUsername(loginResponse.getUser().getUsername());
                        prefsManager.saveRole(loginResponse.getUser().getRole());
                        
                        // Connect WebSocket
                        webSocketManager.connect();
                        
                        loginResult.setValue(new AuthResult(true, loginResponse.getMessage(), loginResponse.getUser().getRole()));
                        Log.d(TAG, "Login successful: " + loginResponse.getUser().getUsername());
                    } else {
                        loginResult.setValue(new AuthResult(false, loginResponse.getMessage(), null));
                    }
                } else {
                    loginResult.setValue(new AuthResult(false, getApplication().getString(R.string.login_failed, response.message()), null));
                }
            }
            
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                loading.setValue(false);
                loginResult.setValue(new AuthResult(false, getApplication().getString(R.string.network_error, t.getMessage()), null));
                Log.e(TAG, "Login error", t);
            }
        });
    }
    
    public void register(String username, String password, String role) {
        if (username.isEmpty() || password.isEmpty()) {
            registerResult.setValue(new AuthResult(false, getApplication().getString(R.string.username_password_required), null));
            return;
        }
        
        if (role.isEmpty()) {
            role = "student"; // Default role
        }
        
        loading.setValue(true);
        RegisterRequest request = new RegisterRequest(username, password, role);
        
        apiService.register(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse registerResponse = response.body();
                    if (registerResponse.isSuccess()) {
                        // Save user data
                        prefsManager.saveToken(registerResponse.getToken());
                        prefsManager.saveUserId(registerResponse.getUser().getId());
                        prefsManager.saveUsername(registerResponse.getUser().getUsername());
                        prefsManager.saveRole(registerResponse.getUser().getRole());
                        
                        // Connect WebSocket
                        webSocketManager.connect();
                        
                        registerResult.setValue(new AuthResult(true, registerResponse.getMessage(), registerResponse.getUser().getRole()));
                        Log.d(TAG, "Registration successful: " + registerResponse.getUser().getUsername());
                    } else {
                        registerResult.setValue(new AuthResult(false, registerResponse.getMessage(), null));
                    }
                } else {
                    registerResult.setValue(new AuthResult(false, getApplication().getString(R.string.registration_failed, response.message()), null));
                }
            }
            
            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                loading.setValue(false);
                registerResult.setValue(new AuthResult(false, getApplication().getString(R.string.network_error, t.getMessage()), null));
                Log.e(TAG, "Registration error", t);
            }
        });
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

