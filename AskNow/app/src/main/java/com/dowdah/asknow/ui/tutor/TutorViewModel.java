package com.dowdah.asknow.ui.tutor;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.dowdah.asknow.constants.AppConstants;
import com.dowdah.asknow.constants.QuestionStatus;
import com.dowdah.asknow.constants.WebSocketMessageType;
import com.dowdah.asknow.data.local.dao.QuestionDao;
import com.dowdah.asknow.data.local.entity.QuestionEntity;
import com.dowdah.asknow.data.repository.QuestionRepository;
import com.dowdah.asknow.ui.question.BaseQuestionListViewModel;
import com.dowdah.asknow.utils.SharedPreferencesManager;
import com.dowdah.asknow.utils.WebSocketManager;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * TutorViewModel - 教师端问题列表ViewModel
 * 
 * 继承自 BaseQuestionListViewModel，复用了：
 * - 分页加载逻辑
 * - 下拉刷新逻辑
 * - WebSocket 消息监听
 * 
 * 教师端特有功能：
 * - 查看待接取的问题（PENDING）
 * - 查看进行中的问题（IN_PROGRESS）
 * - 查看已完成的问题（CLOSED）
 */
@HiltViewModel
public class TutorViewModel extends BaseQuestionListViewModel {
    private static final String TAG = "TutorViewModel";
    
    @Inject
    public TutorViewModel(
        @NonNull Application application,
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
            AppConstants.ROLE_TUTOR
        );
    }
    
    @Override
    protected String getWebSocketMessageType() {
        // 教师端监听新问题消息
        return WebSocketMessageType.NEW_QUESTION;
    }
    
    @Override
    public LiveData<List<QuestionEntity>> getQuestions() {
        // 默认返回待接取的问题列表
        return getPendingQuestions();
    }
    
    /**
     * 获取待接取的问题列表（PENDING状态）
     * 
     * @return 待接取问题的LiveData
     */
    public LiveData<List<QuestionEntity>> getPendingQuestions() {
        return questionDao.getQuestionsByStatus(QuestionStatus.PENDING);
    }
    
    /**
     * 获取进行中的问题列表（IN_PROGRESS状态）
     * 只显示当前教师正在回答的问题
     * 
     * @return 进行中问题的LiveData
     */
    public LiveData<List<QuestionEntity>> getInProgressQuestions() {
        long tutorId = prefsManager.getUserId();
        return questionDao.getQuestionsByTutorAndStatus(tutorId, QuestionStatus.IN_PROGRESS);
    }
    
    /**
     * 获取已完成的问题列表（CLOSED状态）
     * 只显示当前教师已完成的问题
     * 
     * @return 已完成问题的LiveData
     */
    public LiveData<List<QuestionEntity>> getClosedQuestions() {
        long tutorId = prefsManager.getUserId();
        return questionDao.getQuestionsByTutorAndStatus(tutorId, QuestionStatus.CLOSED);
    }
}

