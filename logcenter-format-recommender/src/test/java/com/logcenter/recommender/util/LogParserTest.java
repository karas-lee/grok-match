package com.logcenter.recommender.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.*;

/**
 * LogParser 유틸리티 클래스 단위 테스트
 */
public class LogParserTest {
    
    private List<String> testLogs;
    
    @Before
    public void setUp() {
        testLogs = Arrays.asList(
            "2024-01-15 10:30:45 INFO Starting application",
            "2024-01-15 10:30:46 ERROR Connection failed",
            "  at com.example.Main.connect(Main.java:45)",
            "  at com.example.Main.main(Main.java:10)",
            "2024-01-15 10:30:47 INFO Retry connection",
            "",
            "   ",
            "192.168.1.1 - - [15/Jan/2024:10:30:45 +0900] \"GET /index.html HTTP/1.1\" 200 1234"
        );
    }
    
    @Test
    public void testNormalizeLog() {
        // 기본 정규화
        assertEquals("test log", LogParser.normalizeLog("  test   log  "));
        
        // 제어 문자 제거
        assertEquals("clean log", LogParser.normalizeLog("clean\u0000log"));
        
        // 탭은 공백으로
        assertEquals("tab to space", LogParser.normalizeLog("tab\tto\tspace"));
        
        // null 처리
        assertEquals("", LogParser.normalizeLog(null));
    }
    
    @Test
    public void testMergeMultilineLog() {
        List<String> merged = LogParser.mergeMultilineLog(testLogs);
        
        // 8개 라인이 4개로 병합 (스택트레이스 병합, 빈 라인도 병합)
        assertEquals(4, merged.size());
        
        // 스택트레이스가 병합되었는지 확인
        String errorLog = merged.get(1);
        assertTrue(errorLog.contains("ERROR Connection failed"));
        assertTrue(errorLog.contains("at com.example.Main.connect"));
        assertTrue(errorLog.contains("at com.example.Main.main"));
        
        // 다른 로그는 그대로
        assertEquals("2024-01-15 10:30:45 INFO Starting application", merged.get(0));
        
        // Retry connection 로그 (빈 라인들이 병합됨)
        String retryLog = merged.get(2);
        assertTrue(retryLog.contains("2024-01-15 10:30:47 INFO Retry connection"));
        
        // Apache 로그
        String apacheLog = merged.get(3);
        assertTrue(apacheLog.startsWith("192.168.1.1"));
    }
    
    @Test
    public void testExtractTimestamp() {
        // ISO 8601 형식
        assertEquals("2024-01-15 10:30:45", 
            LogParser.extractTimestamp("2024-01-15 10:30:45 INFO Test log"));
        
        // Apache 형식
        assertEquals("15/Jan/2024:10:30:45", 
            LogParser.extractTimestamp("192.168.1.1 - - [15/Jan/2024:10:30:45 +0900] \"GET /\""));
        
        // Syslog 형식
        assertEquals("Oct  4 14:14:53", 
            LogParser.extractTimestamp("Oct  4 14:14:53 logcenter sshd[8536]: Failed"));
        
        // 타임스탬프 없음
        assertNull(LogParser.extractTimestamp("This log has no timestamp"));
        
        // null 처리
        assertNull(LogParser.extractTimestamp(null));
    }
    
    @Test
    public void testSampleLogs() {
        List<String> largeLogs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeLogs.add("Log entry " + i);
        }
        
        // 10개 샘플링
        List<String> sampled = LogParser.sampleLogs(largeLogs, 10);
        assertEquals(10, sampled.size());
        
        // 균등 분포 확인
        assertTrue(sampled.contains("Log entry 0"));
        assertTrue(sampled.contains("Log entry 90"));
        
        // 작은 로그는 그대로
        List<String> smallLogs = Arrays.asList("Log1", "Log2", "Log3");
        List<String> smallSampled = LogParser.sampleLogs(smallLogs, 10);
        assertEquals(3, smallSampled.size());
        
        // null 처리
        assertNull(LogParser.sampleLogs(null, 10));
    }
    
    @Test
    public void testGetLogStatistics() {
        Map<String, Object> stats = LogParser.getLogStatistics(testLogs);
        
        assertEquals(8, stats.get("totalLogs")); // 빈 라인 포함
        assertTrue((Integer) stats.get("avgLength") > 0);
        assertTrue((Integer) stats.get("minLength") >= 0);
        assertTrue((Integer) stats.get("maxLength") > 0);
        assertTrue((Double) stats.get("timestampRatio") > 0);
        
        // 빈 리스트
        Map<String, Object> emptyStats = LogParser.getLogStatistics(new ArrayList<>());
        assertEquals(0, emptyStats.get("totalLogs"));
        assertEquals(0, emptyStats.get("avgLength"));
        
        // null 처리
        Map<String, Object> nullStats = LogParser.getLogStatistics(null);
        assertEquals(0, nullStats.get("totalLogs"));
    }
    
    @Test
    public void testFilterLogs() {
        List<String> filtered = LogParser.filterLogs(testLogs);
        
        // 빈 라인 제거
        assertEquals(6, filtered.size()); // 8 - 2 (빈 라인)
        
        for (String log : filtered) {
            assertNotNull(log);
            assertFalse(log.trim().isEmpty());
        }
        
        // null 처리
        List<String> nullFiltered = LogParser.filterLogs(null);
        assertNotNull(nullFiltered);
        assertTrue(nullFiltered.isEmpty());
    }
    
    @Test
    public void testMultilinePatterns() {
        List<String> javaLogs = Arrays.asList(
            "2024-01-15 10:30:45 ERROR NullPointerException occurred",
            "java.lang.NullPointerException: Cannot invoke method",
            "\tat com.example.Service.process(Service.java:123)",
            "\tat com.example.Controller.handle(Controller.java:45)",
            "Caused by: java.lang.IllegalStateException",
            "\tat com.example.Repository.find(Repository.java:78)",
            "\t... 10 more",
            "2024-01-15 10:30:46 INFO Recovery started"
        );
        
        List<String> merged = LogParser.mergeMultilineLog(javaLogs);
        // "java.lang.NullPointerException"도 새로운 로그로 인식되므로 3개로 병합
        assertEquals(3, merged.size());
        
        // 첫 번째 로그
        String errorLog = merged.get(0);
        assertTrue(errorLog.contains("ERROR NullPointerException occurred"));
        
        // 두 번째 로그 (스택트레이스)
        String stackTrace = merged.get(1);
        assertTrue(stackTrace.contains("java.lang.NullPointerException"));
        assertTrue(stackTrace.contains("Caused by"));
        assertTrue(stackTrace.contains("... 10 more"));
    }
}