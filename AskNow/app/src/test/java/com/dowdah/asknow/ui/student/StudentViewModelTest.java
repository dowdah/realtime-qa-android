package com.dowdah.asknow.ui.student;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.dowdah.asknow.constants.QuestionStatus;
import com.dowdah.asknow.data.model.WebSocketMessage;
import com.dowdah.asknow.data.api.ApiService;
import com.dowdah.asknow.data.local.dao.QuestionDao;
import com.dowdah.asknow.data.local.entity.QuestionEntity;
import com.dowdah.asknow.data.model.QuestionRequest;
import com.dowdah.asknow.data.model.QuestionResponse;
import com.dowdah.asknow.data.repository.QuestionRepository;
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

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * StudentViewModel 单元测试
 * 
 * 测试功能：
 * - 创建新问题
 * - 查询自己的问题列表
 * - 分页加载逻辑
 */
@RunWith(MockitoJUnitRunner.class)
public class StudentViewModelTest {
    
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    
    @Mock
    private Application application;
    
    @Mock
    private ApiService apiService;
    
    @Mock
    private QuestionDao questionDao;
    
    @Mock
    private SharedPreferencesManager prefsManager;
    
    @Mock
    private QuestionRepository questionRepository;
    
    @Mock
    private WebSocketManager webSocketManager;
    
    @Mock
    private Call<QuestionResponse> questionCall;
    
    @Mock
    private Observer<String> errorObserver;
    
    private StudentViewModel viewModel;
    
    private static final long TEST_USER_ID = 1L;
    private static final String TEST_TOKEN = "test_token";
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock Application getString
        when(application.getString(any(int.class)))
            .thenReturn("Test String");
        when(application.getString(any(int.class), any()))
            .thenReturn("Test String with args");
        
        when(prefsManager.getUserId()).thenReturn(TEST_USER_ID);
        when(prefsManager.getToken()).thenReturn(TEST_TOKEN);
        
        // Mock WebSocketManager.getIncomingMessage() 以避免 NullPointerException
        MutableLiveData<WebSocketMessage> mockIncomingMessage = new MutableLiveData<>();
        when(webSocketManager.getIncomingMessage()).thenReturn(mockIncomingMessage);
        
        viewModel = new StudentViewModel(
            application,
            apiService,
            questionDao,
            prefsManager,
            questionRepository,
            webSocketManager
        );
    }
    
    /**
     * 测试创建问题成功
     */
    @Test
    public void testCreateQuestion_Success() {
        // Arrange
        String content = "How to solve this problem?";
        List<String> imagePaths = Arrays.asList("/uploads/1/test1.jpg", "/uploads/1/test2.jpg");
        
        QuestionResponse.QuestionData questionData = new QuestionResponse.QuestionData();
        questionData.setId(100L);
        questionData.setUserId(TEST_USER_ID);
        questionData.setContent(content);
        questionData.setImagePaths(imagePaths);
        questionData.setStatus(QuestionStatus.PENDING);
        questionData.setCreatedAt(System.currentTimeMillis());
        
        QuestionResponse questionResponse = new QuestionResponse();
        questionResponse.setSuccess(true);
        questionResponse.setQuestion(questionData);
        
        Response<QuestionResponse> response = Response.success(questionResponse);
        
        when(apiService.createQuestion(anyString(), any(QuestionRequest.class)))
            .thenReturn(questionCall);
        
        ArgumentCaptor<Callback<QuestionResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        // Act
        viewModel.createQuestion(content, imagePaths);
        
        verify(questionCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(questionCall, response);
        
        // Assert - 应该保存到本地数据库
        verify(questionDao).insert(any(QuestionEntity.class));
    }
    
    /**
     * 测试创建问题成功 - 无图片
     */
    @Test
    public void testCreateQuestion_NoImages() {
        // Arrange
        String content = "A question without images";
        
        QuestionResponse.QuestionData questionData = new QuestionResponse.QuestionData();
        questionData.setId(101L);
        questionData.setUserId(TEST_USER_ID);
        questionData.setContent(content);
        questionData.setImagePaths(null);
        questionData.setStatus(QuestionStatus.PENDING);
        questionData.setCreatedAt(System.currentTimeMillis());
        
        QuestionResponse questionResponse = new QuestionResponse();
        questionResponse.setSuccess(true);
        questionResponse.setQuestion(questionData);
        
        Response<QuestionResponse> response = Response.success(questionResponse);
        
        when(apiService.createQuestion(anyString(), any(QuestionRequest.class)))
            .thenReturn(questionCall);
        
        ArgumentCaptor<Callback<QuestionResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        // Act
        viewModel.createQuestion(content, null);
        
        verify(questionCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(questionCall, response);
        
        // Assert
        verify(questionDao).insert(any(QuestionEntity.class));
    }
    
    /**
     * 测试创建问题失败
     */
    @Test
    public void testCreateQuestion_Failure() {
        // Arrange
        String content = "Test question";
        Throwable error = new java.io.IOException("Network error");
        
        when(apiService.createQuestion(anyString(), any(QuestionRequest.class)))
            .thenReturn(questionCall);
        
        ArgumentCaptor<Callback<QuestionResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        // Act
        viewModel.getErrorMessage().observeForever(errorObserver);
        viewModel.createQuestion(content, null);
        
        verify(questionCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(questionCall, error);
        
        // Assert - 不应该保存到本地数据库
        verify(questionDao, never()).insert(any(QuestionEntity.class));
        verify(errorObserver, atLeastOnce()).onChanged(anyString());
    }
    
    /**
     * 测试创建问题失败 - 服务器返回失败
     */
    @Test
    public void testCreateQuestion_ServerError() {
        // Arrange
        String content = "Test question";
        
        QuestionResponse questionResponse = new QuestionResponse();
        questionResponse.setSuccess(false);
        questionResponse.setMessage("创建失败");
        
        Response<QuestionResponse> response = Response.success(questionResponse);
        
        when(apiService.createQuestion(anyString(), any(QuestionRequest.class)))
            .thenReturn(questionCall);
        
        ArgumentCaptor<Callback<QuestionResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        // Act
        viewModel.getErrorMessage().observeForever(errorObserver);
        viewModel.createQuestion(content, null);
        
        verify(questionCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(questionCall, response);
        
        // Assert
        verify(questionDao, never()).insert(any(QuestionEntity.class));
        verify(errorObserver, atLeastOnce()).onChanged(anyString());
    }
    
    /**
     * 测试获取问题列表
     */
    @Test
    public void testGetQuestions() {
        // Act
        viewModel.getQuestions();
        
        // Assert - 应该查询当前用户的问题
        verify(questionDao).getQuestionsByUserId(TEST_USER_ID);
    }
}

