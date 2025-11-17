package com.dowdah.asknow.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * ValidationUtils 单元测试
 * 
 * 测试功能：
 * - 用户名验证
 * - 密码验证
 * - 邮箱验证
 * - 问题内容验证
 * - 消息内容验证
 */
public class ValidationUtilsTest {
    
    /**
     * 测试isNotNullOrEmpty
     */
    @Test
    public void testIsNotNullOrEmpty() {
        // null
        assertFalse(ValidationUtils.isNotNullOrEmpty(null));
        
        // 空字符串
        assertFalse(ValidationUtils.isNotNullOrEmpty(""));
        
        // 只有空格
        assertFalse(ValidationUtils.isNotNullOrEmpty("   "));
        
        // 有效字符串
        assertTrue(ValidationUtils.isNotNullOrEmpty("test"));
        assertTrue(ValidationUtils.isNotNullOrEmpty(" test "));
    }
    
    /**
     * 测试isValidUsername
     */
    @Test
    public void testIsValidUsername() {
        // null
        assertFalse(ValidationUtils.isValidUsername(null));
        
        // 太短
        assertFalse(ValidationUtils.isValidUsername("ab"));
        
        // 太长
        assertFalse(ValidationUtils.isValidUsername("a".repeat(21)));
        
        // 包含非法字符
        assertFalse(ValidationUtils.isValidUsername("user@123"));
        assertFalse(ValidationUtils.isValidUsername("user name"));
        assertFalse(ValidationUtils.isValidUsername("用户名"));
        
        // 有效用户名
        assertTrue(ValidationUtils.isValidUsername("abc"));
        assertTrue(ValidationUtils.isValidUsername("user123"));
        assertTrue(ValidationUtils.isValidUsername("user_name"));
        assertTrue(ValidationUtils.isValidUsername("User_123"));
    }
    
    /**
     * 测试isValidPassword
     */
    @Test
    public void testIsValidPassword() {
        // null
        assertFalse(ValidationUtils.isValidPassword(null));
        
        // 太短
        assertFalse(ValidationUtils.isValidPassword("12345"));
        
        // 太长
        assertFalse(ValidationUtils.isValidPassword("a".repeat(21)));
        
        // 有效密码
        assertTrue(ValidationUtils.isValidPassword("123456"));
        assertTrue(ValidationUtils.isValidPassword("password"));
        assertTrue(ValidationUtils.isValidPassword("Pass@123!"));
        assertTrue(ValidationUtils.isValidPassword("a".repeat(20)));
    }
    
    /**
     * 测试validateUsername
     */
    @Test
    public void testValidateUsername() {
        // 空用户名
        ValidationUtils.ValidationResult result1 = 
            ValidationUtils.validateUsername("");
        assertFalse(result1.isValid());
        assertNotNull(result1.getMessage());
        
        // 太短
        ValidationUtils.ValidationResult result2 = 
            ValidationUtils.validateUsername("ab");
        assertFalse(result2.isValid());
        assertTrue(result2.getMessage().contains("至少"));
        
        // 太长
        ValidationUtils.ValidationResult result3 = 
            ValidationUtils.validateUsername("a".repeat(21));
        assertFalse(result3.isValid());
        assertTrue(result3.getMessage().contains("最多"));
        
        // 非法字符
        ValidationUtils.ValidationResult result4 = 
            ValidationUtils.validateUsername("user@123");
        assertFalse(result4.isValid());
        assertTrue(result4.getMessage().contains("字母") || 
                   result4.getMessage().contains("数字") ||
                   result4.getMessage().contains("下划线"));
        
        // 有效用户名
        ValidationUtils.ValidationResult result5 = 
            ValidationUtils.validateUsername("user123");
        assertTrue(result5.isValid());
        assertNotNull(result5.getMessage());
    }
    
    /**
     * 测试validatePassword
     */
    @Test
    public void testValidatePassword() {
        // 空密码
        ValidationUtils.ValidationResult result1 = 
            ValidationUtils.validatePassword("");
        assertFalse(result1.isValid());
        
        // 太短
        ValidationUtils.ValidationResult result2 = 
            ValidationUtils.validatePassword("12345");
        assertFalse(result2.isValid());
        assertTrue(result2.getMessage().contains("至少"));
        
        // 太长
        ValidationUtils.ValidationResult result3 = 
            ValidationUtils.validatePassword("a".repeat(21));
        assertFalse(result3.isValid());
        assertTrue(result3.getMessage().contains("最多"));
        
        // 有效密码
        ValidationUtils.ValidationResult result4 = 
            ValidationUtils.validatePassword("password123");
        assertTrue(result4.isValid());
    }
    
    /**
     * 测试validateEmail
     */
    @Test
    public void testValidateEmail() {
        // 空邮箱
        ValidationUtils.ValidationResult result1 = 
            ValidationUtils.validateEmail("");
        assertFalse(result1.isValid());
        
        // 无效邮箱格式
        ValidationUtils.ValidationResult result2 = 
            ValidationUtils.validateEmail("invalid");
        assertFalse(result2.isValid());
        assertTrue(result2.getMessage().contains("格式"));
        
        ValidationUtils.ValidationResult result3 = 
            ValidationUtils.validateEmail("test@");
        assertFalse(result3.isValid());
        
        ValidationUtils.ValidationResult result4 = 
            ValidationUtils.validateEmail("@example.com");
        assertFalse(result4.isValid());
        
        // 有效邮箱
        ValidationUtils.ValidationResult result5 = 
            ValidationUtils.validateEmail("test@example.com");
        assertTrue(result5.isValid());
        
        ValidationUtils.ValidationResult result6 = 
            ValidationUtils.validateEmail("user.name+tag@example.co.uk");
        assertTrue(result6.isValid());
    }
    
    /**
     * 测试validateQuestionContent
     */
    @Test
    public void testValidateQuestionContent() {
        // 空内容
        ValidationUtils.ValidationResult result1 = 
            ValidationUtils.validateQuestionContent("");
        assertFalse(result1.isValid());
        
        // 太短
        ValidationUtils.ValidationResult result2 = 
            ValidationUtils.validateQuestionContent("1234");
        assertFalse(result2.isValid());
        assertTrue(result2.getMessage().contains("至少"));
        
        // 太长
        ValidationUtils.ValidationResult result3 = 
            ValidationUtils.validateQuestionContent("a".repeat(501));
        assertFalse(result3.isValid());
        assertTrue(result3.getMessage().contains("最多"));
        
        // 有效内容
        ValidationUtils.ValidationResult result4 = 
            ValidationUtils.validateQuestionContent("How to solve this problem?");
        assertTrue(result4.isValid());
    }
    
    /**
     * 测试validateMessageContent
     */
    @Test
    public void testValidateMessageContent() {
        // 空内容
        ValidationUtils.ValidationResult result1 = 
            ValidationUtils.validateMessageContent("");
        assertFalse(result1.isValid());
        
        // 只有空格
        ValidationUtils.ValidationResult result2 = 
            ValidationUtils.validateMessageContent("   ");
        assertFalse(result2.isValid());
        
        // 太长
        ValidationUtils.ValidationResult result3 = 
            ValidationUtils.validateMessageContent("a".repeat(1001));
        assertFalse(result3.isValid());
        assertTrue(result3.getMessage().contains("最多"));
        
        // 有效内容
        ValidationUtils.ValidationResult result4 = 
            ValidationUtils.validateMessageContent("Hello, this is a test message.");
        assertTrue(result4.isValid());
        
        // 最大长度边界
        ValidationUtils.ValidationResult result5 = 
            ValidationUtils.validateMessageContent("a".repeat(1000));
        assertTrue(result5.isValid());
    }
    
    /**
     * 测试ValidationResult类
     */
    @Test
    public void testValidationResult() {
        ValidationUtils.ValidationResult result1 = 
            new ValidationUtils.ValidationResult(true, "Success");
        assertTrue(result1.isValid());
        assertEquals("Success", result1.getMessage());
        
        ValidationUtils.ValidationResult result2 = 
            new ValidationUtils.ValidationResult(false, "Error message");
        assertFalse(result2.isValid());
        assertEquals("Error message", result2.getMessage());
    }
}

