package com.dowdah.asknow.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * 错误处理工具类
 * 统一处理应用中的各种异常，提供用户友好的错误信息
 * 
 * 功能：
 * - 网络异常处理（超时、无网络、连接失败等）
 * - 数据库异常处理
 * - 通用异常处理
 * - 支持null安全检查
 */
public final class ErrorHandler {
    
    // 默认错误消息
    private static final String DEFAULT_ERROR_MESSAGE = "未知错误";
    private static final String DEFAULT_OPERATION_FAILED = "操作失败，请重试";
    
    // 网络错误消息
    private static final String NETWORK_TIMEOUT = "网络连接超时，请检查网络后重试";
    private static final String NETWORK_UNAVAILABLE = "无法连接到服务器，请检查网络设置";
    private static final String CONNECTION_REFUSED = "服务器拒绝连接，请稍后重试";
    private static final String NETWORK_ERROR_PREFIX = "网络错误: ";
    
    // 数据库错误消息
    private static final String DATABASE_ERROR = "数据库错误，请稍后重试";
    
    // Private constructor to prevent instantiation
    private ErrorHandler() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    /**
     * 获取详细的错误信息（用于网络和数据库错误）
     * 根据异常类型返回用户友好的错误消息
     * 
     * @param error 异常对象，可以为null
     * @return 用户友好的错误信息，永不返回null
     */
    @NonNull
    public static String getDetailedErrorMessage(@Nullable Throwable error) {
        if (error == null) {
            return DEFAULT_ERROR_MESSAGE;
        }
        
        // 网络超时
        if (error instanceof SocketTimeoutException) {
            return NETWORK_TIMEOUT;
        }
        
        // 无网络连接
        if (error instanceof UnknownHostException) {
            return NETWORK_UNAVAILABLE;
        }
        
        // 连接被拒绝
        if (error instanceof ConnectException) {
            return CONNECTION_REFUSED;
        }
        
        // 通用IO错误
        if (error instanceof IOException) {
            String message = error.getMessage();
            return NETWORK_ERROR_PREFIX + (message != null && !message.isEmpty() ? message : "未知IO错误");
        }
        
        // 数据库错误
        if (error instanceof android.database.sqlite.SQLiteException) {
            return DATABASE_ERROR;
        }
        
        // 其他错误
        String message = error.getMessage();
        return message != null && !message.isEmpty() ? 
            "操作失败: " + message : DEFAULT_OPERATION_FAILED;
    }
    
    /**
     * 获取简短的错误信息（仅返回异常消息）
     * 如果异常消息为空，返回默认消息
     * 
     * @param error 异常对象
     * @param defaultMessage 默认错误消息
     * @return 错误信息
     */
    @NonNull
    public static String getErrorMessage(@Nullable Throwable error, @NonNull String defaultMessage) {
        if (error == null) {
            return defaultMessage;
        }
        
        String message = error.getMessage();
        return message != null && !message.isEmpty() ? message : defaultMessage;
    }
    
    /**
     * 判断是否为网络相关错误
     * 
     * @param error 异常对象
     * @return true if 是网络错误
     */
    public static boolean isNetworkError(@Nullable Throwable error) {
        if (error == null) {
            return false;
        }
        
        return error instanceof SocketTimeoutException ||
               error instanceof UnknownHostException ||
               error instanceof ConnectException ||
               error instanceof IOException;
    }
    
    /**
     * 判断是否为数据库错误
     * 
     * @param error 异常对象
     * @return true if 是数据库错误
     */
    public static boolean isDatabaseError(@Nullable Throwable error) {
        return error instanceof android.database.sqlite.SQLiteException;
    }
    
    /**
     * 判断是否为超时错误
     * 
     * @param error 异常对象
     * @return true if 是超时错误
     */
    public static boolean isTimeoutError(@Nullable Throwable error) {
        return error instanceof SocketTimeoutException;
    }
    
    /**
     * 获取异常的根本原因
     * 递归查找异常链中的根本原因
     * 
     * @param error 异常对象
     * @return 根本原因异常
     */
    @Nullable
    public static Throwable getRootCause(@Nullable Throwable error) {
        if (error == null) {
            return null;
        }
        
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        
        return cause;
    }
    
    /**
     * 获取详细的错误信息（包含根本原因）
     * 
     * @param error 异常对象
     * @return 详细错误信息
     */
    @NonNull
    public static String getDetailedErrorMessageWithCause(@Nullable Throwable error) {
        Throwable rootCause = getRootCause(error);
        return getDetailedErrorMessage(rootCause != null ? rootCause : error);
    }
}

