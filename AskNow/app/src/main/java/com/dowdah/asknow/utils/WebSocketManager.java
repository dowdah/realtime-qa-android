package com.dowdah.asknow.utils;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dowdah.asknow.constants.QuestionStatus;
import com.dowdah.asknow.constants.WebSocketMessageType;
import com.dowdah.asknow.data.api.WebSocketClient;
import com.dowdah.asknow.data.local.dao.MessageDao;
import com.dowdah.asknow.data.local.dao.QuestionDao;
import com.dowdah.asknow.data.local.entity.MessageEntity;
import com.dowdah.asknow.data.local.entity.QuestionEntity;
import com.dowdah.asknow.data.model.WebSocketMessage;
import com.dowdah.asknow.data.repository.MessageRepository;
import com.google.gson.JsonObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.OkHttpClient;

@Singleton
public class WebSocketManager {
    private static final String TAG = "WebSocketManager";
    
    private WebSocketClient webSocketClient;
    private final OkHttpClient okHttpClient;
    private final String wsBaseUrl;
    private final MessageRepository messageRepository;
    private final QuestionDao questionDao;
    private final MessageDao messageDao;
    private final SharedPreferencesManager prefsManager;
    private final ExecutorService executor;
    
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<WebSocketMessage> incomingMessage = new MutableLiveData<>();
    private final MutableLiveData<Long> newMessageReceived = new MutableLiveData<>();  // 通知新消息到达，值为 questionId
    
    @Inject
    public WebSocketManager(
        OkHttpClient okHttpClient,
        String wsBaseUrl,
        MessageRepository messageRepository,
        QuestionDao questionDao,
        MessageDao messageDao,
        SharedPreferencesManager prefsManager
    ) {
        this.okHttpClient = okHttpClient;
        this.wsBaseUrl = wsBaseUrl;
        this.messageRepository = messageRepository;
        this.questionDao = questionDao;
        this.messageDao = messageDao;
        this.prefsManager = prefsManager;
        this.executor = Executors.newSingleThreadExecutor();
        
        // 设置网络恢复监听器，统一由 WebSocketManager 处理重连
        messageRepository.setNetworkAvailableListener(() -> {
            Log.d(TAG, "Network available, checking WebSocket connection");
            if (webSocketClient == null || !webSocketClient.isConnected()) {
                Log.d(TAG, "WebSocket not connected, attempting reconnect");
                reconnect();
            } else {
                Log.d(TAG, "WebSocket already connected, no action needed");
            }
        });
    }
    
    public void connect() {
        // 严格清理旧实例，防止多个 WebSocket 同时运行
        if (webSocketClient != null) {
            if (webSocketClient.isConnected()) {
                Log.d(TAG, "WebSocket already connected");
                return;
            }
            Log.d(TAG, "Cleaning up old WebSocketClient instance");
            webSocketClient.disconnect();
            webSocketClient = null;
        }
        
        long userId = prefsManager.getUserId();
        String role = prefsManager.getRole();
        
        if (userId <= 0 || role == null) {
            Log.w(TAG, "Cannot connect: User not logged in");
            return;
        }
        
        String url = wsBaseUrl + userId;
        Log.d(TAG, "Connecting WebSocket for user " + userId + " (" + role + ")");
        
        webSocketClient = new WebSocketClient(okHttpClient, url, new WebSocketClient.WebSocketCallback() {
            @Override
            public void onConnected() {
                Log.d(TAG, "WebSocket connected");
                isConnected.postValue(true);
                messageRepository.onWebSocketConnected();
            }
            
            @Override
            public void onMessage(WebSocketMessage message) {
                Log.d(TAG, "Received message: " + message.getType());
                incomingMessage.postValue(message);
                handleMessage(message, role);
            }
            
            @Override
            public void onDisconnected() {
                Log.d(TAG, "WebSocket disconnected");
                isConnected.postValue(false);
            }
            
            @Override
            public void onError(Throwable error) {
                Log.e(TAG, "WebSocket error", error);
                errorMessage.postValue("Connection error: " + error.getMessage());
            }
        });
        
        messageRepository.setWebSocketClient(webSocketClient);
        webSocketClient.connect();
    }
    
    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.disconnect();
            webSocketClient = null;
        }
        isConnected.postValue(false);
    }
    
    public void reconnect() {
        disconnect();
        connect();
    }
    
    private void handleMessage(WebSocketMessage message, String role) {
        String type = message.getType();
        
        if (WebSocketMessageType.ACK.equals(type)) {
            String messageId = message.getMessageId();
            if (messageId != null) {
                messageRepository.onMessageAcknowledged(messageId);
            }
        } else if (WebSocketMessageType.CHAT_MESSAGE.equals(type)) {
            handleChatMessage(message);
        } else if (WebSocketMessageType.QUESTION_UPDATED.equals(type)) {
            // 新的统一的问题更新消息类型（替代 QUESTION_ACCEPTED 和 QUESTION_CLOSED）
            handleQuestionUpdated(message);
        } else if (WebSocketMessageType.QUESTION_ACCEPTED.equals(type)) {
            // 向后兼容
            handleQuestionAccepted(message);
        } else if (WebSocketMessageType.QUESTION_CLOSED.equals(type)) {
            // 向后兼容
            handleQuestionClosed(message);
        } else if (WebSocketMessageType.NEW_QUESTION.equals(type) && "tutor".equals(role)) {
            handleNewQuestion(message);
        } else if (WebSocketMessageType.NEW_ANSWER.equals(type) && "student".equals(role)) {
            // 向后兼容
        }
    }
    
    private void handleChatMessage(WebSocketMessage message) {
        executor.execute(() -> {
            try {
                if (message == null) {
                    Log.w(TAG, "Received null message");
                    return;
                }
                
                JsonObject data = message.getData();
                if (data == null) {
                    Log.w(TAG, "Message data is null");
                    return;
                }
                
                // 处理两种消息格式：
                // 1. 后端发送的格式：data.id
                // 2. 客户端转发的格式：data.messageId
                long messageId;
                if (data.has("id") && !data.get("id").isJsonNull()) {
                    messageId = data.get("id").getAsLong();
                } else if (data.has("messageId") && !data.get("messageId").isJsonNull()) {
                    messageId = data.get("messageId").getAsLong();
                } else {
                    Log.w(TAG, "Message missing both id and messageId fields");
                    return;
                }
                
                // 验证必要字段
                if (!data.has("questionId") || data.get("questionId").isJsonNull()) {
                    Log.w(TAG, "Message missing questionId field");
                    return;
                }
                if (!data.has("senderId") || data.get("senderId").isJsonNull()) {
                    Log.w(TAG, "Message missing senderId field");
                    return;
                }
                if (!data.has("content") || data.get("content").isJsonNull()) {
                    Log.w(TAG, "Message missing content field");
                    return;
                }
                
                long questionId = data.get("questionId").getAsLong();
                long senderId = data.get("senderId").getAsLong();
                String content = data.get("content").getAsString();
                
                // 获取消息类型，默认为text
                String messageType = "text";
                if (data.has("messageType") && !data.get("messageType").isJsonNull()) {
                    messageType = data.get("messageType").getAsString();
                }
                
                // 获取创建时间，如果没有则使用当前时间
                long createdAt = System.currentTimeMillis();
                if (data.has("createdAt") && !data.get("createdAt").isJsonNull()) {
                    createdAt = data.get("createdAt").getAsLong();
                }
                
                // 获取已读状态，默认为未读
                boolean isRead = false;
                if (data.has("isRead") && !data.get("isRead").isJsonNull()) {
                    isRead = data.get("isRead").getAsBoolean();
                }
                
                MessageEntity entity = new MessageEntity(
                    questionId,
                    senderId,
                    content,
                    messageType,
                    createdAt
                );
                entity.setId(messageId);
                entity.setRead(isRead); // 设置已读状态
                messageDao.insert(entity);
                
                // 通知界面有新消息到达（触发未读数量刷新）
                newMessageReceived.postValue(questionId);
                
                Log.d(TAG, "Saved chat message from WebSocket: id=" + messageId + ", questionId=" + questionId);
            } catch (Exception e) {
                Log.e(TAG, "Error handling chat message", e);
            }
        });
    }
    
    private void handleQuestionUpdated(WebSocketMessage message) {
        executor.execute(() -> {
            try {
                if (message == null) {
                    Log.w(TAG, "Received null message");
                    return;
                }
                
                JsonObject data = message.getData();
                if (data == null) {
                    Log.w(TAG, "Message data is null");
                    return;
                }
                
                // 验证必要字段
                if (!data.has("questionId") || data.get("questionId").isJsonNull()) {
                    Log.w(TAG, "Question update missing questionId field");
                    return;
                }
                if (!data.has("userId") || data.get("userId").isJsonNull()) {
                    Log.w(TAG, "Question update missing userId field");
                    return;
                }
                if (!data.has("content") || data.get("content").isJsonNull()) {
                    Log.w(TAG, "Question update missing content field");
                    return;
                }
                if (!data.has("status") || data.get("status").isJsonNull()) {
                    Log.w(TAG, "Question update missing status field");
                    return;
                }
                
                long questionId = data.get("questionId").getAsLong();
                long userId = data.get("userId").getAsLong();
                Long tutorId = data.has("tutorId") && !data.get("tutorId").isJsonNull() 
                    ? data.get("tutorId").getAsLong() : null;
                String content = data.get("content").getAsString();
                String imagePath = data.has("imagePath") && !data.get("imagePath").isJsonNull() 
                    ? data.get("imagePath").getAsString() : null;
                String status = data.get("status").getAsString();
                long createdAt = data.has("createdAt") && !data.get("createdAt").isJsonNull()
                    ? data.get("createdAt").getAsLong() : System.currentTimeMillis();
                long updatedAt = data.has("updatedAt") && !data.get("updatedAt").isJsonNull()
                    ? data.get("updatedAt").getAsLong() : System.currentTimeMillis();
                
                // 使用 INSERT OR REPLACE 策略确保 LiveData 被触发
                QuestionEntity entity = new QuestionEntity(
                    userId,
                    tutorId,
                    content,
                    imagePath,
                    status,
                    createdAt,
                    updatedAt
                );
                entity.setId(questionId);
                // 使用 update 而不是 insert，避免触发外键级联删除导致消息丢失
                questionDao.update(entity);
                
                Log.d(TAG, "Question updated from WebSocket: " + questionId + ", status: " + status);
            } catch (Exception e) {
                Log.e(TAG, "Error handling question updated", e);
            }
        });
    }
    
    private void handleQuestionAccepted(WebSocketMessage message) {
        updateQuestionStatus(message, QuestionStatus.IN_PROGRESS, true, "QUESTION_ACCEPTED");
    }
    
    private void handleQuestionClosed(WebSocketMessage message) {
        updateQuestionStatus(message, QuestionStatus.CLOSED, false, "QUESTION_CLOSED");
    }
    
    /**
     * 统一处理问题状态更新
     * 
     * @param message WebSocket消息
     * @param newStatus 新状态
     * @param updateTutor 是否更新tutorId
     * @param messageType 消息类型（用于日志）
     */
    private void updateQuestionStatus(WebSocketMessage message, String newStatus, boolean updateTutor, String messageType) {
        executor.execute(() -> {
            try {
                if (message == null || message.getData() == null) {
                    Log.w(TAG, "Invalid message for " + messageType);
                    return;
                }
                
                JsonObject data = message.getData();
                if (!data.has("questionId") || data.get("questionId").isJsonNull()) {
                    Log.w(TAG, messageType + " missing questionId");
                    return;
                }
                
                long questionId = data.get("questionId").getAsLong();
                QuestionEntity question = questionDao.getQuestionById(questionId);
                
                if (question != null) {
                    question.setStatus(newStatus);
                    question.setUpdatedAt(System.currentTimeMillis());
                    
                    // 如果需要更新tutorId
                    if (updateTutor && data.has("tutorId") && !data.get("tutorId").isJsonNull()) {
                        long tutorId = data.get("tutorId").getAsLong();
                        question.setTutorId(tutorId);
                    }
                    
                    // 使用 update 而不是 insert，避免触发外键级联删除导致消息丢失
                    questionDao.update(question);
                    Log.d(TAG, "Updated question " + questionId + " status to " + newStatus + " via " + messageType);
                } else {
                    Log.w(TAG, "Question not found in database for " + messageType + ": " + questionId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling " + messageType, e);
            }
        });
    }
    
    private void handleNewQuestion(WebSocketMessage message) {
        executor.execute(() -> {
            try {
                JsonObject data = message.getData();
                long questionId = data.get("questionId").getAsLong();
                long userId = data.get("userId").getAsLong();
                String content = data.get("content").getAsString();
                String imagePath = data.has("imagePath") && !data.get("imagePath").isJsonNull() 
                    ? data.get("imagePath").getAsString() : null;
                String status = data.get("status").getAsString();
                long createdAt = data.get("createdAt").getAsLong();
                
                QuestionEntity entity = new QuestionEntity(
                    userId,
                    null,
                    content,
                    imagePath,
                    status,
                    createdAt,
                    createdAt
                );
                entity.setId(questionId);
                questionDao.insert(entity);
                
                Log.d(TAG, "Question saved from WebSocket");
            } catch (Exception e) {
                Log.e(TAG, "Error handling new question", e);
            }
        });
    }
    
    public LiveData<Boolean> isConnected() {
        return isConnected;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<WebSocketMessage> getIncomingMessage() {
        return incomingMessage;
    }
    
    public LiveData<Long> getNewMessageReceived() {
        return newMessageReceived;
    }
    
    public void sendMessage(WebSocketMessage message) {
        if (webSocketClient != null && webSocketClient.isConnected()) {
            webSocketClient.sendMessage(message);
        } else {
            Log.w(TAG, "Cannot send message: WebSocket not connected");
        }
    }
    
    public void cleanup() {
        disconnect();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            Log.d(TAG, "Executor shutdown");
        }
    }
}

