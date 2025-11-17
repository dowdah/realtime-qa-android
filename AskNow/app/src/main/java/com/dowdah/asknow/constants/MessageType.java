package com.dowdah.asknow.constants;

/**
 * 消息类型常量
 */
public final class MessageType {
    
    /**
     * 文本消息
     */
    public static final String TEXT = "text";
    
    /**
     * 图片消息
     */
    public static final String IMAGE = "image";
    
    // Private constructor to prevent instantiation
    private MessageType() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}

