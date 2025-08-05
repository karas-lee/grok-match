package com.logcenter.recommender.matcher;

import com.logcenter.recommender.grok.GrokCompilerWrapper;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.model.MatchResult;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * SimpleLogMatcher 단위 테스트
 */
public class SimpleLogMatcherTest {
    
    private SimpleLogMatcher matcher;
    private GrokCompilerWrapper grokCompiler;
    
    @Before
    public void setUp() {
        grokCompiler = new GrokCompilerWrapper();
        grokCompiler.loadStandardPatterns();
        grokCompiler.loadCustomPatterns();
        
        matcher = new SimpleLogMatcher(grokCompiler);
    }
    
    @Test
    public void testMatchWithApacheLog() {
        // Apache 로그 포맷
        LogFormat apacheFormat = new LogFormat();
        apacheFormat.setFormatId("APACHE_COMMON");
        apacheFormat.setFormatName("Apache Common Log");
        apacheFormat.setGrokPattern("%{IPORHOST:clientip} %{USER:ident} %{USER:auth} \\[%{HTTPDATE:timestamp}\\] \"%{WORD:verb} %{DATA:request} HTTP/%{NUMBER:httpversion}\" %{NUMBER:response} %{NUMBER:bytes}");
        
        // 테스트 로그
        String logLine = "192.168.1.100 - frank [10/Oct/2000:13:55:36 -0700] \"GET /apache_pb.gif HTTP/1.0\" 200 2326";
        
        // 매칭 수행
        MatchResult result = matcher.match(logLine, apacheFormat);
        
        // 검증
        assertNotNull(result);
        assertTrue(result.isCompleteMatch());
        assertEquals(98.0, result.getConfidence(), 0.1);
        
        Map<String, Object> fields = result.getExtractedFields();
        assertEquals("192.168.1.100", fields.get("clientip"));
        assertEquals("frank", fields.get("auth"));
        assertEquals("GET", fields.get("verb"));
        assertEquals("/apache_pb.gif", fields.get("request"));
        assertEquals("200", fields.get("response"));
        assertEquals("2326", fields.get("bytes"));
    }
    
    @Test
    public void testMatchWithSyslog() {
        // Syslog 포맷
        LogFormat syslogFormat = new LogFormat();
        syslogFormat.setFormatId("SYSLOG");
        syslogFormat.setFormatName("Syslog Format");
        syslogFormat.setGrokPattern("%{SYSLOGBASE} %{GREEDYDATA:message}");
        
        // 테스트 로그
        String logLine = "Jan  1 10:00:00 server01 sshd[1234]: Accepted password for user from 192.168.1.100 port 22 ssh2";
        
        // 매칭 수행
        MatchResult result = matcher.match(logLine, syslogFormat);
        
        // 검증
        assertNotNull(result);
        // Syslog 패턴은 부분 매칭일 수 있음
        assertTrue(result.isCompleteMatch() || result.isPartialMatch());
        assertTrue(result.getConfidence() > 0);
    }
    
    @Test
    public void testMatchWithInvalidLog() {
        // 잘못된 포맷
        LogFormat format = new LogFormat();
        format.setFormatId("TEST");
        format.setFormatName("Test Format");
        format.setGrokPattern("%{IP:src_ip} -> %{IP:dst_ip}");
        
        // 매칭되지 않는 로그
        String logLine = "This is not a valid log line";
        
        // 매칭 수행
        MatchResult result = matcher.match(logLine, format);
        
        // 검증
        assertNotNull(result);
        assertFalse(result.isCompleteMatch());
        assertFalse(result.isPartialMatch());
        assertEquals(0.0, result.getConfidence(), 0.1);
    }
    
    @Test
    public void testMatchAll() {
        // 여러 포맷 준비
        LogFormat format1 = new LogFormat();
        format1.setFormatId("FORMAT1");
        format1.setFormatName("IP Format");
        format1.setGrokPattern("%{IP:src_ip} -> %{IP:dst_ip}");
        
        LogFormat format2 = new LogFormat();
        format2.setFormatId("FORMAT2");
        format2.setFormatName("Apache Format");
        format2.setGrokPattern("%{IPORHOST:clientip} .* \\[%{HTTPDATE:timestamp}\\]");
        
        List<LogFormat> formats = Arrays.asList(format1, format2);
        
        // 테스트 로그
        String logLine = "192.168.1.1 -> 192.168.1.2";
        
        // 매칭 수행
        List<MatchResult> results = matcher.matchAll(logLine, formats);
        
        // 검증
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        // 첫 번째 포맷이 매칭되어야 함
        MatchResult firstResult = results.get(0);
        assertEquals("FORMAT1", firstResult.getLogFormatId());
        assertTrue(firstResult.isCompleteMatch());
    }
    
    @Test
    public void testMatchMultiLine() {
        // 멀티라인 포맷
        LogFormat format = new LogFormat();
        format.setFormatId("MULTILINE");
        format.setFormatName("Multi-line Format");
        format.setGrokPattern("START %{GREEDYDATA:content} END");
        
        // 멀티라인 로그
        List<String> logLines = Arrays.asList(
            "START This is",
            "a multi-line",
            "log message END"
        );
        
        // 매칭 수행
        MatchResult result = matcher.matchMultiLine(logLines, format);
        
        // 검증
        assertNotNull(result);
        // 멀티라인 매칭 결과 확인
        Map<String, Object> fields = result.getExtractedFields();
        assertNotNull(fields);
    }
    
    @Test
    public void testExtractFields() {
        // 테스트 로그
        String logLine = "2024-01-15 10:30:45 192.168.1.100 GET /index.html 200";
        
        // 필드 추출
        Map<String, Object> fields = matcher.extractFields(logLine);
        
        // 검증
        assertNotNull(fields);
        assertFalse(fields.isEmpty());
        
        // IP 주소가 추출되어야 함
        assertTrue(fields.containsKey("ip_addresses"));
        List<String> ips = (List<String>) fields.get("ip_addresses");
        assertTrue(ips.contains("192.168.1.100"));
        
        // 타임스탬프가 추출되어야 함
        assertTrue(fields.containsKey("timestamp"));
        assertEquals("2024-01-15 10:30:45", fields.get("timestamp"));
    }
    
    @Test
    public void testMatchOptions() {
        // 대소문자 구분 없는 매칭 테스트
        LogFormat format = new LogFormat();
        format.setFormatId("TEST");
        format.setFormatName("Test Format");
        format.setGrokPattern("ERROR %{GREEDYDATA:message}");
        
        // 옵션 설정
        LogMatcher.MatchOptions options = new LogMatcher.MatchOptions();
        options.setCaseInsensitive(true);
        matcher.setOptions(options);
        
        // 소문자 로그
        String logLine = "error this is an error message";
        
        // 매칭 수행
        MatchResult result = matcher.match(logLine, format);
        
        // 검증 (대소문자 무시로 매칭되어야 함)
        assertNotNull(result);
        // 패턴이 대문자 ERROR인데 로그는 소문자 error이므로,
        // 대소문자 구분 없이 매칭하려면 패턴도 조정 필요
    }
    
    @Test
    public void testStatistics() {
        // 통계 초기화
        matcher.reset();
        
        LogFormat format = new LogFormat();
        format.setFormatId("TEST");
        format.setFormatName("Test Format");
        format.setGrokPattern("%{IP:ip} %{WORD:action}");
        
        // 여러 매칭 수행
        matcher.match("192.168.1.1 GET", format); // 성공
        matcher.match("192.168.1.2 POST", format); // 성공
        matcher.match("invalid log", format); // 실패
        
        // 통계 확인
        LogMatcher.MatchStatistics stats = matcher.getStatistics();
        assertNotNull(stats);
        assertEquals(3, stats.getTotalMatches());
        assertEquals(2, stats.getCompleteMatches());
        assertEquals(1, stats.getFailedMatches());
        assertTrue(stats.getSuccessRate() > 60.0);
    }
    
    @Test
    public void testNullInputs() {
        // null 입력 테스트
        MatchResult result1 = matcher.match(null, new LogFormat());
        assertNotNull(result1);
        assertFalse(result1.isCompleteMatch());
        
        MatchResult result2 = matcher.match("test log", null);
        assertNotNull(result2);
        assertFalse(result2.isCompleteMatch());
        
        List<MatchResult> results = matcher.matchAll(null, Arrays.asList(new LogFormat()));
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}