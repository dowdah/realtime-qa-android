package com.dowdah.asknow.base;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dowdah.asknow.utils.ErrorHandler;
import com.dowdah.asknow.utils.ThreadUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BaseViewModel - 提供公共的生命周期管理和线程池管理
 * 所有ViewModel应该继承此类
 * 
 * 提供的通用功能：
 * - 线程池管理（executor）
 * - 错误消息管理（errorMessage）
 * - 加载状态管理（loading）
 * - 分页状态管理（可选，通过 enablePagination() 启用）
 * - 资源清理（cleanup()）
 */
public abstract class BaseViewModel extends AndroidViewModel {
    
    protected final ExecutorService executor;
    protected final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    protected final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    
    // 分页相关字段（子类可选使用）
    protected int currentPage = 1;
    protected final MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>(false);
    protected final MutableLiveData<Boolean> hasMoreData = new MutableLiveData<>(true);
    protected final MutableLiveData<Boolean> isSyncing = new MutableLiveData<>(false);
    
    public BaseViewModel(@NonNull Application application) {
        super(application);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 获取错误消息LiveData
     * 
     * @return 错误消息
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * 获取加载状态LiveData
     * 
     * @return 加载状态
     */
    public LiveData<Boolean> getLoading() {
        return loading;
    }
    
    /**
     * 获取同步状态LiveData
     * 
     * @return 同步状态
     */
    public LiveData<Boolean> getIsSyncing() {
        return isSyncing;
    }
    
    /**
     * 获取加载更多状态LiveData
     * 
     * @return 加载更多状态
     */
    public LiveData<Boolean> getIsLoadingMore() {
        return isLoadingMore;
    }
    
    /**
     * 获取是否还有更多数据LiveData
     * 
     * @return 是否还有更多数据
     */
    public LiveData<Boolean> getHasMoreData() {
        return hasMoreData;
    }
    
    /**
     * 设置错误消息
     * 
     * @param error 错误消息
     */
    protected void setError(String error) {
        errorMessage.postValue(error);
    }
    
    /**
     * 设置加载状态
     * 
     * @param isLoading 是否正在加载
     */
    protected void setLoading(boolean isLoading) {
        loading.postValue(isLoading);
    }
    
    /**
     * 重置分页状态
     */
    protected void resetPaginationState() {
        currentPage = 1;
        hasMoreData.postValue(true);
    }
    
    /**
     * 增加页码
     */
    protected void incrementPage() {
        currentPage++;
    }
    
    /**
     * 减少页码（用于加载失败时恢复）
     */
    protected void decrementPage() {
        if (currentPage > 1) {
            currentPage--;
        }
    }
    
    /**
     * 获取当前页码
     * 
     * @return 当前页码
     */
    protected int getCurrentPage() {
        return currentPage;
    }
    
    /**
     * 在后台线程执行任务
     * 
     * @param runnable 要执行的任务
     */
    protected void executeInBackground(Runnable runnable) {
        if (runnable != null) {
            executor.execute(runnable);
        }
    }
    
    /**
     * 在IO线程池执行任务（适用于网络请求、文件读写等）
     * 
     * @param runnable 要执行的任务
     */
    protected void executeOnIO(Runnable runnable) {
        ThreadUtils.executeOnIO(runnable);
    }
    
    /**
     * 在主线程执行任务
     * 
     * @param runnable 要执行的任务
     */
    protected void executeOnMain(Runnable runnable) {
        ThreadUtils.executeOnMain(runnable);
    }
    
    /**
     * 处理异常并设置错误消息
     * 
     * @param throwable 异常
     * @param defaultMessage 默认错误消息
     */
    protected void handleError(Throwable throwable, String defaultMessage) {
        String message = throwable.getMessage();
        if (message == null || message.isEmpty()) {
            message = defaultMessage;
        }
        setError(message);
    }
    
    /**
     * 获取详细的错误信息（用于网络和数据库错误）
     * 委托给ErrorHandler统一处理
     * 
     * @param error 异常对象
     * @return 用户友好的错误信息
     */
    protected String getDetailedErrorMessage(Throwable error) {
        return ErrorHandler.getDetailedErrorMessage(error);
    }
    
    /**
     * 清理资源
     * 子类可以重写此方法来添加额外的清理逻辑
     */
    protected void cleanup() {
        // 子类可以重写此方法
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        
        // 调用cleanup方法
        cleanup();
        
        // 关闭线程池
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}

