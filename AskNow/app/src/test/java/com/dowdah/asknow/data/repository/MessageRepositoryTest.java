package com.dowdah.asknow.data.repository;

import android.content.Context;
import android.net.ConnectivityManager;

import com.dowdah.asknow.data.api.ApiService;
import com.dowdah.asknow.data.api.WebSocketClient;
import com.dowdah.asknow.data.local.dao.MessageDao;
import com.dowdah.asknow.data.local.dao.PendingMessageDao;
import com.dowdah.asknow.data.local.entity.PendingMessageEntity;
import com.dowdah.asknow.data.model.WebSocketMessage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * MessageRepository 单元测试
 * 
 * 测试功能：
 * - WebSocket消息发送（在线/离线）
 * - 待发消息队列管理
 * - 网络状态监听
 * - 标记消息为已读
 */
@RunWith(MockitoJUnitRunner.class)
public class MessageRepositoryTest {
    
    @Mock
    private Context context;
    
    @Mock
    private PendingMessageDao pendingMessageDao;
    
    @Mock
    private MessageDao messageDao;
    
    @Mock
    private ApiService apiService;
    
    @Mock
    private WebSocketClient webSocketClient;
    
    @Mock
    private ConnectivityManager connectivityManager;
    
    @Mock
    private Call<JsonObject> apiCall;
    
    private ExecutorService executor;
    private Gson gson;
    private MessageRepository repository;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        executor = Executors.newSingleThreadExecutor();
        gson = new Gson();
        
        // Mock Context
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(connectivityManager);
        
        repository = new MessageRepository(
            context,
            pendingMessageDao,
            messageDao,
            apiService,
            executor,
            gson
        );
        
        repository.setWebSocketClient(webSocketClient);
    }
    
    /**
     * 测试在线发送消息
     */
    @Test
    public void testSendMessage_Online() {
        // Arrange
        String messageType = "new_message";
        JsonObject data = new JsonObject();
        data.addProperty("content", "Test message");
        
        when(webSocketClient.isConnected()).thenReturn(true);
        
        // Act
        repository.sendMessage(messageType, data);
        
        // Assert - 应该直接通过WebSocket发送
        ArgumentCaptor<WebSocketMessage> messageCaptor = 
            ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(webSocketClient).sendMessage(messageCaptor.capture());
        
        WebSocketMessage sentMessage = messageCaptor.getValue();
        assertEquals(messageType, sentMessage.getType());
        assertNotNull(sentMessage.getMessageId());
        
        // 不应该保存到待发消息队列
        verify(pendingMessageDao, never()).insert(any(PendingMessageEntity.class));
    }
    
    /**
     * 测试离线发送消息
     */
    @Test
    public void testSendMessage_Offline() throws InterruptedException {
        // Arrange
        String messageType = "new_message";
        JsonObject data = new JsonObject();
        data.addProperty("content", "Test message");
        
        when(webSocketClient.isConnected()).thenReturn(false);
        
        // Act
        repository.sendMessage(messageType, data);
        
        // 等待异步操作完成
        Thread.sleep(200);
        
        // Assert - 应该保存到待发消息队列
        ArgumentCaptor<PendingMessageEntity> entityCaptor = 
            ArgumentCaptor.forClass(PendingMessageEntity.class);
        verify(pendingMessageDao).insert(entityCaptor.capture());
        
        PendingMessageEntity entity = entityCaptor.getValue();
        assertEquals(messageType, entity.getMessageType());
        assertNotNull(entity.getMessageId());
        
        // 不应该通过WebSocket发送
        verify(webSocketClient, never()).sendMessage(any(WebSocketMessage.class));
    }
    
    /**
     * 测试WebSocket连接后发送待发消息
     */
    @Test
    public void testOnWebSocketConnected_SendsPendingMessages() throws InterruptedException {
        // Arrange
        PendingMessageEntity pending1 = new PendingMessageEntity();
        pending1.setId(1L);
        pending1.setMessageType("new_message");
        pending1.setMessageId("msg1");
        
        WebSocketMessage wsMessage1 = new WebSocketMessage();
        wsMessage1.setType("new_message");
        wsMessage1.setMessageId("msg1");
        pending1.setPayload(gson.toJson(wsMessage1));
        
        PendingMessageEntity pending2 = new PendingMessageEntity();
        pending2.setId(2L);
        pending2.setMessageType("new_message");
        pending2.setMessageId("msg2");
        
        WebSocketMessage wsMessage2 = new WebSocketMessage();
        wsMessage2.setType("new_message");
        wsMessage2.setMessageId("msg2");
        pending2.setPayload(gson.toJson(wsMessage2));
        
        List<PendingMessageEntity> pendingMessages = Arrays.asList(pending1, pending2);
        
        when(pendingMessageDao.getAllPendingMessages()).thenReturn(pendingMessages);
        when(webSocketClient.isConnected()).thenReturn(true);
        
        // Act
        repository.onWebSocketConnected();
        
        // 等待异步操作完成
        Thread.sleep(300);
        
        // Assert - 应该发送所有待发消息
        verify(webSocketClient, times(2)).sendMessage(any(WebSocketMessage.class));
    }
    
    /**
     * 测试消息确认后删除待发消息
     */
    @Test
    public void testOnMessageAcknowledged_DeletesPendingMessage() throws InterruptedException {
        // Arrange
        String messageId = "msg123";
        PendingMessageEntity pendingMessage = new PendingMessageEntity();
        pendingMessage.setId(1L);
        pendingMessage.setMessageId(messageId);
        
        when(pendingMessageDao.getMessageByMessageId(messageId)).thenReturn(pendingMessage);
        
        // Act
        repository.onMessageAcknowledged(messageId);
        
        // 等待异步操作完成
        Thread.sleep(200);
        
        // Assert - 应该删除待发消息
        verify(pendingMessageDao).deleteMessage(1L);
    }
    
    /**
     * 测试超过最大重试次数的消息被删除
     */
    @Test
    public void testPendingMessages_MaxRetriesExceeded() throws InterruptedException {
        // Arrange
        PendingMessageEntity exceededMessage = new PendingMessageEntity();
        exceededMessage.setId(1L);
        exceededMessage.setMessageId("msg1");
        exceededMessage.setRetryCount(5); // 超过最大重试次数
        
        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setMessageId("msg1");
        exceededMessage.setPayload(gson.toJson(wsMessage));
        
        List<PendingMessageEntity> pendingMessages = Arrays.asList(exceededMessage);
        
        when(pendingMessageDao.getAllPendingMessages()).thenReturn(pendingMessages);
        when(webSocketClient.isConnected()).thenReturn(true);
        
        // Act
        repository.onWebSocketConnected();
        
        // 等待异步操作完成
        Thread.sleep(200);
        
        // Assert - 应该删除超过重试次数的消息
        verify(pendingMessageDao).deleteMessage(1L);
        
        // 不应该发送该消息
        verify(webSocketClient, never()).sendMessage(any(WebSocketMessage.class));
    }
    
    /**
     * 测试标记消息为已读 - 成功
     */
    @Test
    public void testMarkMessagesAsRead_Success() throws InterruptedException {
        // Arrange
        String token = "test_token";
        long questionId = 1L;
        long currentUserId = 2L;
        
        when(apiService.markMessagesAsRead(anyString(), any(JsonObject.class)))
            .thenReturn(apiCall);
        
        MessageRepository.MarkReadCallback callback = mock(MessageRepository.MarkReadCallback.class);
        
        // Act
        repository.markMessagesAsRead(token, questionId, currentUserId, callback);
        
        // 等待异步操作完成
        Thread.sleep(300);
        
        // Assert - 应该更新本地数据库
        verify(messageDao).markMessagesAsRead(eq(questionId), eq(currentUserId));
    }
    
    /**
     * 测试获取未读消息数量
     */
    @Test
    public void testGetUnreadMessageCountAsync() throws InterruptedException {
        // Arrange
        long questionId = 1L;
        long currentUserId = 2L;
        int unreadCount = 5;
        
        when(messageDao.getUnreadMessageCount(questionId, currentUserId))
            .thenReturn(unreadCount);
        
        MessageRepository.UnreadCountCallback callback = mock(MessageRepository.UnreadCountCallback.class);
        
        // Act
        repository.getUnreadMessageCountAsync(questionId, currentUserId, callback);
        
        // 等待异步操作完成
        Thread.sleep(200);
        
        // Assert
        verify(callback).onCountReceived(unreadCount);
    }
}

