package com.dowdah.asknow.data.repository;

import android.util.Log;

import com.dowdah.asknow.data.api.ApiService;
import com.dowdah.asknow.data.local.dao.MessageDao;
import com.dowdah.asknow.data.local.dao.QuestionDao;
import com.dowdah.asknow.data.local.entity.MessageEntity;
import com.dowdah.asknow.data.local.entity.QuestionEntity;
import com.dowdah.asknow.data.model.MessagesListResponse;
import com.dowdah.asknow.data.model.QuestionsListResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 问题同步Repository
 * 负责从服务器同步问题数据到本地数据库
 */
@Singleton
public class QuestionRepository {
    private static final String TAG = "QuestionRepository";
    
    private final ApiService apiService;
    private final QuestionDao questionDao;
    private final MessageDao messageDao;
    private final ExecutorService executor;
    
    @Inject
    public QuestionRepository(ApiService apiService, QuestionDao questionDao, MessageDao messageDao) {
        this.apiService = apiService;
        this.questionDao = questionDao;
        this.messageDao = messageDao;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 同步问题从服务器到本地
     * 对于学生：同步自己提出的问题
     * 对于老师：同步自己接取或已完结的问题
     * 
     * @param token 认证token
     * @param userId 当前用户ID
     * @param role 用户角色 ("student" 或 "tutor")
     * @param callback 同步回调
     */
    public void syncQuestionsFromServer(String token, long userId, String role, SyncCallback callback) {
        syncQuestionsFromServer(token, userId, role, 1, 20, false, callback);
    }
    
    /**
     * 同步问题从服务器到本地（支持分页）
     * 
     * @param token 认证token
     * @param userId 当前用户ID
     * @param role 用户角色
     * @param page 页码
     * @param pageSize 每页大小
     * @param isAppendMode 是否为追加模式（true=追加，false=刷新）
     * @param callback 同步回调
     */
    public void syncQuestionsFromServer(String token, long userId, String role, int page, int pageSize, boolean isAppendMode, SyncCallback callback) {
        Log.d(TAG, "Starting sync for user " + userId + " with role " + role + " page=" + page);
        
        String authHeader = "Bearer " + token;
        apiService.getQuestions(authHeader, null, page, pageSize).enqueue(new Callback<QuestionsListResponse>() {
            @Override
            public void onResponse(Call<QuestionsListResponse> call, Response<QuestionsListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<QuestionsListResponse.QuestionData> serverQuestions = response.body().getQuestions();
                    com.dowdah.asknow.data.model.Pagination pagination = response.body().getPagination();
                    boolean hasMore = pagination != null && pagination.hasMore();
                    
                    executor.execute(() -> {
                        try {
                            // 获取服务器返回的问题ID集合
                            Set<Long> serverQuestionIds = new HashSet<>();
                            for (QuestionsListResponse.QuestionData serverQuestion : serverQuestions) {
                                serverQuestionIds.add(serverQuestion.getId());
                                
                                // 插入或更新本地数据库
                                QuestionEntity entity = new QuestionEntity();
                                entity.setId(serverQuestion.getId());
                                entity.setUserId(serverQuestion.getUserId());
                                entity.setTutorId(serverQuestion.getTutorId());
                                entity.setContent(serverQuestion.getContent());
                                entity.setImagePath(serverQuestion.getImagePath());
                                entity.setStatus(serverQuestion.getStatus());
                                entity.setCreatedAt(serverQuestion.getCreatedAt());
                                entity.setUpdatedAt(serverQuestion.getUpdatedAt());
                                
                                questionDao.insert(entity);
                            }
                            
                            // 只在非追加模式（刷新模式）且是第一页时清理本地不存在于服务器的数据
                            if (!isAppendMode && page == 1) {
                                // 删除本地存在但服务器不存在的问题
                                List<QuestionEntity> localQuestions;
                                if ("student".equals(role)) {
                                    // 学生端：获取本地所有该学生创建的问题
                                    localQuestions = questionDao.getQuestionsByUserIdSync(userId);
                                } else {
                                    // 老师端：获取本地所有该老师接取的问题
                                    localQuestions = questionDao.getQuestionsByTutorId(userId);
                                }
                                
                                if (localQuestions != null) {
                                    for (QuestionEntity localQuestion : localQuestions) {
                                        if (!serverQuestionIds.contains(localQuestion.getId())) {
                                            // 服务器不存在该问题，从本地删除
                                            questionDao.deleteQuestion(localQuestion.getId());
                                            Log.d(TAG, "Deleted question " + localQuestion.getId() + " (not on server)");
                                        }
                                    }
                                }
                            }
                            
                            // 同步每个问题的消息
                            if (!serverQuestions.isEmpty()) {
                                Log.d(TAG, "Starting messages sync for " + serverQuestions.size() + " questions");
                                syncMessagesForQuestions(token, serverQuestions, hasMore, callback);
                            } else {
                                Log.d(TAG, "Sync completed successfully. Synced " + serverQuestions.size() + " questions");
                                if (callback != null) {
                                    callback.onSuccess(serverQuestions.size());
                                    callback.onPageLoaded(hasMore);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error during sync", e);
                            if (callback != null) {
                                callback.onError("同步数据时发生错误: " + e.getMessage());
                            }
                        }
                    });
                } else {
                    Log.e(TAG, "Sync failed: " + response.code());
                    if (callback != null) {
                        callback.onError("同步失败: 服务器返回错误");
                    }
                }
            }
            
            @Override
            public void onFailure(Call<QuestionsListResponse> call, Throwable t) {
                Log.e(TAG, "Sync network error", t);
                if (callback != null) {
                    callback.onError("网络错误: " + t.getMessage());
                }
            }
        });
    }
    
    /**
     * 同步每个问题的消息
     */
    private void syncMessagesForQuestions(String token, List<QuestionsListResponse.QuestionData> questions, boolean hasMore, SyncCallback callback) {
        if (questions.isEmpty()) {
            if (callback != null) {
                callback.onSuccess(0);
                callback.onPageLoaded(hasMore);
            }
            return;
        }
        
        String authHeader = "Bearer " + token;
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger dbOperationCompletedCount = new AtomicInteger(0);
        AtomicInteger totalCount = new AtomicInteger(questions.size());
        
        for (QuestionsListResponse.QuestionData question : questions) {
            long questionId = question.getId();
            
            // 使用默认分页参数同步消息
            apiService.getMessages(authHeader, questionId, 1, 50).enqueue(new Callback<MessagesListResponse>() {
                @Override
                public void onResponse(Call<MessagesListResponse> call, Response<MessagesListResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        List<MessagesListResponse.MessageData> serverMessages = response.body().getMessages();
                        
                        executor.execute(() -> {
                            try {
                                // 获取服务器消息ID集合
                                Set<Long> serverMessageIds = new HashSet<>();
                                for (MessagesListResponse.MessageData serverMessage : serverMessages) {
                                    serverMessageIds.add(serverMessage.getId());
                                    
                                    // 插入或更新消息到本地数据库
                                    MessageEntity entity = new MessageEntity();
                                    entity.setId(serverMessage.getId());
                                    entity.setQuestionId(serverMessage.getQuestionId());
                                    entity.setSenderId(serverMessage.getSenderId());
                                    entity.setContent(serverMessage.getContent());
                                    entity.setMessageType(serverMessage.getMessageType());
                                    entity.setCreatedAt(serverMessage.getCreatedAt());
                                    entity.setRead(serverMessage.isRead()); // 保留已读状态
                                    entity.setSendStatus("sent"); // 从服务器同步的消息都是已发送状态
                                    
                                    messageDao.insert(entity);
                                }
                                
                                // 删除本地存在但服务器不存在的消息
                                List<MessageEntity> localMessages = messageDao.getMessagesByQuestionIdSync(questionId);
                                if (localMessages != null) {
                                    for (MessageEntity localMessage : localMessages) {
                                        if (!serverMessageIds.contains(localMessage.getId())) {
                                            messageDao.deleteMessage(localMessage.getId());
                                            Log.d(TAG, "Deleted message " + localMessage.getId() + " (not on server)");
                                        }
                                    }
                                }
                                
                                Log.d(TAG, "Synced " + serverMessages.size() + " messages for question " + questionId);
                            } catch (Exception e) {
                                Log.e(TAG, "Error syncing messages for question " + questionId, e);
                            } finally {
                                // 数据库操作完成后，检查是否所有操作都已完成
                                int dbCompleted = dbOperationCompletedCount.incrementAndGet();
                                if (dbCompleted >= totalCount.get()) {
                                    Log.d(TAG, "All messages synced and saved to database successfully");
                                    if (callback != null) {
                                        callback.onSuccess(questions.size());
                                        callback.onPageLoaded(hasMore);
                                    }
                                }
                            }
                        });
                    } else {
                        // API响应失败，但仍需要计数
                        int dbCompleted = dbOperationCompletedCount.incrementAndGet();
                        if (dbCompleted >= totalCount.get()) {
                            Log.d(TAG, "All messages processing completed");
                            if (callback != null) {
                                callback.onSuccess(questions.size());
                                callback.onPageLoaded(hasMore);
                            }
                        }
                    }
                    
                    // 标记API请求完成
                    completedCount.incrementAndGet();
                }
                
                @Override
                public void onFailure(Call<MessagesListResponse> call, Throwable t) {
                    Log.e(TAG, "Failed to sync messages for question " + questionId, t);
                    
                    // 即使某个问题的消息同步失败，继续处理其他问题
                    int completed = completedCount.incrementAndGet();
                    int dbCompleted = dbOperationCompletedCount.incrementAndGet();
                    if (dbCompleted >= totalCount.get()) {
                        if (callback != null) {
                            callback.onSuccess(questions.size());
                            callback.onPageLoaded(hasMore);
                        }
                    }
                }
            });
        }
    }
    
    /**
     * 同步回调接口
     */
    public interface SyncCallback {
        void onSuccess(int syncedCount);
        void onError(String errorMessage);
        void onPageLoaded(boolean hasMore);
    }
}

