package com.dowdah.asknow.constants;

/**
 * WebSocket消息类型常量
 */
public final class WebSocketMessageType {
    
    /**
     * 确认消息（服务器确认收到消息）
     */
    public static final String ACK = "ACK";
    
    /**
     * 聊天消息
     */
    public static final String CHAT_MESSAGE = "CHAT_MESSAGE";
    
    /**
     * 问题更新消息（统一的问题状态更新）
     */
    public static final String QUESTION_UPDATED = "QUESTION_UPDATED";
    
    /**
     * 问题被接受消息（向后兼容）
     */
    public static final String QUESTION_ACCEPTED = "QUESTION_ACCEPTED";
    
    /**
     * 问题被关闭消息（向后兼容）
     */
    public static final String QUESTION_CLOSED = "QUESTION_CLOSED";
    
    /**
     * 新问题消息（老师端接收）
     */
    public static final String NEW_QUESTION = "NEW_QUESTION";
    
    /**
     * 新回答消息（学生端接收，向后兼容）
     */
    public static final String NEW_ANSWER = "NEW_ANSWER";
    
    // Private constructor to prevent instantiation
    private WebSocketMessageType() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}

