package com.dowdah.asknow.ui.chat;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dowdah.asknow.R;
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

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class ChatViewModel extends AndroidViewModel {
    private static final String TAG = "ChatViewModel";
    
    private final ApiService apiService;
    private final QuestionDao questionDao;
    private final MessageDao messageDao;
    private final MessageRepository messageRepository;
    private final SharedPreferencesManager prefsManager;
    private final ExecutorService executor;
    
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
        // 生成临时 ID（使用负数避免与真实 ID 冲突）
        long tempId = -System.currentTimeMillis();
        long currentUserId = prefsManager.getUserId();
        long currentTime = System.currentTimeMillis();
        
        // 1. 乐观更新：立即插入本地数据库，状态为 pending
        executor.execute(() -> {
            MessageEntity tempEntity = new MessageEntity(
                questionId,
                currentUserId,
                content,
                "text",
                currentTime
            );
            tempEntity.setId(tempId);
            tempEntity.setSendStatus("pending");
            tempEntity.setRead(true); // 自己发送的消息标记为已读
            messageDao.insert(tempEntity);
            Log.d(TAG, "Optimistic update: inserted temp message with id=" + tempId);
        });
        
        // 2. 发送 HTTP API
        String token = "Bearer " + prefsManager.getToken();
        MessageRequest request = new MessageRequest(questionId, content, "text");
        
        apiService.sendMessage(token, request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    MessageResponse.MessageData data = response.body().getData();
                    
                    // 3. 替换临时消息为真实消息
                    executor.execute(() -> {
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
                        realEntity.setSendStatus("sent");
                        realEntity.setRead(true); // 自己发送的消息标记为已读
                        messageDao.insert(realEntity);
                        
                        Log.d(TAG, "Message sent successfully: replaced temp id=" + tempId + " with real id=" + data.getId());
                    });
                    
                    // 注意：不需要再通过 WebSocket 发送消息
                    // HTTP API 后端已经处理了消息的保存和 WebSocket 推送
                    
                    messageSent.postValue(true);
                } else {
                    // 4. 标记失败
                    executor.execute(() -> {
                        messageDao.updateSendStatus(tempId, "failed");
                        Log.e(TAG, "Message send failed: marked temp id=" + tempId + " as failed");
                    });
                    errorMessage.postValue(getApplication().getString(R.string.failed_to_send_message));
                }
            }
            
            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                // 5. 标记失败
                executor.execute(() -> {
                    messageDao.updateSendStatus(tempId, "failed");
                    Log.e(TAG, "Message send error: marked temp id=" + tempId + " as failed", t);
                });
                errorMessage.postValue(getApplication().getString(R.string.network_error, t.getMessage()));
            }
        });
    }
    
    public void acceptQuestion(long questionId) {
        String token = "Bearer " + prefsManager.getToken();
        JsonObject request = new JsonObject();
        request.addProperty("questionId", questionId);
        
        apiService.acceptQuestion(token, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // Update local database
                    executor.execute(() -> {
                        QuestionEntity question = questionDao.getQuestionById(questionId);
                        if (question != null) {
                            question.setStatus("in_progress");
                            question.setTutorId(prefsManager.getUserId());
                            questionDao.update(question);
                        }
                    });
                    
                    // 注意：不需要再通过 WebSocket 发送通知
                    // HTTP API 后端已经处理了 WebSocket 广播
                    
                    Log.d(TAG, "Question accepted");
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error accepting question", t);
                errorMessage.postValue(getApplication().getString(R.string.failed_to_accept_question));
            }
        });
    }
    
    public void closeQuestion(long questionId) {
        String token = "Bearer " + prefsManager.getToken();
        JsonObject request = new JsonObject();
        request.addProperty("questionId", questionId);
        
        apiService.closeQuestion(token, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // Update local database
                    executor.execute(() -> {
                        QuestionEntity question = questionDao.getQuestionById(questionId);
                        if (question != null) {
                            question.setStatus("closed");
                            questionDao.update(question);
                        }
                    });
                    
                    // 注意：不需要再通过 WebSocket 发送通知
                    // HTTP API 后端已经处理了 WebSocket 推送
                    
                    Log.d(TAG, "Question closed");
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error closing question", t);
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

