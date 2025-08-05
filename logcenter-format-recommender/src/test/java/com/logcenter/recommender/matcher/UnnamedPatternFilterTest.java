package com.logcenter.recommender.matcher;

import com.logcenter.recommender.grok.GrokCompilerWrapper;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.model.MatchResult;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unnamed 패턴 필터링 테스트
 */
public class UnnamedPatternFilterTest {
    
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
    public void testUnnamedPatternFiltering() {
        // Given - SSH 로그 예제
        String logLine = "20191015175852 <86>Oct  4 14:14:53 logcenter sshd[8536]: Failed password for root from 192.168.1.173 port 49364 ssh2";
        
        LogFormat logFormat = new LogFormat();
        logFormat.setFormatId("linux_sshd");
        logFormat.setFormatName("Linux SSHD");
        // SKIP과 DATE_FORMAT3는 필드명이 없으므로 필터링되어야 함
        logFormat.setGrokPattern("%{DATE_FORMAT18:event_time} %{SKIP}%{DATE_FORMAT3} %{DATA:hostname} %{WORD:program}\\[%{INT:pid}\\]: Failed password for %{DATA:user} from %{IP:src_ip} port %{INT:src_port} %{SKIP}");
        
        // When
        MatchResult result = simpleMatcher.match(logLine, logFormat);
        
        // Then
        assertNotNull("결과가 null이 아니어야 함", result);
        assertTrue("매칭이 성공해야 함", result.isCompleteMatch() || result.isPartialMatch());
        
        Map<String, Object> fields = result.getExtractedFields();
        assertNotNull("필드가 null이 아니어야 함", fields);
        
        // 필드명이 지정된 것들만 있어야 함
        assertTrue("event_time 필드가 있어야 함", fields.containsKey("event_time"));
        assertTrue("hostname 필드가 있어야 함", fields.containsKey("hostname"));
        assertTrue("program 필드가 있어야 함", fields.containsKey("program"));
        assertTrue("pid 필드가 있어야 함", fields.containsKey("pid"));
        assertTrue("user 필드가 있어야 함", fields.containsKey("user"));
        assertTrue("src_ip 필드가 있어야 함", fields.containsKey("src_ip"));
        assertTrue("src_port 필드가 있어야 함", fields.containsKey("src_port"));
        
        // 필드명이 없는 것들은 없어야 함
        assertFalse("SKIP 필드는 없어야 함", fields.containsKey("SKIP"));
        assertFalse("DATE_FORMAT3 필드는 없어야 함", fields.containsKey("DATE_FORMAT3"));
        
        // 숫자로만 된 키도 없어야 함
        for (String key : fields.keySet()) {
            assertFalse("숫자로만 된 키는 없어야 함: " + key, key.matches("^\\d+$"));
        }
        
        System.out.println("필터링된 필드: " + fields);
    }
    
    @Test
    public void testMixedPatternFiltering() {
        // Given - 다양한 패턴이 섞인 경우
        String logLine = "192.168.1.100 - - [04/Oct/2023:14:14:53 +0900] \"GET /index.html HTTP/1.1\" 200 1234";
        
        LogFormat logFormat = new LogFormat();
        logFormat.setFormatId("test_mixed");
        logFormat.setFormatName("Test Mixed");
        // IP와 DATA는 필드명이 있고, WORD는 필드명이 없음
        logFormat.setGrokPattern("%{IP:client_ip} %{DATA:ident} %{DATA:auth} \\[%{DATA:timestamp}\\] \"%{WORD} %{DATA:uri} %{DATA:protocol}\" %{NUMBER:status} %{NUMBER:bytes}");
        
        // When
        MatchResult result = simpleMatcher.match(logLine, logFormat);
        
        // Then
        assertNotNull("결과가 null이 아니어야 함", result);
        
        Map<String, Object> fields = result.getExtractedFields();
        assertNotNull("필드가 null이 아니어야 함", fields);
        
        // 필드명이 지정된 것들만 있어야 함
        assertTrue("client_ip 필드가 있어야 함", fields.containsKey("client_ip"));
        assertTrue("ident 필드가 있어야 함", fields.containsKey("ident"));
        assertTrue("auth 필드가 있어야 함", fields.containsKey("auth"));
        assertTrue("timestamp 필드가 있어야 함", fields.containsKey("timestamp"));
        assertTrue("uri 필드가 있어야 함", fields.containsKey("uri"));
        assertTrue("protocol 필드가 있어야 함", fields.containsKey("protocol"));
        assertTrue("status 필드가 있어야 함", fields.containsKey("status"));
        assertTrue("bytes 필드가 있어야 함", fields.containsKey("bytes"));
        
        // 필드명이 없는 WORD는 없어야 함
        assertFalse("WORD 필드는 없어야 함", fields.containsKey("WORD"));
        
        System.out.println("필터링된 필드: " + fields);
    }
}