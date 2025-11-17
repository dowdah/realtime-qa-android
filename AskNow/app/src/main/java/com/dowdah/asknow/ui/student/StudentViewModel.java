package com.dowdah.asknow.ui.student;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.dowdah.asknow.R;
import com.dowdah.asknow.constants.AppConstants;
import com.dowdah.asknow.constants.WebSocketMessageType;
import com.dowdah.asknow.data.api.ApiService;
import com.dowdah.asknow.data.local.dao.QuestionDao;
import com.dowdah.asknow.data.local.entity.QuestionEntity;
import com.dowdah.asknow.data.model.QuestionRequest;
import com.dowdah.asknow.data.model.QuestionResponse;
import com.dowdah.asknow.data.repository.QuestionRepository;
import com.dowdah.asknow.ui.question.BaseQuestionListViewModel;
import com.dowdah.asknow.utils.SharedPreferencesManager;
import com.dowdah.asknow.utils.WebSocketManager;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * StudentViewModel - 学生端问题列表ViewModel
 * 
 * 继承自 BaseQuestionListViewModel，复用了：
 * - 分页加载逻辑
 * - 下拉刷新逻辑
 * - WebSocket 消息监听
 * 
 * 学生端特有功能：
 * - 创建新问题
 * - 查看自己提出的问题
 */
@HiltViewModel
public class StudentViewModel extends BaseQuestionListViewModel {
    private static final String TAG = "StudentViewModel";
    
    private final ApiService apiService;
    
    @Inject
    public StudentViewModel(
        @NonNull Application application,
        ApiService apiService,
        QuestionDao questionDao,
        SharedPreferencesManager prefsManager,
        QuestionRepository questionRepository,
        WebSocketManager webSocketManager
    ) {
        super(
            application,
            questionDao,
            prefsManager,
            questionRepository,
            webSocketManager,
            AppConstants.ROLE_STUDENT
        );
        this.apiService = apiService;
    }
    
    @Override
    protected String getWebSocketMessageType() {
        // 学生端监听新回答消息（向后兼容）
        return WebSocketMessageType.NEW_ANSWER;
    }
    
    @Override
    public LiveData<List<QuestionEntity>> getQuestions() {
        long userId = prefsManager.getUserId();
        return questionDao.getQuestionsByUserId(userId);
    }
    
    /**
     * 创建新问题（学生端特有功能）
     * 
     * @param content 问题内容
     * @param imagePaths 图片路径列表
     */
    public void createQuestion(String content, List<String> imagePaths) {
        String token = "Bearer " + prefsManager.getToken();
        QuestionRequest request = new QuestionRequest(content, imagePaths);
        
        apiService.createQuestion(token, request).enqueue(new Callback<QuestionResponse>() {
            @Override
            public void onResponse(Call<QuestionResponse> call, Response<QuestionResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    QuestionResponse.QuestionData data = response.body().getQuestion();
                    
                    // Save to local database
                    executeInBackground(() -> {
                        // 将图片路径列表转换为 JSON 字符串
                        String imagePathsJson = null;
                        if (data.getImagePaths() != null && !data.getImagePaths().isEmpty()) {
                            imagePathsJson = new com.google.gson.Gson().toJson(data.getImagePaths());
                        }
                        
                        QuestionEntity entity = new QuestionEntity(
                            data.getUserId(),
                            null, // tutorId
                            data.getContent(),
                            imagePathsJson,
                            data.getStatus(),
                            data.getCreatedAt(),
                            data.getCreatedAt() // updatedAt
                        );
                        entity.setId(data.getId());
                        questionDao.insert(entity);
                    });
                    
                    // 注意：不需要再通过 WebSocket 发送
                    // HTTP API 后端已经处理了 WebSocket 广播
                    
                    Log.d(TAG, "Question created successfully");
                } else {
                    setError(getApplication().getString(R.string.failed_to_create_question));
                }
            }
            
            @Override
            public void onFailure(Call<QuestionResponse> call, Throwable t) {
                Log.e(TAG, "Error creating question", t);
                setError(getApplication().getString(R.string.network_error, t.getMessage()));
            }
        });
    }
}

