package com.dowdah.asknow.utils;

import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

/**
 * ErrorHandler 单元测试
 * 
 * 测试功能：
 * - 各种异常类型的错误消息处理
 * - null处理
 * - 异常链处理
 */
public class ErrorHandlerTest {
    
    /**
     * 测试null异常
     */
    @Test
    public void testGetDetailedErrorMessage_NullError() {
        String message = ErrorHandler.getDetailedErrorMessage(null);
        assertNotNull(message);
        assertEquals("未知错误", message);
    }
    
    /**
     * 测试SocketTimeoutException
     */
    @Test
    public void testGetDetailedErrorMessage_SocketTimeoutException() {
        Throwable error = new SocketTimeoutException("Connection timed out");
        String message = ErrorHandler.getDetailedErrorMessage(error);
        
        assertNotNull(message);
        assertTrue(message.contains("超时"));
    }
    
    /**
     * 测试UnknownHostException
     */
    @Test
    public void testGetDetailedErrorMessage_UnknownHostException() {
        Throwable error = new UnknownHostException("Unable to resolve host");
        String message = ErrorHandler.getDetailedErrorMessage(error);
        
        assertNotNull(message);
        assertTrue(message.contains("无法连接") || message.contains("服务器"));
    }
    
    /**
     * 测试ConnectException
     */
    @Test
    public void testGetDetailedErrorMessage_ConnectException() {
        Throwable error = new ConnectException("Connection refused");
        String message = ErrorHandler.getDetailedErrorMessage(error);
        
        assertNotNull(message);
        assertTrue(message.contains("拒绝") || message.contains("连接"));
    }
    
    /**
     * 测试IOException
     */
    @Test
    public void testGetDetailedErrorMessage_IOException() {
        Throwable error = new IOException("Network is unreachable");
        String message = ErrorHandler.getDetailedErrorMessage(error);
        
        assertNotNull(message);
        assertTrue(message.contains("网络错误"));
    }
    
    /**
     * 测试SQLiteException
     */
    @Test
    public void testGetDetailedErrorMessage_SQLiteException() {
        Throwable error = new android.database.sqlite.SQLiteException("Database locked");
        String message = ErrorHandler.getDetailedErrorMessage(error);
        
        assertNotNull(message);
        assertTrue(message.contains("数据库"));
    }
    
    /**
     * 测试通用异常
     */
    @Test
    public void testGetDetailedErrorMessage_GenericException() {
        Throwable error = new RuntimeException("Something went wrong");
        String message = ErrorHandler.getDetailedErrorMessage(error);
        
        assertNotNull(message);
        assertTrue(message.contains("操作失败"));
    }
    
    /**
     * 测试空消息的异常
     */
    @Test
    public void testGetDetailedErrorMessage_EmptyMessage() {
        Throwable error = new RuntimeException();
        String message = ErrorHandler.getDetailedErrorMessage(error);
        
        assertNotNull(message);
        assertFalse(message.isEmpty());
    }
    
    /**
     * 测试isNetworkError
     */
    @Test
    public void testIsNetworkError() {
        assertTrue(ErrorHandler.isNetworkError(new SocketTimeoutException()));
        assertTrue(ErrorHandler.isNetworkError(new UnknownHostException()));
        assertTrue(ErrorHandler.isNetworkError(new ConnectException()));
        assertTrue(ErrorHandler.isNetworkError(new IOException()));
        
        assertFalse(ErrorHandler.isNetworkError(new RuntimeException()));
        assertFalse(ErrorHandler.isNetworkError(null));
    }
    
    /**
     * 测试isDatabaseError
     */
    @Test
    public void testIsDatabaseError() {
        assertTrue(ErrorHandler.isDatabaseError(
            new android.database.sqlite.SQLiteException("Error")));
        
        assertFalse(ErrorHandler.isDatabaseError(new IOException()));
        assertFalse(ErrorHandler.isDatabaseError(null));
    }
    
    /**
     * 测试isTimeoutError
     */
    @Test
    public void testIsTimeoutError() {
        assertTrue(ErrorHandler.isTimeoutError(new SocketTimeoutException()));
        
        assertFalse(ErrorHandler.isTimeoutError(new IOException()));
        assertFalse(ErrorHandler.isTimeoutError(null));
    }
    
    /**
     * 测试getRootCause - 无cause
     */
    @Test
    public void testGetRootCause_NoCause() {
        Throwable error = new RuntimeException("Error");
        Throwable rootCause = ErrorHandler.getRootCause(error);
        
        assertSame(error, rootCause);
    }
    
    /**
     * 测试getRootCause - 有cause
     */
    @Test
    public void testGetRootCause_WithCause() {
        Throwable rootError = new IOException("Root cause");
        Throwable wrapperError = new RuntimeException("Wrapper", rootError);
        
        Throwable rootCause = ErrorHandler.getRootCause(wrapperError);
        
        assertSame(rootError, rootCause);
    }
    
    /**
     * 测试getRootCause - 多层嵌套
     */
    @Test
    public void testGetRootCause_MultipleNesting() {
        Throwable level3 = new IOException("Level 3");
        Throwable level2 = new RuntimeException("Level 2", level3);
        Throwable level1 = new Exception("Level 1", level2);
        
        Throwable rootCause = ErrorHandler.getRootCause(level1);
        
        assertSame(level3, rootCause);
    }
    
    /**
     * 测试getRootCause - null
     */
    @Test
    public void testGetRootCause_Null() {
        Throwable rootCause = ErrorHandler.getRootCause(null);
        assertNull(rootCause);
    }
    
    /**
     * 测试getErrorMessage带默认消息
     */
    @Test
    public void testGetErrorMessage_WithDefault() {
        String defaultMsg = "Default message";
        
        // null异常
        String message1 = ErrorHandler.getErrorMessage(null, defaultMsg);
        assertEquals(defaultMsg, message1);
        
        // 有消息的异常
        String message2 = ErrorHandler.getErrorMessage(
            new RuntimeException("Custom error"), 
            defaultMsg
        );
        assertEquals("Custom error", message2);
        
        // 空消息的异常
        String message3 = ErrorHandler.getErrorMessage(
            new RuntimeException(), 
            defaultMsg
        );
        assertEquals(defaultMsg, message3);
    }
    
    /**
     * 测试getDetailedErrorMessageWithCause
     */
    @Test
    public void testGetDetailedErrorMessageWithCause() {
        Throwable rootError = new SocketTimeoutException("Timeout");
        Throwable wrapperError = new RuntimeException("Wrapper", rootError);
        
        String message = ErrorHandler.getDetailedErrorMessageWithCause(wrapperError);
        
        assertNotNull(message);
        assertTrue(message.contains("超时"));
    }
}

