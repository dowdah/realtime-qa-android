package com.dowdah.asknow.utils;

import android.text.TextUtils;
import android.util.Patterns;

/**
 * 输入验证工具类
 */
public final class ValidationUtils {
    
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 20;
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 20;
    
    // Private constructor to prevent instantiation
    private ValidationUtils() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    /**
     * 检查字符串是否非空
     * 
     * @param text 文本
     * @return true if not null or empty
     */
    public static boolean isNotNullOrEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }
    
    /**
     * 检查用户名是否有效（简化版）
     * 
     * @param username 用户名
     * @return true if valid
     */
    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        return username.length() >= MIN_USERNAME_LENGTH && 
               username.length() <= MAX_USERNAME_LENGTH &&
               username.matches("^[a-zA-Z0-9_]+$");
    }
    
    /**
     * 检查密码是否有效（简化版）
     * 
     * @param password 密码
     * @return true if valid
     */
    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.length() >= MIN_PASSWORD_LENGTH && 
               password.length() <= MAX_PASSWORD_LENGTH;
    }
    
    /**
     * 验证用户名是否有效
     * 规则：3-20个字符，只能包含字母、数字和下划线
     * 
     * @param username 用户名
     * @return 验证结果
     */
    public static ValidationResult validateUsername(String username) {
        if (TextUtils.isEmpty(username)) {
            return new ValidationResult(false, "用户名不能为空");
        }
        
        if (username.length() < MIN_USERNAME_LENGTH) {
            return new ValidationResult(false, "用户名至少需要" + MIN_USERNAME_LENGTH + "个字符");
        }
        
        if (username.length() > MAX_USERNAME_LENGTH) {
            return new ValidationResult(false, "用户名最多" + MAX_USERNAME_LENGTH + "个字符");
        }
        
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return new ValidationResult(false, "用户名只能包含字母、数字和下划线");
        }
        
        return new ValidationResult(true, "");
    }
    
    /**
     * 验证密码是否有效
     * 规则：6-20个字符
     * 
     * @param password 密码
     * @return 验证结果
     */
    public static ValidationResult validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            return new ValidationResult(false, "密码不能为空");
        }
        
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return new ValidationResult(false, "密码至少需要" + MIN_PASSWORD_LENGTH + "个字符");
        }
        
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return new ValidationResult(false, "密码最多" + MAX_PASSWORD_LENGTH + "个字符");
        }
        
        return new ValidationResult(true, "");
    }
    
    /**
     * 验证邮箱是否有效
     * 
     * @param email 邮箱地址
     * @return 验证结果
     */
    public static ValidationResult validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return new ValidationResult(false, "邮箱不能为空");
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return new ValidationResult(false, "邮箱格式不正确");
        }
        
        return new ValidationResult(true, "");
    }
    
    /**
     * 验证问题内容是否有效
     * 
     * @param content 问题内容
     * @return 验证结果
     */
    public static ValidationResult validateQuestionContent(String content) {
        if (TextUtils.isEmpty(content)) {
            return new ValidationResult(false, "问题内容不能为空");
        }
        
        if (content.trim().length() < 5) {
            return new ValidationResult(false, "问题内容至少需要5个字符");
        }
        
        if (content.length() > 500) {
            return new ValidationResult(false, "问题内容最多500个字符");
        }
        
        return new ValidationResult(true, "");
    }
    
    /**
     * 验证消息内容是否有效
     * 
     * @param content 消息内容
     * @return 验证结果
     */
    public static ValidationResult validateMessageContent(String content) {
        if (TextUtils.isEmpty(content)) {
            return new ValidationResult(false, "消息内容不能为空");
        }
        
        if (content.trim().isEmpty()) {
            return new ValidationResult(false, "消息内容不能为空");
        }
        
        if (content.length() > 1000) {
            return new ValidationResult(false, "消息内容最多1000个字符");
        }
        
        return new ValidationResult(true, "");
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}

