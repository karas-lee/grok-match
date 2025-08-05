package com.logcenter.recommender.filter;

import com.logcenter.recommender.model.LogFormat;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * PatternFilter 단위 테스트
 */
public class PatternFilterTest {
    
    @Test
    public void testIsOverlyGeneric_TimeAndMessage() {
        // 시간과 메시지만 있는 패턴
        assertTrue(PatternFilter.isOverlyGeneric("^%{LOG_TIME:log_time} %{MESSAGE:message}$"));
        assertTrue(PatternFilter.isOverlyGeneric("^%{LOG_TIME:log_time}\\s+%{MESSAGE:message}$"));
    }
    
    @Test
    public void testIsOverlyGeneric_MessageOnly() {
        // 메시지만 있는 패턴
        assertTrue(PatternFilter.isOverlyGeneric("^%{MESSAGE:message}$"));
        assertTrue(PatternFilter.isOverlyGeneric("^.*%{MESSAGE:message}.*$"));
    }
    
    @Test
    public void testIsOverlyGeneric_GreedyData() {
        // GREEDYDATA 패턴
        assertTrue(PatternFilter.isOverlyGeneric("^%{GREEDYDATA:data}$"));
        assertTrue(PatternFilter.isOverlyGeneric("^%{DATA:data}$"));
    }
    
    @Test
    public void testIsOverlyGeneric_TooShort() {
        // 너무 짧은 패턴
        assertTrue(PatternFilter.isOverlyGeneric("^.*$"));
        assertTrue(PatternFilter.isOverlyGeneric(""));
        assertTrue(PatternFilter.isOverlyGeneric(null));
    }
    
    @Test
    public void testIsOverlyGeneric_SpecificPatterns() {
        // 구체적인 패턴들은 일반적이지 않음
        assertFalse(PatternFilter.isOverlyGeneric(
            "^%{DATE_FORMAT1:log_time} %{SRC_IP:src_ip} %{DST_IP:dst_ip} %{ACTION:action}$"));
        assertFalse(PatternFilter.isOverlyGeneric(
            "^%{LOG_TIME:log_time} \\[%{LOGLEVEL:level}\\] %{IP:client_ip} %{METHOD:method} %{PATH:path}$"));
    }
    
    @Test
    public void testHasUsablePatterns() {
        // 모든 패턴이 일반적인 LogFormat
        LogFormat genericFormat = new LogFormat();
        genericFormat.setFormatName("GENERIC_FORMAT");
        
        LogFormat.LogType logType = new LogFormat.LogType();
        LogFormat.Pattern pattern1 = new LogFormat.Pattern();
        pattern1.setGrokExp("^%{LOG_TIME:log_time} %{MESSAGE:message}$");
        LogFormat.Pattern pattern2 = new LogFormat.Pattern();
        pattern2.setGrokExp("^%{MESSAGE:message}$");
        
        logType.setPatterns(Arrays.asList(pattern1, pattern2));
        genericFormat.setLogTypes(Arrays.asList(logType));
        
        assertFalse(PatternFilter.hasUsablePatterns(genericFormat));
        
        // 사용 가능한 패턴이 있는 LogFormat
        LogFormat specificFormat = new LogFormat();
        specificFormat.setFormatName("SPECIFIC_FORMAT");
        
        LogFormat.LogType logType2 = new LogFormat.LogType();
        LogFormat.Pattern pattern3 = new LogFormat.Pattern();
        pattern3.setGrokExp("^%{DATE_FORMAT1:log_time} %{SRC_IP:src_ip} %{DST_IP:dst_ip}$");
        logType2.setPatterns(Arrays.asList(pattern1, pattern3)); // 하나는 일반적, 하나는 구체적
        specificFormat.setLogTypes(Arrays.asList(logType2));
        
        assertTrue(PatternFilter.hasUsablePatterns(specificFormat));
    }
    
    @Test
    public void testGetSpecificityScore() {
        // 일반적인 패턴은 점수 0
        assertEquals(0.0, PatternFilter.getSpecificityScore("^%{MESSAGE:message}$"), 0.01);
        assertEquals(0.0, PatternFilter.getSpecificityScore("^%{LOG_TIME:log_time} %{MESSAGE:message}$"), 0.01);
        
        // 구체적인 패턴은 높은 점수
        double score1 = PatternFilter.getSpecificityScore(
            "^%{DATE_FORMAT1:log_time} %{SRC_IP:src_ip} %{DST_IP:dst_ip} %{ACTION:action}$");
        assertTrue("Score should be > 0.3, but was: " + score1, score1 > 0.3);
        
        // IP와 PORT가 있으면 더 높은 점수
        double score2 = PatternFilter.getSpecificityScore(
            "^%{SRC_IP:src_ip}:%{SRC_PORT:src_port} -> %{DST_IP:dst_ip}:%{DST_PORT:dst_port}$");
        assertTrue(score2 > score1);
    }
    
    @Test
    public void testFieldCounting() {
        // 실제 SECUI_BLUEMAX_NGF 패턴처럼 복잡한 패턴
        String complexPattern = "^%{DATE_FORMAT1:log_time} %{HOST_NAME:host_name} %{LOG_NAME:log_name}: " +
            "%{ACTION:action} %{PROTOCOL:protocol} %{SRC_IP:src_ip} %{DST_IP:dst_ip} " +
            "%{SRC_PORT:src_port} %{DST_PORT:dst_port} %{MESSAGE:message}$";
        
        assertFalse(PatternFilter.isOverlyGeneric(complexPattern));
        double complexScore = PatternFilter.getSpecificityScore(complexPattern);
        assertTrue("Complex pattern score should be > 0.4, but was: " + complexScore, complexScore > 0.4);
    }
}