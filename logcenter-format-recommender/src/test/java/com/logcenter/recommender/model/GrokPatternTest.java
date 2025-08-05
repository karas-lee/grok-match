package com.logcenter.recommender.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * GrokPattern 모델 클래스 단위 테스트
 */
public class GrokPatternTest {
    
    private GrokPattern pattern;
    
    @Before
    public void setUp() {
        pattern = new GrokPattern("SRC_IP", "(?:(?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})[.](?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})[.](?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})[.](?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2}))");
    }
    
    @Test
    public void testConstructors() {
        GrokPattern p1 = new GrokPattern();
        assertNull(p1.getName());
        assertFalse(p1.isCompiled());
        
        GrokPattern p2 = new GrokPattern("TEST", "\\d+");
        assertEquals("TEST", p2.getName());
        assertEquals("\\d+", p2.getPattern());
        assertEquals("CUSTOM", p2.getType());
        assertFalse(p2.isCompiled());
        
        GrokPattern p3 = new GrokPattern("TEST", "\\d+", "STANDARD");
        assertEquals("TEST", p3.getName());
        assertEquals("\\d+", p3.getPattern());
        assertEquals("STANDARD", p3.getType());
    }
    
    @Test
    public void testCategorize() {
        // IP 카테고리
        pattern.categorize();
        assertEquals("IP", pattern.getCategory());
        
        // PORT 카테고리
        GrokPattern portPattern = new GrokPattern("SRC_PORT", "\\d{1,5}");
        portPattern.categorize();
        assertEquals("PORT", portPattern.getCategory());
        
        // DATE_TIME 카테고리
        GrokPattern datePattern = new GrokPattern("DATE_FORMAT1", "\\d{4}-\\d{2}-\\d{2}");
        datePattern.categorize();
        assertEquals("DATE_TIME", datePattern.getCategory());
        
        // TEXT 카테고리
        GrokPattern textPattern = new GrokPattern("TEXT1", "[^\\n]+");
        textPattern.categorize();
        assertEquals("TEXT", textPattern.getCategory());
        
        // EMAIL 카테고리
        GrokPattern emailPattern = new GrokPattern("MAIL", "[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}");
        emailPattern.categorize();
        assertEquals("EMAIL", emailPattern.getCategory());
        
        // NUMBER 카테고리
        GrokPattern countPattern = new GrokPattern("COUNT", "\\d+");
        countPattern.categorize();
        assertEquals("NUMBER", countPattern.getCategory());
        
        // VENDOR_SPECIFIC 카테고리
        GrokPattern ciscoPattern = new GrokPattern("CISCO1", ".*");
        ciscoPattern.categorize();
        assertEquals("VENDOR_SPECIFIC", ciscoPattern.getCategory());
        
        // FIELD 카테고리 (기본값)
        GrokPattern fieldPattern = new GrokPattern("UNKNOWN_FIELD", ".*");
        fieldPattern.categorize();
        assertEquals("FIELD", fieldPattern.getCategory());
    }
    
    @Test
    public void testSetPattern() {
        assertTrue(pattern.isCompiled() == false);
        
        pattern.setPattern("new pattern");
        assertEquals("new pattern", pattern.getPattern());
        assertFalse(pattern.isCompiled()); // 패턴 변경 시 재컴파일 필요
    }
    
    @Test
    public void testEquals() {
        GrokPattern other = new GrokPattern("SRC_IP", "different pattern");
        assertTrue(pattern.equals(other)); // 이름만으로 비교
        
        GrokPattern different = new GrokPattern("DST_IP", pattern.getPattern());
        assertFalse(pattern.equals(different));
    }
    
    @Test
    public void testHashCode() {
        GrokPattern other = new GrokPattern("SRC_IP", "different pattern");
        assertEquals(pattern.hashCode(), other.hashCode());
    }
    
    @Test
    public void testToString() {
        pattern.setType("CUSTOM");
        pattern.categorize();
        
        String str = pattern.toString();
        assertTrue(str.contains("SRC_IP"));
        assertTrue(str.contains("CUSTOM"));
        assertTrue(str.contains("IP"));
        assertTrue(str.contains("isCompiled=false"));
    }
}