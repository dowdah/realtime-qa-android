package com.dowdah.asknow.ui.chat;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.dowdah.asknow.constants.MessageStatus;
import com.dowdah.asknow.constants.QuestionStatus;
import com.dowdah.asknow.data.api.ApiService;
import com.dowdah.asknow.data.local.dao.MessageDao;
import com.dowdah.asknow.data.local.dao.QuestionDao;
import com.dowdah.asknow.data.local.entity.MessageEntity;
import com.dowdah.asknow.data.local.entity.QuestionEntity;
import com.dowdah.asknow.data.model.MessageRequest;
import com.dowdah.asknow.data.model.MessageResponse;
import com.dowdah.asknow.data.repository.MessageRepository;
import com.dowdah.asknow.utils.SharedPreferencesManager;
import com.google.gson.JsonObject;

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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ChatViewModel 单元测试
 * 
 * 测试功能：
 * - 发送消息的成功/失败场景
 * - 乐观更新模式
 * - 接受问题功能
 * - 关闭问题功能
 * - 失败回滚机制
 */
@RunWith(MockitoJUnitRunner.class)
public class ChatViewModelTest {
    
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    
    @Mock
    private Application application;
    
    @Mock
    private ApiService apiService;
    
    @Mock
    private QuestionDao questionDao;
    
    @Mock
    private MessageDao messageDao;
    
    @Mock
    private MessageRepository messageRepository;
    
    @Mock
    private SharedPreferencesManager prefsManager;
    
    @Mock
    private Call<MessageResponse> messageCall;
    
    @Mock
    private Call<JsonObject> jsonCall;
    
    @Mock
    private Observer<Boolean> messageSentObserver;
    
    @Mock
    private Observer<String> errorObserver;
    
    private ChatViewModel viewModel;
    
    private static final long TEST_QUESTION_ID = 1L;
    private static final long TEST_USER_ID = 2L;
    private static final String TEST_TOKEN = "test_token";
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock Application getString
        when(application.getString(any(int.class)))
            .thenReturn("Test String");
        
        when(prefsManager.getUserId()).thenReturn(TEST_USER_ID);
        when(prefsManager.getToken()).thenReturn(TEST_TOKEN);
        
        viewModel = new ChatViewModel(
            application,
            apiService,
            questionDao,
            messageDao,
            messageRepository,
            prefsManager
        );
    }
    
    /**
     * 测试发送消息成功场景
     */
    @Test
    public void testSendMessage_Success() {
        // Arrange
        String content = "Test message";
        long messageId = 100L;
        
        MessageResponse.MessageData messageData = new MessageResponse.MessageData();
        messageData.setId(messageId);
        messageData.setQuestionId(TEST_QUESTION_ID);
        messageData.setSenderId(TEST_USER_ID);
        messageData.setContent(content);
        messageData.setMessageType("text");
        messageData.setCreatedAt(System.currentTimeMillis());
        
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setSuccess(true);
        messageResponse.setData(messageData);
        
        Response<MessageResponse> response = Response.success(messageResponse);
        
        when(apiService.sendMessage(anyString(), any(MessageRequest.class)))
            .thenReturn(messageCall);
        
        ArgumentCaptor<Callback<MessageResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        // Act
        viewModel.getMessageSent().observeForever(messageSentObserver);
        viewModel.sendMessage(TEST_QUESTION_ID, content);
        
        verify(messageCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(messageCall, response);
        
        // Assert - 应该先插入临时消息，然后替换为真实消息
        verify(messageDao, atLeast(2)).insert(any(MessageEntity.class));
        verify(messageDao).deleteById(anyLong()); // 删除临时消息
        verify(questionDao).updateUpdatedAt(eq(TEST_QUESTION_ID), anyLong());
        
        ArgumentCaptor<Boolean> sentCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(messageSentObserver, atLeastOnce()).onChanged(sentCaptor.capture());
        assertTrue(sentCaptor.getValue());
    }
    
    /**
     * 测试发送消息失败场景
     */
    @Test
    public void testSendMessage_Failure() {
        // Arrange
        String content = "Test message";
        Throwable error = new java.io.IOException("Network error");
        
        when(apiService.sendMessage(anyString(), any(MessageRequest.class)))
            .thenReturn(messageCall);
        
        ArgumentCaptor<Callback<MessageResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        // Act
        viewModel.getErrorMessage().observeForever(errorObserver);
        viewModel.sendMessage(TEST_QUESTION_ID, content);
        
        verify(messageCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(messageCall, error);
        
        // Assert - 应该插入临时消息，然后标记为失败
        verify(messageDao).insert(any(MessageEntity.class));
        verify(messageDao).updateSendStatus(anyLong(), eq(MessageStatus.FAILED));
        verify(errorObserver, atLeastOnce()).onChanged(anyString());
    }
    
    /**
     * 测试发送图片消息
     */
    @Test
    public void testSendImageMessage_Success() {
        // Arrange
        String imagePath = "/uploads/1/test.jpg";
        long messageId = 101L;
        
        MessageResponse.MessageData messageData = new MessageResponse.MessageData();
        messageData.setId(messageId);
        messageData.setQuestionId(TEST_QUESTION_ID);
        messageData.setSenderId(TEST_USER_ID);
        messageData.setContent(imagePath);
        messageData.setMessageType("image");
        messageData.setCreatedAt(System.currentTimeMillis());
        
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.setSuccess(true);
        messageResponse.setData(messageData);
        
        Response<MessageResponse> response = Response.success(messageResponse);
        
        when(apiService.sendMessage(anyString(), any(MessageRequest.class)))
            .thenReturn(messageCall);
        
        ArgumentCaptor<Callback<MessageResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        // Act
        viewModel.sendImageMessage(TEST_QUESTION_ID, imagePath);
        
        verify(messageCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(messageCall, response);
        
        // Assert
        verify(messageDao, atLeast(2)).insert(any(MessageEntity.class));
        verify(messageDao).deleteById(anyLong());
    }
    
    /**
     * 测试接受问题成功
     */
    @Test
    public void testAcceptQuestion_Success() {
        // Arrange
        QuestionEntity question = new QuestionEntity(
            1L, null, "Test question", null, 
            QuestionStatus.PENDING, 
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );
        question.setId(TEST_QUESTION_ID);
        
        when(questionDao.getQuestionById(TEST_QUESTION_ID)).thenReturn(question);
        when(apiService.acceptQuestion(anyString(), any(JsonObject.class)))
            .thenReturn(jsonCall);
        
        JsonObject successResponse = new JsonObject();
        successResponse.addProperty("success", true);
        Response<JsonObject> response = Response.success(successResponse);
        
        ArgumentCaptor<Callback<JsonObject>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        // Act
        viewModel.acceptQuestion(TEST_QUESTION_ID);
        
        verify(jsonCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(jsonCall, response);
        
        // Assert - 应该更新问题状态
        verify(questionDao, atLeastOnce()).update(any(QuestionEntity.class));
    }
    
    /**
     * 测试接受问题失败并回滚
     */
    @Test
    public void testAcceptQuestion_FailureAndRollback() {
        // Arrange
        QuestionEntity question = new QuestionEntity(
            1L, null, "Test question", null, 
            QuestionStatus.PENDING, 
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );
        question.setId(TEST_QUESTION_ID);
        
        when(questionDao.getQuestionById(TEST_QUESTION_ID)).thenReturn(question);
        when(apiService.acceptQuestion(anyString(), any(JsonObject.class)))
            .thenReturn(jsonCall);
        
        Throwable error = new java.io.IOException("Network error");
        
        ArgumentCaptor<Callback<JsonObject>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        // Act
        viewModel.getErrorMessage().observeForever(errorObserver);
        viewModel.acceptQuestion(TEST_QUESTION_ID);
        
        verify(jsonCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(jsonCall, error);
        
        // Assert - 应该回滚状态
        verify(questionDao, atLeast(2)).update(any(QuestionEntity.class));
        verify(errorObserver, atLeastOnce()).onChanged(anyString());
    }
    
    /**
     * 测试关闭问题成功
     */
    @Test
    public void testCloseQuestion_Success() {
        // Arrange
        QuestionEntity question = new QuestionEntity(
            1L, TEST_USER_ID, "Test question", null, 
            QuestionStatus.IN_PROGRESS, 
            System.currentTimeMillis(),
            System.currentTimeMillis()
        );
        question.setId(TEST_QUESTION_ID);
        
        when(questionDao.getQuestionById(TEST_QUESTION_ID)).thenReturn(question);
        when(apiService.closeQuestion(anyString(), any(JsonObject.class)))
            .thenReturn(jsonCall);
        
        JsonObject successResponse = new JsonObject();
        successResponse.addProperty("success", true);
        Response<JsonObject> response = Response.success(successResponse);
        
        ArgumentCaptor<Callback<JsonObject>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        // Act
        viewModel.closeQuestion(TEST_QUESTION_ID);
        
        verify(jsonCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(jsonCall, response);
        
        // Assert
        verify(questionDao, atLeastOnce()).update(any(QuestionEntity.class));
    }
    
    /**
     * 测试防抖机制 - 避免重复发送消息
     */
    @Test
    public void testSendMessage_Debounce() {
        // Arrange
        String content = "Test message";
        
        when(apiService.sendMessage(anyString(), any(MessageRequest.class)))
            .thenReturn(messageCall);
        
        // Act - 快速连续发送两次消息
        viewModel.sendMessage(TEST_QUESTION_ID, content);
        viewModel.sendMessage(TEST_QUESTION_ID, content);
        
        // Assert - 应该只调用一次API（第二次被防抖拦截）
        verify(apiService, times(1)).sendMessage(anyString(), any(MessageRequest.class));
    }
}

