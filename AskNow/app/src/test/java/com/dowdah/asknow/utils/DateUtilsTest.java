package com.dowdah.asknow.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * DateUtils 单元测试
 * 
 * 测试功能：
 * - 日期格式化
 * - 相对时间显示
 * - 时间判断（今天、本周）
 */
public class DateUtilsTest {
    
    /**
     * 测试formatDateTime - 默认格式
     */
    @Test
    public void testFormatDateTime_DefaultFormat() {
        long timestamp = 1640000000000L; // 2021-12-20 14:13:20 UTC
        String formatted = DateUtils.formatDateTime(timestamp);
        
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
        // 应该包含日期和时间
        assertTrue(formatted.contains("2021") || formatted.contains("Dec"));
    }
    
    /**
     * 测试formatDateTime - 自定义格式
     */
    @Test
    public void testFormatDateTime_CustomFormat() {
        long timestamp = 1640000000000L;
        String formatted = DateUtils.formatDateTime(timestamp, "yyyy-MM-dd");
        
        assertNotNull(formatted);
        assertTrue(formatted.contains("2021"));
        assertTrue(formatted.contains("12"));
        assertTrue(formatted.contains("20"));
    }
    
    /**
     * 测试formatDate
     */
    @Test
    public void testFormatDate() {
        long timestamp = 1640000000000L;
        String formatted = DateUtils.formatDate(timestamp);
        
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty());
        // 不应该包含时间
        assertFalse(formatted.contains(":"));
    }
    
    /**
     * 测试formatTime
     */
    @Test
    public void testFormatTime() {
        long timestamp = 1640000000000L;
        String formatted = DateUtils.formatTime(timestamp);
        
        assertNotNull(formatted);
        // 应该只包含时间，格式为 HH:mm
        assertTrue(formatted.matches("\\d{2}:\\d{2}"));
    }
    
    /**
     * 测试formatISO
     */
    @Test
    public void testFormatISO() {
        long timestamp = 1640000000000L;
        String formatted = DateUtils.formatISO(timestamp);
        
        assertNotNull(formatted);
        // ISO格式应该包含日期和时间，用空格分隔
        assertTrue(formatted.contains(" "));
        assertTrue(formatted.contains(":"));
        assertTrue(formatted.contains("2021"));
    }
    
    /**
     * 测试formatDateTime - 无效时间戳
     */
    @Test
    public void testFormatDateTime_InvalidTimestamp() {
        // 负数时间戳
        String formatted = DateUtils.formatDateTime(-1L);
        // 应该返回某个字符串（可能是空或错误格式）
        assertNotNull(formatted);
    }
    
    /**
     * 测试getRelativeTimeSpan - 刚刚
     */
    @Test
    public void testGetRelativeTimeSpan_JustNow() {
        long now = System.currentTimeMillis();
        long timestamp1 = now - 30 * 1000; // 30秒前
        
        String relative = DateUtils.getRelativeTimeSpan(timestamp1);
        assertEquals("刚刚", relative);
    }
    
    /**
     * 测试getRelativeTimeSpan - 分钟前
     */
    @Test
    public void testGetRelativeTimeSpan_MinutesAgo() {
        long now = System.currentTimeMillis();
        long timestamp = now - 5 * 60 * 1000; // 5分钟前
        
        String relative = DateUtils.getRelativeTimeSpan(timestamp);
        assertTrue(relative.contains("分钟前"));
        assertTrue(relative.contains("5"));
    }
    
    /**
     * 测试getRelativeTimeSpan - 小时前
     */
    @Test
    public void testGetRelativeTimeSpan_HoursAgo() {
        long now = System.currentTimeMillis();
        long timestamp = now - 3 * 60 * 60 * 1000; // 3小时前
        
        String relative = DateUtils.getRelativeTimeSpan(timestamp);
        assertTrue(relative.contains("小时前"));
        assertTrue(relative.contains("3"));
    }
    
    /**
     * 测试getRelativeTimeSpan - 天前
     */
    @Test
    public void testGetRelativeTimeSpan_DaysAgo() {
        long now = System.currentTimeMillis();
        long timestamp = now - 2 * 24 * 60 * 60 * 1000; // 2天前
        
        String relative = DateUtils.getRelativeTimeSpan(timestamp);
        assertTrue(relative.contains("天前"));
        assertTrue(relative.contains("2"));
    }
    
    /**
     * 测试getRelativeTimeSpan - 超过7天显示日期
     */
    @Test
    public void testGetRelativeTimeSpan_MoreThanWeek() {
        long now = System.currentTimeMillis();
        long timestamp = now - 10 * 24 * 60 * 60 * 1000; // 10天前
        
        String relative = DateUtils.getRelativeTimeSpan(timestamp);
        // 应该显示具体日期，不是"天前"
        assertFalse(relative.contains("天前"));
        assertFalse(relative.isEmpty());
    }
    
    /**
     * 测试getRelativeTimeSpan - 未来时间
     */
    @Test
    public void testGetRelativeTimeSpan_FutureTime() {
        long now = System.currentTimeMillis();
        long timestamp = now + 5 * 60 * 1000; // 5分钟后
        
        String relative = DateUtils.getRelativeTimeSpan(timestamp);
        assertEquals("刚刚", relative);
    }
    
    /**
     * 测试isToday - 今天
     */
    @Test
    public void testIsToday_Today() {
        long now = System.currentTimeMillis();
        assertTrue(DateUtils.isToday(now));
        
        // 今天的开始（午夜）
        long todayMidnight = now - (now % (24 * 60 * 60 * 1000));
        assertTrue(DateUtils.isToday(todayMidnight));
    }
    
    /**
     * 测试isToday - 昨天
     */
    @Test
    public void testIsToday_Yesterday() {
        long now = System.currentTimeMillis();
        long yesterday = now - 24 * 60 * 60 * 1000;
        assertFalse(DateUtils.isToday(yesterday));
    }
    
    /**
     * 测试isToday - 明天
     */
    @Test
    public void testIsToday_Tomorrow() {
        long now = System.currentTimeMillis();
        long tomorrow = now + 24 * 60 * 60 * 1000;
        assertFalse(DateUtils.isToday(tomorrow));
    }
    
    /**
     * 测试isThisWeek - 本周
     */
    @Test
    public void testIsThisWeek_ThisWeek() {
        long now = System.currentTimeMillis();
        assertTrue(DateUtils.isThisWeek(now));
        
        // 3天前
        long threeDaysAgo = now - 3 * 24 * 60 * 60 * 1000;
        assertTrue(DateUtils.isThisWeek(threeDaysAgo));
    }
    
    /**
     * 测试isThisWeek - 上周
     */
    @Test
    public void testIsThisWeek_LastWeek() {
        long now = System.currentTimeMillis();
        long lastWeek = now - 8 * 24 * 60 * 60 * 1000;
        assertFalse(DateUtils.isThisWeek(lastWeek));
    }
    
    /**
     * 测试isThisWeek - 未来
     */
    @Test
    public void testIsThisWeek_Future() {
        long now = System.currentTimeMillis();
        long future = now + 2 * 24 * 60 * 60 * 1000;
        // 未来2天应该返回false（方法检查diff >= 0）
        assertFalse(DateUtils.isThisWeek(future));
    }
    
    /**
     * 测试边界情况 - 零时间戳
     */
    @Test
    public void testEdgeCase_ZeroTimestamp() {
        String formatted = DateUtils.formatDateTime(0L);
        assertNotNull(formatted);
        
        String relative = DateUtils.getRelativeTimeSpan(0L);
        assertNotNull(relative);
    }
    
    /**
     * 测试formatDateTime - 无效格式
     */
    @Test
    public void testFormatDateTime_InvalidFormat() {
        long timestamp = System.currentTimeMillis();
        String formatted = DateUtils.formatDateTime(timestamp, "invalid_format");
        
        // 无效格式应该返回空字符串（根据实现）
        assertNotNull(formatted);
    }
}

