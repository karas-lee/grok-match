package com.logcenter.recommender.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.Map;

/**
 * MatchResult 모델 클래스 단위 테스트
 */
public class MatchResultTest {
    
    private MatchResult matchResult;
    private Map<String, Object> testFields;
    
    @Before
    public void setUp() {
        matchResult = new MatchResult("APACHE_HTTP_1.00", "APACHE_ACCESS_LOG");
        
        testFields = new HashMap<>();
        testFields.put("client", "192.168.1.1");
        testFields.put("method", "GET");
        testFields.put("status", "200");
        testFields.put("bytes", "1234");
    }
    
    @Test
    public void testCompleteMatch() {
        MatchResult result = MatchResult.completeMatch(
            "APACHE_HTTP_1.00", 
            "APACHE_ACCESS_LOG", 
            testFields
        );
        
        assertTrue(result.isCompleteMatch());
        assertFalse(result.isPartialMatch());
        assertEquals(1.0, result.getMatchScore(), 0.001);
        assertEquals(98.0, result.getConfidence(), 0.001);
        assertEquals(4, result.getExtractedFieldCount());
    }
    
    @Test
    public void testPartialMatch() {
        MatchResult result = MatchResult.partialMatch(
            "APACHE_HTTP_1.00", 
            "APACHE_ACCESS_LOG", 
            testFields,
            0.75
        );
        
        assertFalse(result.isCompleteMatch());
        assertTrue(result.isPartialMatch());
        assertEquals(0.75, result.getMatchScore(), 0.001);
        assertEquals(52.5, result.getConfidence(), 0.001); // 0.75 * 70.0
    }
    
    @Test
    public void testNoMatch() {
        MatchResult result = MatchResult.noMatch(
            "APACHE_HTTP_1.00", 
            "APACHE_ACCESS_LOG"
        );
        
        assertFalse(result.isCompleteMatch());
        assertFalse(result.isPartialMatch());
        assertEquals(0.0, result.getMatchScore(), 0.001);
        assertEquals(0.0, result.getConfidence(), 0.001);
        assertEquals(0, result.getExtractedFieldCount());
    }
    
    @Test
    public void testFieldOperations() {
        matchResult.setExtractedFields(testFields);
        
        // 필드 개수 확인
        assertEquals(4, matchResult.getExtractedFieldCount());
        
        // 특정 필드 값 확인
        assertEquals("192.168.1.1", matchResult.getFieldValue("client"));
        assertEquals("GET", matchResult.getFieldValue("method"));
        assertNull(matchResult.getFieldValue("nonexistent"));
        
        // 필드 존재 여부 확인
        assertTrue(matchResult.hasField("client"));
        assertTrue(matchResult.hasField("status"));
        assertFalse(matchResult.hasField("nonexistent"));
    }
    
    @Test
    public void testRequiredFields() {
        matchResult.setExtractedFields(testFields);
        
        // 모든 필수 필드가 있는 경우
        assertTrue(matchResult.hasRequiredFields("client", "method", "status"));
        
        // 일부 필수 필드가 없는 경우
        assertFalse(matchResult.hasRequiredFields("client", "method", "timestamp"));
        
        // null 처리
        assertFalse(matchResult.hasRequiredFields((String[]) null));
        
        // 빈 필드 맵
        matchResult.setExtractedFields(null);
        assertFalse(matchResult.hasRequiredFields("client"));
    }
    
    @Test
    public void testGettersAndSetters() {
        matchResult.setGrokExpression("%{IP:client} %{WORD:method}");
        matchResult.setMatchTime(100L);
        matchResult.setMatchDetails("Perfect match for Apache access log");
        
        assertEquals("%{IP:client} %{WORD:method}", matchResult.getGrokExpression());
        assertEquals(100L, matchResult.getMatchTime());
        assertEquals("Perfect match for Apache access log", matchResult.getMatchDetails());
    }
    
    @Test
    public void testEquals() {
        MatchResult other = new MatchResult("APACHE_HTTP_1.00", "APACHE_ACCESS_LOG");
        assertTrue(matchResult.equals(other));
        
        MatchResult different = new MatchResult("NGINX_1.00", "NGINX_ACCESS_LOG");
        assertFalse(matchResult.equals(different));
    }
    
    @Test
    public void testToString() {
        matchResult.setCompleteMatch(true);
        matchResult.setConfidence(98.0);
        matchResult.setExtractedFields(testFields);
        
        String str = matchResult.toString();
        assertTrue(str.contains("APACHE_HTTP_1.00"));
        assertTrue(str.contains("APACHE_ACCESS_LOG"));
        assertTrue(str.contains("isCompleteMatch=true"));
        assertTrue(str.contains("confidence=98.0"));
        assertTrue(str.contains("extractedFields=4"));
    }
}