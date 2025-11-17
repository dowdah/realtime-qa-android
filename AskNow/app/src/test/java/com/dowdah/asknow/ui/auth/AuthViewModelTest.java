package com.dowdah.asknow.ui.auth;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.dowdah.asknow.data.api.ApiService;
import com.dowdah.asknow.data.model.LoginRequest;
import com.dowdah.asknow.data.model.LoginResponse;
import com.dowdah.asknow.data.model.RegisterRequest;
import com.dowdah.asknow.data.model.RegisterResponse;
import com.dowdah.asknow.utils.SharedPreferencesManager;
import com.dowdah.asknow.utils.WebSocketManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthViewModel 单元测试
 * 
 * 测试功能：
 * - 登录成功/失败场景
 * - 注册成功/失败场景
 * - 输入验证
 * - WebSocket连接管理
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthViewModelTest {
    
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    
    @Mock
    private Application application;
    
    @Mock
    private ApiService apiService;
    
    @Mock
    private SharedPreferencesManager prefsManager;
    
    @Mock
    private WebSocketManager webSocketManager;
    
    @Mock
    private Call<LoginResponse> loginCall;
    
    @Mock
    private Call<RegisterResponse> registerCall;
    
    @Mock
    private Observer<AuthViewModel.AuthResult> authResultObserver;
    
    private AuthViewModel viewModel;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock Application getString
        when(application.getString(any(int.class)))
            .thenReturn("Test String");
        when(application.getString(any(int.class), any()))
            .thenReturn("Test String with args");
        
        viewModel = new AuthViewModel(application, apiService, prefsManager, webSocketManager);
    }
    
    /**
     * 测试登录成功场景
     */
    @Test
    public void testLogin_Success() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        String token = "test_token";
        long userId = 1L;
        String role = "student";
        
        LoginResponse.UserData user = new LoginResponse.UserData();
        user.setId(userId);
        user.setUsername(username);
        user.setRole(role);
        
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setSuccess(true);
        loginResponse.setMessage("登录成功");
        loginResponse.setToken(token);
        loginResponse.setUser(user);
        
        Response<LoginResponse> response = Response.success(loginResponse);
        
        when(apiService.login(any(LoginRequest.class))).thenReturn(loginCall);
        
        // Capture callback
        ArgumentCaptor<Callback<LoginResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        // Act
        viewModel.getLoginResult().observeForever(authResultObserver);
        viewModel.login(username, password);
        
        verify(loginCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(loginCall, response);
        
        // Assert
        verify(prefsManager).saveToken(token);
        verify(prefsManager).saveUserId(userId);
        verify(prefsManager).saveUsername(username);
        verify(prefsManager).saveRole(role);
        verify(webSocketManager).connect();
        
        ArgumentCaptor<AuthViewModel.AuthResult> resultCaptor = 
            ArgumentCaptor.forClass(AuthViewModel.AuthResult.class);
        verify(authResultObserver, atLeastOnce()).onChanged(resultCaptor.capture());
        
        AuthViewModel.AuthResult result = resultCaptor.getValue();
        assertTrue(result.isSuccess());
        assertEquals(role, result.getRole());
    }
    
    /**
     * 测试登录失败 - 空用户名或密码
     */
    @Test
    public void testLogin_EmptyCredentials() {
        // Act
        viewModel.getLoginResult().observeForever(authResultObserver);
        viewModel.login("", "");
        
        // Assert
        ArgumentCaptor<AuthViewModel.AuthResult> resultCaptor = 
            ArgumentCaptor.forClass(AuthViewModel.AuthResult.class);
        verify(authResultObserver).onChanged(resultCaptor.capture());
        
        AuthViewModel.AuthResult result = resultCaptor.getValue();
        assertFalse(result.isSuccess());
        assertNull(result.getRole());
        
        // 确保没有调用API
        verify(apiService, never()).login(any(LoginRequest.class));
    }
    
    /**
     * 测试登录失败 - 网络错误
     */
    @Test
    public void testLogin_NetworkError() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        Throwable error = new java.io.IOException("Network error");
        
        when(apiService.login(any(LoginRequest.class))).thenReturn(loginCall);
        
        ArgumentCaptor<Callback<LoginResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        // Act
        viewModel.getLoginResult().observeForever(authResultObserver);
        viewModel.login(username, password);
        
        verify(loginCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(loginCall, error);
        
        // Assert
        ArgumentCaptor<AuthViewModel.AuthResult> resultCaptor = 
            ArgumentCaptor.forClass(AuthViewModel.AuthResult.class);
        verify(authResultObserver, atLeastOnce()).onChanged(resultCaptor.capture());
        
        AuthViewModel.AuthResult result = resultCaptor.getValue();
        assertFalse(result.isSuccess());
        assertNull(result.getRole());
        
        // 确保没有保存数据
        verify(prefsManager, never()).saveToken(anyString());
        verify(webSocketManager, never()).connect();
    }
    
    /**
     * 测试注册成功场景
     */
    @Test
    public void testRegister_Success() {
        // Arrange
        String username = "newuser";
        String password = "password123";
        String role = "student";
        String token = "test_token";
        long userId = 2L;
        
        RegisterResponse.UserData user = new RegisterResponse.UserData();
        user.setId(userId);
        user.setUsername(username);
        user.setRole(role);
        
        RegisterResponse registerResponse = new RegisterResponse();
        registerResponse.setSuccess(true);
        registerResponse.setMessage("注册成功");
        registerResponse.setToken(token);
        registerResponse.setUser(user);
        
        Response<RegisterResponse> response = Response.success(registerResponse);
        
        when(apiService.register(any(RegisterRequest.class))).thenReturn(registerCall);
        
        ArgumentCaptor<Callback<RegisterResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        // Act
        viewModel.getRegisterResult().observeForever(authResultObserver);
        viewModel.register(username, password, role);
        
        verify(registerCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(registerCall, response);
        
        // Assert
        verify(prefsManager).saveToken(token);
        verify(prefsManager).saveUserId(userId);
        verify(prefsManager).saveUsername(username);
        verify(prefsManager).saveRole(role);
        verify(webSocketManager).connect();
        
        ArgumentCaptor<AuthViewModel.AuthResult> resultCaptor = 
            ArgumentCaptor.forClass(AuthViewModel.AuthResult.class);
        verify(authResultObserver, atLeastOnce()).onChanged(resultCaptor.capture());
        
        AuthViewModel.AuthResult result = resultCaptor.getValue();
        assertTrue(result.isSuccess());
        assertEquals(role, result.getRole());
    }
    
    /**
     * 测试注册失败 - 空用户名或密码
     */
    @Test
    public void testRegister_EmptyCredentials() {
        // Act
        viewModel.getRegisterResult().observeForever(authResultObserver);
        viewModel.register("", "", "student");
        
        // Assert
        ArgumentCaptor<AuthViewModel.AuthResult> resultCaptor = 
            ArgumentCaptor.forClass(AuthViewModel.AuthResult.class);
        verify(authResultObserver).onChanged(resultCaptor.capture());
        
        AuthViewModel.AuthResult result = resultCaptor.getValue();
        assertFalse(result.isSuccess());
        assertNull(result.getRole());
        
        // 确保没有调用API
        verify(apiService, never()).register(any(RegisterRequest.class));
    }
    
    /**
     * 测试注册 - 默认角色
     */
    @Test
    public void testRegister_DefaultRole() {
        // Arrange
        String username = "newuser";
        String password = "password123";
        
        when(apiService.register(any(RegisterRequest.class))).thenReturn(registerCall);
        
        // Act
        viewModel.register(username, password, "");
        
        // Assert
        ArgumentCaptor<RegisterRequest> requestCaptor = 
            ArgumentCaptor.forClass(RegisterRequest.class);
        verify(apiService).register(requestCaptor.capture());
        
        RegisterRequest request = requestCaptor.getValue();
        assertEquals("student", request.getRole()); // 应该使用默认角色
    }
}

