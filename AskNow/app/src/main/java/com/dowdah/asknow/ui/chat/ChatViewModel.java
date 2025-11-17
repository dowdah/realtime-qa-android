package com.dowdah.asknow.ui.chat;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dowdah.asknow.R;
import com.dowdah.asknow.constants.MessageStatus;
import com.dowdah.asknow.constants.QuestionStatus;
import com.dowdah.asknow.data.api.ApiService;
import com.dowdah.asknow.data.local.dao.MessageDao;
import com.dowdah.asknow.data.local.dao.QuestionDao;
import com.dowdah.asknow.data.local.entity.MessageEntity;
import com.dowdah.asknow.data.local.entity.QuestionEntity;

import java.util.List;
import com.dowdah.asknow.data.model.MessageRequest;
import com.dowdah.asknow.data.model.MessageResponse;
import com.dowdah.asknow.data.repository.MessageRepository;
import com.dowdah.asknow.utils.SharedPreferencesManager;
import com.google.gson.JsonObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ChatViewModel - 聊天界面的ViewModel
 * 负责处理消息的发送、接收和问题状态的管理
 * 
 * 主要功能：
 * - 乐观更新：发送消息时立即显示，提升用户体验
 * - 消息状态管理：pending（发送中）、sent（已发送）、failed（失败）
 * - 问题状态管理：接受问题、关闭问题
 * - 已读未读管理：标记消息为已读
 */
@HiltViewModel
public class ChatViewModel extends AndroidViewModel {
    private static final String TAG = "ChatViewModel";
    
    private final ApiService apiService;
    private final QuestionDao questionDao;
    private final MessageDao messageDao;
    private final MessageRepository messageRepository;
    private final SharedPreferencesManager prefsManager;
    private final ExecutorService executor;
    
    // 使用AtomicLong生成唯一的临时消息ID
    private final AtomicLong tempIdGenerator = new AtomicLong(-System.currentTimeMillis());
    
    // 锁对象，用于保护临时消息的删除和插入操作
    private final Object messageLock = new Object();
    
    // 锁对象，用于保护问题状态操作
    private final Object questionLock = new Object();
    
    // 防抖：记录正在进行的操作
    private volatile boolean isAcceptingQuestion = false;
    private volatile boolean isClosingQuestion = false;
    private volatile boolean isSendingMessage = false;
    
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> messageSent = new MutableLiveData<>();
    
    @Inject
    public ChatViewModel(
        @NonNull Application application,
        ApiService apiService,
        QuestionDao questionDao,
        MessageDao messageDao,
        MessageRepository messageRepository,
        SharedPreferencesManager prefsManager
    ) {
        super(application);
        this.apiService = apiService;
        this.questionDao = questionDao;
        this.messageDao = messageDao;
        this.messageRepository = messageRepository;
        this.prefsManager = prefsManager;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public LiveData<List<MessageEntity>> getMessagesByQuestionId(long questionId) {
        return messageDao.getMessagesByQuestionId(questionId);
    }
    
    /**
     * 获取未读消息数量
     */
    public LiveData<Integer> getUnreadMessageCount(long questionId) {
        long currentUserId = prefsManager.getUserId();
        return messageRepository.getUnreadMessageCount(questionId, currentUserId);
    }
    
    /**
     * 标记消息为已读
     */
    public void markMessagesAsRead(long questionId) {
        String token = prefsManager.getToken();
        long currentUserId = prefsManager.getUserId();
        
        messageRepository.markMessagesAsRead(token, questionId, currentUserId, new MessageRepository.MarkReadCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Messages marked as read successfully");
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error marking messages as read: " + error);
            }
        });
    }
    
    public void sendMessage(long questionId, String content) {
        // 防抖：避免重复发送
        if (isSendingMessage) {
            Log.w(TAG, "Message is already being sent, ignoring duplicate request");
            return;
        }
        isSendingMessage = true;
        
        // 使用AtomicLong生成唯一的临时ID（负数避免与真实ID冲突）
        final long tempId = tempIdGenerator.decrementAndGet();
        long currentUserId = prefsManager.getUserId();
        long currentTime = System.currentTimeMillis();
        
        // 1. 乐观更新：立即插入本地数据库，状态为 pending
        executor.execute(() -> {
            synchronized (messageLock) {
                MessageEntity tempEntity = new MessageEntity(
                    questionId,
                    currentUserId,
                    content,
                    "text",
                    currentTime
                );
                tempEntity.setId(tempId);
                tempEntity.setSendStatus(MessageStatus.PENDING);
                tempEntity.setRead(true); // 自己发送的消息标记为已读
                messageDao.insert(tempEntity);
                Log.d(TAG, "Optimistic update: inserted temp message with id=" + tempId);
            }
        });
        
        // 2. 发送 HTTP API
        String token = "Bearer " + prefsManager.getToken();
        MessageRequest request = new MessageRequest(questionId, content, "text");
        
        apiService.sendMessage(token, request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    MessageResponse.MessageData data = response.body().getData();
                    
                    // 3. 替换临时消息为真实消息（使用锁保护，防止竞态条件）
                    executor.execute(() -> {
                        synchronized (messageLock) {
                            // 删除临时消息
                            messageDao.deleteById(tempId);
                            
                            // 插入真实消息
                            MessageEntity realEntity = new MessageEntity(
                                data.getQuestionId(),
                                data.getSenderId(),
                                data.getContent(),
                                data.getMessageType(),
                                data.getCreatedAt()
                            );
                            realEntity.setId(data.getId());
                            realEntity.setSendStatus(MessageStatus.SENT);
                            realEntity.setRead(true); // 自己发送的消息标记为已读
                            messageDao.insert(realEntity);
                            
                            Log.d(TAG, "Message sent successfully: replaced temp id=" + tempId + " with real id=" + data.getId());
                        }
                    });
                    
                    // 注意：不需要再通过 WebSocket 发送消息
                    // HTTP API 后端已经处理了消息的保存和 WebSocket 推送
                    
                    isSendingMessage = false; // 重置发送标志
                    messageSent.postValue(true);
                } else {
                    // 4. 标记失败（使用锁保护）
                    executor.execute(() -> {
                        synchronized (messageLock) {
                            messageDao.updateSendStatus(tempId, MessageStatus.FAILED);
                            Log.e(TAG, "Message send failed: marked temp id=" + tempId + " as failed");
                        }
                    });
                    isSendingMessage = false; // 重置发送标志
                    errorMessage.postValue(getApplication().getString(R.string.failed_to_send_message));
                }
            }
            
            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                // 5. 标记失败（使用锁保护）
                executor.execute(() -> {
                    synchronized (messageLock) {
                        messageDao.updateSendStatus(tempId, MessageStatus.FAILED);
                        Log.e(TAG, "Message send error: marked temp id=" + tempId + " as failed", t);
                    }
                });
                isSendingMessage = false; // 重置发送标志
                errorMessage.postValue(getApplication().getString(R.string.network_error, t.getMessage()));
            }
        });
    }
    
    public void acceptQuestion(long questionId) {
        // 防抖：避免重复接受
        if (isAcceptingQuestion) {
            Log.w(TAG, "Question is already being accepted, ignoring duplicate request");
            return;
        }
        isAcceptingQuestion = true;
        
        final long tutorId = prefsManager.getUserId();
        // 保存原始状态用于回滚
        final Long[] originalTutorId = new Long[1];
        final String[] originalStatus = new String[1];
        
        // 1. 乐观更新：立即更新本地数据库（使用锁保护）
        executor.execute(() -> {
            synchronized (questionLock) {
                QuestionEntity question = questionDao.getQuestionById(questionId);
                if (question != null) {
                    originalTutorId[0] = question.getTutorId();
                    originalStatus[0] = question.getStatus();
                    question.setStatus(QuestionStatus.IN_PROGRESS);
                    question.setTutorId(tutorId);
                    question.setUpdatedAt(System.currentTimeMillis());
                    questionDao.update(question);
                    Log.d(TAG, "Optimistic update: accepted question " + questionId);
                }
            }
        });
        
        // 2. 发送API请求
        String token = "Bearer " + prefsManager.getToken();
        JsonObject request = new JsonObject();
        request.addProperty("questionId", questionId);
        
        apiService.acceptQuestion(token, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // 注意：不需要再通过 WebSocket 发送通知
                    // HTTP API 后端已经处理了 WebSocket 广播
                    isAcceptingQuestion = false; // 重置标志
                    Log.d(TAG, "Question accepted successfully");
                } else {
                    // 3. API失败，回滚本地更新（使用锁保护）
                    executor.execute(() -> {
                        synchronized (questionLock) {
                            QuestionEntity question = questionDao.getQuestionById(questionId);
                            if (question != null) {
                                question.setStatus(originalStatus[0] != null ? originalStatus[0] : QuestionStatus.PENDING);
                                question.setTutorId(originalTutorId[0]);
                                questionDao.update(question);
                                Log.e(TAG, "Failed to accept question, rolled back local update");
                            }
                        }
                    });
                    isAcceptingQuestion = false; // 重置标志
                    errorMessage.postValue(getApplication().getString(R.string.failed_to_accept_question));
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // 4. 网络失败，回滚本地更新（使用锁保护）
                executor.execute(() -> {
                    synchronized (questionLock) {
                        QuestionEntity question = questionDao.getQuestionById(questionId);
                        if (question != null) {
                            question.setStatus(originalStatus[0] != null ? originalStatus[0] : QuestionStatus.PENDING);
                            question.setTutorId(originalTutorId[0]);
                            questionDao.update(question);
                            Log.e(TAG, "Error accepting question, rolled back local update", t);
                        }
                    }
                });
                isAcceptingQuestion = false; // 重置标志
                errorMessage.postValue(getApplication().getString(R.string.failed_to_accept_question));
            }
        });
    }
    
    public void closeQuestion(long questionId) {
        // 防抖：避免重复关闭
        if (isClosingQuestion) {
            Log.w(TAG, "Question is already being closed, ignoring duplicate request");
            return;
        }
        isClosingQuestion = true;
        
        // 保存原始状态用于回滚
        final String[] originalStatus = new String[1];
        
        // 1. 乐观更新：立即更新本地数据库（使用锁保护）
        executor.execute(() -> {
            synchronized (questionLock) {
                QuestionEntity question = questionDao.getQuestionById(questionId);
                if (question != null) {
                    originalStatus[0] = question.getStatus();
                    question.setStatus(QuestionStatus.CLOSED);
                    question.setUpdatedAt(System.currentTimeMillis());
                    questionDao.update(question);
                    Log.d(TAG, "Optimistic update: closed question " + questionId);
                }
            }
        });
        
        // 2. 发送API请求
        String token = "Bearer " + prefsManager.getToken();
        JsonObject request = new JsonObject();
        request.addProperty("questionId", questionId);
        
        apiService.closeQuestion(token, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // 注意：不需要再通过 WebSocket 发送通知
                    // HTTP API 后端已经处理了 WebSocket 推送
                    isClosingQuestion = false; // 重置标志
                    Log.d(TAG, "Question closed successfully");
                } else {
                    // 3. API失败，回滚本地更新（使用锁保护）
                    executor.execute(() -> {
                        synchronized (questionLock) {
                            QuestionEntity question = questionDao.getQuestionById(questionId);
                            if (question != null && originalStatus[0] != null) {
                                question.setStatus(originalStatus[0]);
                                questionDao.update(question);
                                Log.e(TAG, "Failed to close question, rolled back local update");
                            }
                        }
                    });
                    isClosingQuestion = false; // 重置标志
                    errorMessage.postValue(getApplication().getString(R.string.failed_to_close_question));
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // 4. 网络失败，回滚本地更新（使用锁保护）
                executor.execute(() -> {
                    synchronized (questionLock) {
                        QuestionEntity question = questionDao.getQuestionById(questionId);
                        if (question != null && originalStatus[0] != null) {
                            question.setStatus(originalStatus[0]);
                            questionDao.update(question);
                            Log.e(TAG, "Error closing question, rolled back local update", t);
                        }
                    }
                });
                isClosingQuestion = false; // 重置标志
                errorMessage.postValue(getApplication().getString(R.string.failed_to_close_question));
            }
        });
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getMessageSent() {
        return messageSent;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}

