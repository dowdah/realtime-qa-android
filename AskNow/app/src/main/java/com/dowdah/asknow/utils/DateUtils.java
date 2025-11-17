package com.dowdah.asknow.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 日期工具类，统一日期格式化
 */
public final class DateUtils {
    
    private static final String DEFAULT_DATE_TIME_PATTERN = "MMM dd, yyyy HH:mm";
    private static final String DEFAULT_DATE_PATTERN = "MMM dd, yyyy";
    private static final String DEFAULT_TIME_PATTERN = "HH:mm";
    private static final String ISO_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    
    // Private constructor to prevent instantiation
    private DateUtils() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    /**
     * 将时间戳格式化为默认格式（MMM dd, yyyy HH:mm）
     * 
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的日期字符串
     */
    public static String formatDateTime(long timestamp) {
        return formatDateTime(timestamp, DEFAULT_DATE_TIME_PATTERN);
    }
    
    /**
     * 将时间戳格式化为指定格式
     * 
     * @param timestamp 时间戳（毫秒）
     * @param pattern 日期格式模式
     * @return 格式化后的日期字符串
     */
    public static String formatDateTime(long timestamp, String pattern) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 将时间戳格式化为日期（MMM dd, yyyy）
     * 
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的日期字符串
     */
    public static String formatDate(long timestamp) {
        return formatDateTime(timestamp, DEFAULT_DATE_PATTERN);
    }
    
    /**
     * 将时间戳格式化为时间（HH:mm）
     * 
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的时间字符串
     */
    public static String formatTime(long timestamp) {
        return formatDateTime(timestamp, DEFAULT_TIME_PATTERN);
    }
    
    /**
     * 将时间戳格式化为ISO格式（yyyy-MM-dd HH:mm:ss）
     * 
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的日期字符串
     */
    public static String formatISO(long timestamp) {
        return formatDateTime(timestamp, ISO_DATE_TIME_PATTERN);
    }
    
    /**
     * 获取相对时间描述（例如：刚刚、5分钟前、1小时前等）
     * 
     * @param timestamp 时间戳（毫秒）
     * @return 相对时间描述
     */
    public static String getRelativeTimeSpan(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 0) {
            return "刚刚";
        }
        
        long seconds = diff / 1000;
        if (seconds < 60) {
            return "刚刚";
        }
        
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "分钟前";
        }
        
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "小时前";
        }
        
        long days = hours / 24;
        if (days < 7) {
            return days + "天前";
        }
        
        // 超过7天显示具体日期
        return formatDate(timestamp);
    }
    
    /**
     * 判断是否为今天
     * 
     * @param timestamp 时间戳（毫秒）
     * @return true if the timestamp is today
     */
    public static boolean isToday(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String targetDate = sdf.format(new Date(timestamp));
        String todayDate = sdf.format(new Date());
        return targetDate.equals(todayDate);
    }
    
    /**
     * 判断是否为本周
     * 
     * @param timestamp 时间戳（毫秒）
     * @return true if the timestamp is in this week
     */
    public static boolean isThisWeek(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        return diff >= 0 && diff < 7 * 24 * 60 * 60 * 1000;
    }
}

