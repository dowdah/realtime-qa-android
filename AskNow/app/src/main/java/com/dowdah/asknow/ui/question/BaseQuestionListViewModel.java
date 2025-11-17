package com.dowdah.asknow.ui.question;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.dowdah.asknow.base.BaseViewModel;
import com.dowdah.asknow.constants.AppConstants;
import com.dowdah.asknow.constants.WebSocketMessageType;
import com.dowdah.asknow.data.local.dao.QuestionDao;
import com.dowdah.asknow.data.local.entity.QuestionEntity;
import com.dowdah.asknow.data.model.WebSocketMessage;
import com.dowdah.asknow.data.repository.QuestionRepository;
import com.dowdah.asknow.utils.SharedPreferencesManager;
import com.dowdah.asknow.utils.WebSocketManager;

import java.util.List;

/**
 * BaseQuestionListViewModel - 问题列表的基础ViewModel
 * 
 * 合并了 StudentViewModel 和 TutorViewModel 的公共逻辑：
 * - 问题列表查询
 * - 分页加载
 * - 下拉刷新
 * - WebSocket 消息监听
 * 
 * 子类需要实现：
 * - getQuestionsLiveData(): 返回具体的问题列表 LiveData
 * - getWebSocketMessageType(): 返回需要监听的 WebSocket 消息类型
 */
public abstract class BaseQuestionListViewModel extends BaseViewModel {
    private static final String TAG = "BaseQuestionListVM";
    
    protected final QuestionDao questionDao;
    protected final SharedPreferencesManager prefsManager;
    protected final QuestionRepository questionRepository;
    protected final WebSocketManager webSocketManager;
    
    protected final String role;
    protected final MutableLiveData<WebSocketMessage> newWebSocketMessage = new MutableLiveData<>();
    
    // WebSocket message observer for cleanup
    protected final Observer<WebSocketMessage> webSocketMessageObserver;
    
    /**
     * 构造函数
     * 
     * @param application Application实例
     * @param questionDao 问题数据访问对象
     * @param prefsManager SharedPreferences管理器
     * @param questionRepository 问题仓库
     * @param webSocketManager WebSocket管理器
     * @param role 用户角色 (student/tutor)
     */
    public BaseQuestionListViewModel(
        @NonNull Application application,
        QuestionDao questionDao,
        SharedPreferencesManager prefsManager,
        QuestionRepository questionRepository,
        WebSocketManager webSocketManager,
        String role
    ) {
        super(application);
        this.questionDao = questionDao;
        this.prefsManager = prefsManager;
        this.questionRepository = questionRepository;
        this.webSocketManager = webSocketManager;
        this.role = role;
        
        // 创建 WebSocket 消息观察者
        this.webSocketMessageObserver = message -> {
            if (message != null) {
                handleWebSocketMessage(message);
            }
        };
        
        // 开始观察 WebSocket 消息
        observeWebSocketMessages();
    }
    
    /**
     * 观察 WebSocket 消息
     */
    private void observeWebSocketMessages() {
        webSocketManager.getIncomingMessage().observeForever(webSocketMessageObserver);
    }
    
    /**
     * 处理 WebSocket 消息（子类可以重写以添加特定处理）
     * 
     * @param message WebSocket消息
     */
    protected void handleWebSocketMessage(WebSocketMessage message) {
        String type = message.getType();
        String expectedType = getWebSocketMessageType();
        
        if (expectedType != null && expectedType.equals(type)) {
            newWebSocketMessage.postValue(message);
        }
    }
    
    /**
     * 获取需要监听的 WebSocket 消息类型（子类实现）
     * 
     * @return WebSocket消息类型，如果返回null则不监听特定类型
     */
    protected abstract String getWebSocketMessageType();
    
    /**
     * 获取问题列表 LiveData（子类实现）
     * 
     * @return 问题列表的LiveData
     */
    public abstract LiveData<List<QuestionEntity>> getQuestions();
    
    /**
     * 获取新的 WebSocket 消息
     * 
     * @return WebSocket消息的LiveData
     */
    public LiveData<WebSocketMessage> getNewWebSocketMessage() {
        return newWebSocketMessage;
    }
    
    /**
     * 获取 WebSocket 连接状态
     * 
     * @return 连接状态的LiveData
     */
    public LiveData<Boolean> isConnected() {
        return webSocketManager.isConnected();
    }
    
    /**
     * 从服务器同步问题到本地（下拉刷新，重置为第一页）
     */
    public void syncQuestionsFromServer() {
        if (Boolean.TRUE.equals(isSyncing.getValue())) {
            Log.d(TAG, "Sync already in progress, skipping");
            return;
        }
        
        resetPaginationState();
        isSyncing.postValue(true);
        
        String token = prefsManager.getToken();
        long userId = prefsManager.getUserId();
        
        questionRepository.syncQuestionsFromServer(
            token,
            userId,
            role,
            AppConstants.DEFAULT_START_PAGE,
            AppConstants.DEFAULT_QUESTIONS_PAGE_SIZE,
            false, // 刷新模式
            new QuestionRepository.SyncCallback() {
                @Override
                public void onSuccess(int syncedCount) {
                    isSyncing.postValue(false);
                    Log.d(TAG, "Sync completed: " + syncedCount + " questions");
                }
                
                @Override
                public void onError(String errorMsg) {
                    isSyncing.postValue(false);
                    setError(errorMsg);
                    Log.e(TAG, "Sync failed: " + errorMsg);
                }
                
                @Override
                public void onPageLoaded(boolean hasMore) {
                    hasMoreData.postValue(hasMore);
                }
            }
        );
    }
    
    /**
     * 加载更多问题（滚动加载）
     */
    public void loadMoreQuestions() {
        if (Boolean.TRUE.equals(isLoadingMore.getValue()) || 
            Boolean.FALSE.equals(hasMoreData.getValue())) {
            Log.d(TAG, "Already loading or no more data");
            return;
        }
        
        incrementPage();
        isLoadingMore.postValue(true);
        
        String token = prefsManager.getToken();
        long userId = prefsManager.getUserId();
        
        questionRepository.syncQuestionsFromServer(
            token,
            userId,
            role,
            getCurrentPage(),
            AppConstants.DEFAULT_QUESTIONS_PAGE_SIZE,
            true, // 追加模式
            new QuestionRepository.SyncCallback() {
                @Override
                public void onSuccess(int syncedCount) {
                    isLoadingMore.postValue(false);
                    Log.d(TAG, "Load more completed: " + syncedCount + " questions");
                }
                
                @Override
                public void onError(String errorMsg) {
                    isLoadingMore.postValue(false);
                    decrementPage(); // 恢复页码
                    setError(errorMsg);
                    Log.e(TAG, "Load more failed: " + errorMsg);
                }
                
                @Override
                public void onPageLoaded(boolean hasMore) {
                    hasMoreData.postValue(hasMore);
                }
            }
        );
    }
    
    @Override
    protected void cleanup() {
        super.cleanup();
        
        // 移除 WebSocket 观察者，防止内存泄漏
        if (webSocketMessageObserver != null) {
            webSocketManager.getIncomingMessage().removeObserver(webSocketMessageObserver);
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        
        // 不断开 WebSocket - 应该在应用级别保持连接
        // WebSocket 的生命周期由 WebSocketManager 管理
    }
}

