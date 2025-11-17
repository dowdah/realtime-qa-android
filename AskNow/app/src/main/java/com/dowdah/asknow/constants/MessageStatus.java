package com.dowdah.asknow.constants;

/**
 * 消息发送状态常量
 */
public final class MessageStatus {
    
    /**
     * 消息正在发送中（乐观更新状态）
     */
    public static final String PENDING = "pending";
    
    /**
     * 消息已成功发送
     */
    public static final String SENT = "sent";
    
    /**
     * 消息发送失败
     */
    public static final String FAILED = "failed";
    
    // Private constructor to prevent instantiation
    private MessageStatus() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}

