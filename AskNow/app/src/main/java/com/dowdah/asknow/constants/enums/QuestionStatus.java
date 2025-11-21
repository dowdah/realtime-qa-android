package com.dowdah.asknow.constants.enums;

/**
 * 问题状态常量
 */
public final class QuestionStatus {
    
    /**
     * 待回答状态（问题刚发布，等待老师接取）
     */
    public static final String PENDING = "pending";
    
    /**
     * 回答中状态（老师已接取，正在回答）
     */
    public static final String IN_PROGRESS = "in_progress";
    
    /**
     * 已关闭状态（问题已完成）
     */
    public static final String CLOSED = "closed";
    
    // Private constructor to prevent instantiation
    private QuestionStatus() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}

