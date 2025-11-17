package com.dowdah.asknow.constants;

/**
 * API 相关常量
 * 管理 HTTP 请求、表单字段名等 API 协议相关的常量
 */
public final class ApiConstants {
    
    // ==================== HTTP 表单字段名 ====================
    
    /**
     * 图片上传表单字段名
     */
    public static final String FORM_FIELD_IMAGE = "image";
    
    // ==================== 其他 API 常量 ====================
    
    // 可以在这里添加其他 API 相关的常量，例如：
    // - HTTP 头部字段名
    // - API 版本号
    // - 内容类型等
    
    // Private constructor to prevent instantiation
    private ApiConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}

