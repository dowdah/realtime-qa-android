package com.dowdah.asknow.data.repository;

import com.dowdah.asknow.constants.QuestionStatus;
import com.dowdah.asknow.data.api.ApiService;
import com.dowdah.asknow.data.local.dao.MessageDao;
import com.dowdah.asknow.data.local.dao.QuestionDao;
import com.dowdah.asknow.data.local.entity.QuestionEntity;
import com.dowdah.asknow.data.model.Pagination;
import com.dowdah.asknow.data.model.QuestionsListResponse;
import com.google.gson.Gson;

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
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * QuestionRepository 单元测试
 * 
 * 测试功能：
 * - 同步问题从服务器到本地
 * - 分页加载
 * - 数据清理（删除本地不存在于服务器的数据）
 */
@RunWith(MockitoJUnitRunner.class)
public class QuestionRepositoryTest {
    
    @Mock
    private ApiService apiService;
    
    @Mock
    private QuestionDao questionDao;
    
    @Mock
    private MessageDao messageDao;
    
    @Mock
    private Call<QuestionsListResponse> questionsCall;
    
    private ExecutorService executor;
    private Gson gson;
    private QuestionRepository repository;
    
    private static final String TEST_TOKEN = "test_token";
    private static final long TEST_USER_ID = 1L;
    private static final String TEST_ROLE_STUDENT = "student";
    private static final String TEST_ROLE_TUTOR = "tutor";
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        executor = Executors.newSingleThreadExecutor();
        gson = new Gson();
        
        repository = new QuestionRepository(
            apiService,
            questionDao,
            messageDao,
            executor,
            gson
        );
    }
    
    /**
     * 测试同步问题成功 - 学生端
     */
    @Test
    public void testSyncQuestions_StudentSuccess() throws InterruptedException {
        // Arrange
        QuestionsListResponse.QuestionData question1 = new QuestionsListResponse.QuestionData();
        question1.setId(1L);
        question1.setUserId(TEST_USER_ID);
        question1.setContent("Question 1");
        question1.setStatus(QuestionStatus.PENDING);
        question1.setCreatedAt(System.currentTimeMillis());
        question1.setUpdatedAt(System.currentTimeMillis());
        
        QuestionsListResponse.QuestionData question2 = new QuestionsListResponse.QuestionData();
        question2.setId(2L);
        question2.setUserId(TEST_USER_ID);
        question2.setContent("Question 2");
        question2.setStatus(QuestionStatus.IN_PROGRESS);
        question2.setCreatedAt(System.currentTimeMillis());
        question2.setUpdatedAt(System.currentTimeMillis());
        
        List<QuestionsListResponse.QuestionData> questions = Arrays.asList(question1, question2);
        
        Pagination pagination = new Pagination();
        pagination.setTotalPages(1);
        pagination.setPage(1);
        pagination.setTotal(2);
        
        QuestionsListResponse response = new QuestionsListResponse();
        response.setSuccess(true);
        response.setQuestions(questions);
        response.setPagination(pagination);
        
        when(apiService.getQuestions(anyString(), isNull(), anyInt(), anyInt()))
            .thenReturn(questionsCall);
        
        ArgumentCaptor<Callback<QuestionsListResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        QuestionRepository.SyncCallback syncCallback = mock(QuestionRepository.SyncCallback.class);
        
        // Act
        repository.syncQuestionsFromServer(TEST_TOKEN, TEST_USER_ID, TEST_ROLE_STUDENT, syncCallback);
        
        verify(questionsCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(questionsCall, Response.success(response));
        
        // 等待异步操作
        Thread.sleep(500);
        
        // Assert - 应该保存问题到本地数据库
        verify(questionDao, atLeast(2)).insert(any(QuestionEntity.class));
    }
    
    /**
     * 测试同步问题失败 - 网络错误
     */
    @Test
    public void testSyncQuestions_NetworkError() {
        // Arrange
        Throwable error = new java.io.IOException("Network error");
        
        when(apiService.getQuestions(anyString(), isNull(), anyInt(), anyInt()))
            .thenReturn(questionsCall);
        
        ArgumentCaptor<Callback<QuestionsListResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        QuestionRepository.SyncCallback syncCallback = mock(QuestionRepository.SyncCallback.class);
        
        // Act
        repository.syncQuestionsFromServer(TEST_TOKEN, TEST_USER_ID, TEST_ROLE_STUDENT, syncCallback);
        
        verify(questionsCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(questionsCall, error);
        
        // Assert - 应该调用错误回调
        verify(syncCallback).onError(anyString());
        
        // 不应该保存到数据库
        verify(questionDao, never()).insert(any(QuestionEntity.class));
    }
    
    /**
     * 测试同步问题 - 服务器返回失败
     */
    @Test
    public void testSyncQuestions_ServerError() {
        // Arrange
        QuestionsListResponse response = new QuestionsListResponse();
        response.setSuccess(false);
        
        when(apiService.getQuestions(anyString(), isNull(), anyInt(), anyInt()))
            .thenReturn(questionsCall);
        
        ArgumentCaptor<Callback<QuestionsListResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        QuestionRepository.SyncCallback syncCallback = mock(QuestionRepository.SyncCallback.class);
        
        // Act
        repository.syncQuestionsFromServer(TEST_TOKEN, TEST_USER_ID, TEST_ROLE_STUDENT, syncCallback);
        
        verify(questionsCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(questionsCall, Response.success(response));
        
        // Assert
        verify(syncCallback).onError(anyString());
        verify(questionDao, never()).insert(any(QuestionEntity.class));
    }
    
    /**
     * 测试分页加载 - 有更多数据
     */
    @Test
    public void testSyncQuestions_HasMorePages() throws InterruptedException {
        // Arrange
        QuestionsListResponse.QuestionData question = new QuestionsListResponse.QuestionData();
        question.setId(1L);
        question.setUserId(TEST_USER_ID);
        question.setContent("Question 1");
        question.setStatus(QuestionStatus.PENDING);
        question.setCreatedAt(System.currentTimeMillis());
        question.setUpdatedAt(System.currentTimeMillis());
        
        List<QuestionsListResponse.QuestionData> questions = Arrays.asList(question);
        
        Pagination pagination = new Pagination();
        pagination.setTotalPages(3);
        pagination.setPage(1);
        
        QuestionsListResponse response = new QuestionsListResponse();
        response.setSuccess(true);
        response.setQuestions(questions);
        response.setPagination(pagination);
        
        when(apiService.getQuestions(anyString(), isNull(), anyInt(), anyInt()))
            .thenReturn(questionsCall);
        
        ArgumentCaptor<Callback<QuestionsListResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        QuestionRepository.SyncCallback syncCallback = mock(QuestionRepository.SyncCallback.class);
        
        // Act
        repository.syncQuestionsFromServer(
            TEST_TOKEN, 
            TEST_USER_ID, 
            TEST_ROLE_STUDENT, 
            1, 
            20, 
            false, 
            syncCallback
        );
        
        verify(questionsCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(questionsCall, Response.success(response));
        
        // 等待异步操作
        Thread.sleep(500);
        
        // Assert - 应该通知有更多数据
        verify(syncCallback).onPageLoaded(true);
    }
    
    /**
     * 测试同步空列表
     */
    @Test
    public void testSyncQuestions_EmptyList() throws InterruptedException {
        // Arrange
        List<QuestionsListResponse.QuestionData> questions = Arrays.asList();
        
        Pagination pagination = new Pagination();
        pagination.setTotalPages(0);
        pagination.setPage(1);
        
        QuestionsListResponse response = new QuestionsListResponse();
        response.setSuccess(true);
        response.setQuestions(questions);
        response.setPagination(pagination);
        
        when(apiService.getQuestions(anyString(), isNull(), anyInt(), anyInt()))
            .thenReturn(questionsCall);
        
        ArgumentCaptor<Callback<QuestionsListResponse>> callbackCaptor = 
            ArgumentCaptor.forClass(Callback.class);
        
        QuestionRepository.SyncCallback syncCallback = mock(QuestionRepository.SyncCallback.class);
        
        // Act
        repository.syncQuestionsFromServer(TEST_TOKEN, TEST_USER_ID, TEST_ROLE_STUDENT, syncCallback);
        
        verify(questionsCall).enqueue(callbackCaptor.capture());
        callbackCaptor.getValue().onResponse(questionsCall, Response.success(response));
        
        // 等待异步操作
        Thread.sleep(300);
        
        // Assert
        verify(syncCallback).onSuccess(0);
        verify(syncCallback).onPageLoaded(false);
        
        // 不应该插入任何问题
        verify(questionDao, never()).insert(any(QuestionEntity.class));
    }
}

