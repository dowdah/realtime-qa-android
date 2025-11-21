package com.dowdah.asknow.constants;

/**
 * 应用全局常量
 * 集中管理所有硬编码值，便于维护和配置
 */
public final class AppConstants {
    
    // ==================== 分页相关 ====================
    
    /**
     * 默认每页问题数量
     */
    public static final int DEFAULT_QUESTIONS_PAGE_SIZE = 20;
    
    /**
     * 默认每页消息数量
     */
    public static final int DEFAULT_MESSAGES_PAGE_SIZE = 50;
    
    /**
     * 最大每页问题数量
     */
    public static final int MAX_QUESTIONS_PAGE_SIZE = 100;
    
    /**
     * 最大每页消息数量
     */
    public static final int MAX_MESSAGES_PAGE_SIZE = 200;
    
    /**
     * 默认起始页码
     */
    public static final int DEFAULT_START_PAGE = 1;
    
    // ==================== 重试相关 ====================
    
    /**
     * 最大重试次数
     */
    public static final int MAX_RETRY_COUNT = 3;
    
    /**
     * 初始重试延迟（毫秒）
     */
    public static final long INITIAL_RETRY_DELAY_MS = 1000;
    
    /**
     * 重试退避倍数
     */
    public static final int RETRY_BACKOFF_MULTIPLIER = 2;
    
    // ==================== WebSocket相关 ====================
    
    /**
     * WebSocket退避延迟数组（毫秒）
     */
    public static final int[] WEBSOCKET_BACKOFF_DELAYS = {1000, 2000, 4000, 8000, 16000, 30000};
    
    /**
     * WebSocket最大重连次数
     */
    public static final int WEBSOCKET_MAX_RETRY_COUNT = 10;
    
    /**
     * WebSocket正常关闭代码
     */
    public static final int WEBSOCKET_NORMAL_CLOSURE_CODE = 1000;
    
    // ==================== 文件上传相关 ====================
    
    /**
     * 图片上传表单字段名
     */
    public static final String FORM_FIELD_IMAGE = "image";
    
    /**
     * 最大文件大小（字节）10MB
     */
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    
    /**
     * 图片质量压缩比例
     */
    public static final int IMAGE_COMPRESS_QUALITY = 85;
    
    /**
     * 图片最大宽度
     */
    public static final int IMAGE_MAX_WIDTH = 1920;
    
    /**
     * 图片最大高度
     */
    public static final int IMAGE_MAX_HEIGHT = 1920;
    
    /**
     * 图片缩略图尺寸（dp）
     */
    public static final int IMAGE_THUMBNAIL_SIZE_DP = 120;
    
    // ==================== 缓存相关 ====================
    
    /**
     * 同步初始延迟（毫秒）
     */
    public static final long SYNC_INITIAL_DELAY_MS = 300;
    
    // ==================== UI相关 ====================
    
    /**
     * 分页加载触发阈值（接近底部时提前加载）
     */
    public static final int LOAD_MORE_THRESHOLD = 2;
    
    /**
     * 未读消息显示上限（超过此数量显示 99+）
     */
    public static final int MAX_UNREAD_BADGE_COUNT = 99;
    
    // ==================== 用户角色常量 ====================
    
    /**
     * 学生角色
     */
    public static final String ROLE_STUDENT = "student";
    
    /**
     * 教师角色
     */
    public static final String ROLE_TUTOR = "tutor";

    private AppConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}


