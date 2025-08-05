package com.logcenter.recommender.matcher;

import com.logcenter.recommender.grok.GrokCompilerWrapper;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.model.MatchResult;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * 간단한 필터링 테스트
 */
public class SimpleFilterTest {
    
    private GrokCompilerWrapper grokCompiler;
    private SimpleLogMatcher simpleMatcher;
    
    @Before
    public void setUp() {
        grokCompiler = new GrokCompilerWrapper();
        grokCompiler.loadStandardPatterns();
        grokCompiler.loadCustomPatterns();
        
        simpleMatcher = new SimpleLogMatcher(grokCompiler);
    }
    
    @Test
    public void testSimplePatternWithNoUnnamedGroups() {
        // Given
        String logLine = "192.168.1.100 - - [04/Oct/2023:14:14:53 +0900] \"GET /index.html HTTP/1.1\" 200 1234";
        
        LogFormat logFormat = new LogFormat();
        logFormat.setFormatId("apache");
        logFormat.setFormatName("Apache Access");
        logFormat.setGrokPattern("%{IP:client_ip} %{DATA:ident} %{DATA:auth} \\[%{DATA:timestamp}\\] \"%{WORD:method} %{DATA:uri} %{DATA:protocol}\" %{NUMBER:status} %{NUMBER:bytes}");
        
        // When
        MatchResult result = simpleMatcher.match(logLine, logFormat);
        
        // Then
        assertNotNull("결과가 null이 아니어야 함", result);
        assertTrue("매칭이 성공해야 함", result.isCompleteMatch() || result.isPartialMatch());
        
        Map<String, Object> fields = result.getExtractedFields();
        assertNotNull("필드가 null이 아니어야 함", fields);
        
        // 명시적으로 지정된 필드만 있어야 함
        assertTrue(fields.containsKey("client_ip"));
        assertTrue(fields.containsKey("ident"));
        assertTrue(fields.containsKey("auth"));
        assertTrue(fields.containsKey("timestamp"));
        assertTrue(fields.containsKey("method"));
        assertTrue(fields.containsKey("uri"));
        assertTrue(fields.containsKey("protocol"));
        assertTrue(fields.containsKey("status"));
        assertTrue(fields.containsKey("bytes"));
        
        // 예상 필드 확인
        assertEquals("192.168.1.100", fields.get("client_ip"));
        assertEquals("GET", fields.get("method"));
        assertEquals("200", fields.get("status"));
    }
    
    @Test
    public void testPatternWithUnnamedPattern() {
        // Given
        String logLine = "2023-10-04 14:14:53 ERROR MainThread - Connection failed";
        
        LogFormat logFormat = new LogFormat();
        logFormat.setFormatId("custom");
        logFormat.setFormatName("Custom Log");
        // TIMESTAMP_ISO8601은 명명되지 않은 패턴, timestamp는 명명된 필드
        logFormat.setGrokPattern("%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{DATA:thread} - %{GREEDYDATA:message}");
        
        // When
        MatchResult result = simpleMatcher.match(logLine, logFormat);
        
        // Then
        assertNotNull("결과가 null이 아니어야 함", result);
        
        Map<String, Object> fields = result.getExtractedFields();
        assertNotNull("필드가 null이 아니어야 함", fields);
        
        // 명시적으로 지정된 필드만 있어야 함
        assertEquals(4, fields.size());
        assertTrue(fields.containsKey("timestamp"));
        assertTrue(fields.containsKey("level"));
        assertTrue(fields.containsKey("thread"));
        assertTrue(fields.containsKey("message"));
        
        // 확장된 패턴들은 없어야 함
        assertFalse("TIMESTAMP_ISO8601 패턴은 없어야 함", fields.containsKey("TIMESTAMP_ISO8601"));
        assertFalse("YEAR 필드는 없어야 함", fields.containsKey("YEAR"));
        assertFalse("MONTHNUM 필드는 없어야 함", fields.containsKey("MONTHNUM"));
        assertFalse("MONTHDAY 필드는 없어야 함", fields.containsKey("MONTHDAY"));
    }
    
    @Test 
    public void testPatternWithRealUnnamedPattern() {
        // Given - 실제 SKIP처럼 그룹명이 없는 패턴
        String logLine = "192.168.1.1 error something extra text";
        
        LogFormat logFormat = new LogFormat();
        logFormat.setFormatId("test");
        logFormat.setFormatName("Test");
        // IP와 .*는 캡처되지만, .*는 그룹명이 없으므로 필터링되어야 함
        logFormat.setGrokPattern("%{IP:src_ip} %{WORD:level} %{DATA:message} .*");
        
        // When
        MatchResult result = simpleMatcher.match(logLine, logFormat);
        
        // Then
        assertNotNull("결과가 null이 아니어야 함", result);
        
        Map<String, Object> fields = result.getExtractedFields();
        assertNotNull("필드가 null이 아니어야 함", fields);
        
        // 명명된 필드만 있어야 함
        assertEquals(3, fields.size());
        assertEquals("192.168.1.1", fields.get("src_ip"));
        assertEquals("error", fields.get("level"));
        assertEquals("something", fields.get("message"));
    }
}