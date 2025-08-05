package com.logcenter.recommender.matcher;

import com.logcenter.recommender.grok.GrokCompilerWrapper;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.model.MatchResult;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * 패턴 디버깅 테스트
 */
public class DebugPatternTest {
    
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
    public void testDebugSSHPattern() {
        // Given - SSH 로그 예제
        String logLine = "20191015175852 <86>Oct  4 14:14:53 logcenter sshd[8536]: Failed password for root from 192.168.1.173 port 49364 ssh2";
        
        // 단순한 패턴부터 시작
        LogFormat logFormat = new LogFormat();
        logFormat.setFormatId("debug");
        logFormat.setFormatName("Debug");
        
        // 패턴을 단계적으로 테스트
        String[] patterns = {
            "%{DATE_FORMAT18:event_time}",
            "%{DATE_FORMAT18:event_time} %{SKIP}",
            "%{DATE_FORMAT18:event_time} %{SKIP}%{DATE_FORMAT3:date}",
            "%{DATE_FORMAT18:event_time} <86>%{DATE_FORMAT3:date}",
            "%{DATE_FORMAT18:event_time} %{DATA:skip1}%{DATE_FORMAT3:date}"
        };
        
        for (String pattern : patterns) {
            logFormat.setGrokPattern(pattern);
            MatchResult result = simpleMatcher.match(logLine, logFormat);
            
            System.out.println("\n패턴: " + pattern);
            System.out.println("매칭 성공: " + (result.isCompleteMatch() || result.isPartialMatch()));
            
            if (result.isCompleteMatch() || result.isPartialMatch()) {
                Map<String, Object> fields = result.getExtractedFields();
                System.out.println("추출된 필드: " + fields);
            }
        }
    }
    
    @Test
    public void testSimpleSkipPattern() {
        // SKIP 패턴 자체 테스트
        String logLine = "test <86>data";
        
        LogFormat logFormat = new LogFormat();
        logFormat.setFormatId("skip_test");
        logFormat.setFormatName("Skip Test");
        logFormat.setGrokPattern("%{WORD:word1} %{SKIP}%{WORD:word2}");
        
        MatchResult result = simpleMatcher.match(logLine, logFormat);
        
        System.out.println("\n로그: " + logLine);
        System.out.println("패턴: " + logFormat.getGrokPattern());
        System.out.println("매칭 성공: " + (result.isCompleteMatch() || result.isPartialMatch()));
        
        if (result.isCompleteMatch() || result.isPartialMatch()) {
            Map<String, Object> fields = result.getExtractedFields();
            System.out.println("추출된 필드: " + fields);
        }
    }
}