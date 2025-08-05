package com.logcenter.recommender.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

/**
 * FormatRecommendation 모델 클래스 단위 테스트
 */
public class FormatRecommendationTest {
    
    private FormatRecommendation recommendation;
    private LogFormat logFormat;
    
    @Before
    public void setUp() {
        logFormat = new LogFormat();
        logFormat.setFormatId("APACHE_HTTP_1.00");
        logFormat.setFormatName("APACHE_HTTP");
        logFormat.setFormatVersion("1.00");
        logFormat.setGroupName("Web Server");
        logFormat.setVendor("APACHE");
        
        recommendation = new FormatRecommendation(logFormat);
    }
    
    @Test
    public void testSingleCompleteMatch() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("client", "192.168.1.1");
        fields.put("method", "GET");
        
        MatchResult match = MatchResult.completeMatch(
            "APACHE_HTTP_1.00",
            "APACHE_ACCESS_LOG",
            fields
        );
        
        recommendation.addMatchResult(match);
        
        assertEquals(98.0, recommendation.getConfidence(), 0.001);
        assertTrue(recommendation.isExactMatch());
        assertEquals("단일 Grok 패턴이 완벽하게 매칭됨", recommendation.getRecommendationReason());
        assertEquals(1, recommendation.getSuccessfulMatches());
    }
    
    @Test
    public void testMultipleCompleteMatches() {
        // 첫 번째 완전 매칭
        Map<String, Object> fields1 = new HashMap<>();
        fields1.put("client", "192.168.1.1");
        MatchResult match1 = MatchResult.completeMatch("APACHE_HTTP_1.00", "PATTERN1", fields1);
        
        // 두 번째 완전 매칭
        Map<String, Object> fields2 = new HashMap<>();
        fields2.put("client", "192.168.1.2");
        MatchResult match2 = MatchResult.completeMatch("APACHE_HTTP_1.00", "PATTERN2", fields2);
        
        recommendation.addMatchResult(match1);
        recommendation.addMatchResult(match2);
        
        assertEquals(96.0, recommendation.getConfidence(), 0.001); // 95 + (2 * 0.5) = 96
        assertTrue(recommendation.isExactMatch());
        assertEquals("2개의 Grok 패턴이 완벽하게 매칭됨", recommendation.getRecommendationReason());
        assertEquals(2, recommendation.getSuccessfulMatches());
    }
    
    @Test
    public void testPartialMatches() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("client", "192.168.1.1");
        
        MatchResult match1 = MatchResult.partialMatch("APACHE_HTTP_1.00", "PATTERN1", fields, 0.8);
        MatchResult match2 = MatchResult.partialMatch("APACHE_HTTP_1.00", "PATTERN2", fields, 0.6);
        
        recommendation.addMatchResult(match1);
        recommendation.addMatchResult(match2);
        
        // 평균 점수: (0.8 + 0.6) / 2 = 0.7, 신뢰도: 0.7 * 70 = 49
        assertEquals(49.0, recommendation.getConfidence(), 0.001);
        assertFalse(recommendation.isExactMatch());
        assertEquals("2개의 패턴이 부분적으로 매칭됨", recommendation.getRecommendationReason());
    }
    
    @Test
    public void testNoMatches() {
        MatchResult noMatch = MatchResult.noMatch("APACHE_HTTP_1.00", "PATTERN1");
        recommendation.addMatchResult(noMatch);
        
        assertEquals(0.0, recommendation.getConfidence(), 0.001);
        assertFalse(recommendation.isExactMatch());
        assertEquals("매칭되는 패턴이 없음", recommendation.getRecommendationReason());
        assertEquals(0, recommendation.getSuccessfulMatches());
    }
    
    @Test
    public void testGetBestMatchResult() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("test", "value");
        
        // 부분 매칭들
        MatchResult partial1 = MatchResult.partialMatch("ID", "P1", fields, 0.6);
        MatchResult partial2 = MatchResult.partialMatch("ID", "P2", fields, 0.8);
        
        // 완전 매칭
        MatchResult complete = MatchResult.completeMatch("ID", "P3", fields);
        
        recommendation.addMatchResult(partial1);
        recommendation.addMatchResult(partial2);
        recommendation.addMatchResult(complete);
        
        // 완전 매칭이 최고 결과
        MatchResult best = recommendation.getBestMatchResult();
        assertNotNull(best);
        assertTrue(best.isCompleteMatch());
        assertEquals("P3", best.getPatternName());
    }
    
    @Test
    public void testCompareTo() {
        FormatRecommendation rec1 = new FormatRecommendation();
        FormatRecommendation rec2 = new FormatRecommendation();
        FormatRecommendation rec3 = new FormatRecommendation();
        
        rec1.setConfidence(95.0);
        rec2.setConfidence(98.0);
        rec3.setConfidence(95.0);
        
        rec1.setExactMatch(false);
        rec2.setExactMatch(true);
        rec3.setExactMatch(true);
        
        List<FormatRecommendation> list = Arrays.asList(rec1, rec2, rec3);
        Collections.sort(list);
        
        // rec2 (98%, exact) > rec3 (95%, exact) > rec1 (95%, not exact)
        assertEquals(98.0, list.get(0).getConfidence(), 0.001);
        assertEquals(95.0, list.get(1).getConfidence(), 0.001);
        assertTrue(list.get(1).isExactMatch());
        assertEquals(95.0, list.get(2).getConfidence(), 0.001);
        assertFalse(list.get(2).isExactMatch());
    }
    
    @Test
    public void testGetSummary() {
        recommendation.setConfidence(98.0);
        recommendation.setExactMatch(true);
        
        String summary = recommendation.getSummary();
        assertTrue(summary.contains("APACHE_HTTP"));
        assertTrue(summary.contains("v1.00"));
        assertTrue(summary.contains("98.0%"));
        assertTrue(summary.contains("Web Server"));
        assertTrue(summary.contains("APACHE"));
        assertTrue(summary.contains("[정확한 매칭]"));
    }
    
    @Test
    public void testGettersAndSetters() {
        recommendation.setRank(1);
        recommendation.setAnalysisTime(150L);
        recommendation.setTotalPatternsChecked(50);
        
        assertEquals(1, recommendation.getRank());
        assertEquals(150L, recommendation.getAnalysisTime());
        assertEquals(50, recommendation.getTotalPatternsChecked());
        assertEquals("Web Server", recommendation.getFormatGroup());
        assertEquals("APACHE", recommendation.getFormatVendor());
    }
}