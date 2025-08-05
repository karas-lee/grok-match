package com.logcenter.recommender.test;

import com.logcenter.recommender.grok.GrokCompilerWrapper;
import com.logcenter.recommender.matcher.SimpleLogMatcher;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.model.MatchResult;

import java.util.Map;

/**
 * 패턴 매칭 테스트를 위한 간단한 실행 클래스
 */
public class TestPatternMatcher {
    
    public static void main(String[] args) {
        GrokCompilerWrapper grokCompiler = new GrokCompilerWrapper();
        grokCompiler.loadStandardPatterns();
        grokCompiler.loadCustomPatterns();
        
        SimpleLogMatcher matcher = new SimpleLogMatcher(grokCompiler);
        
        // SSH 로그 테스트
        String logLine = "20191015175852 <86>Oct  4 14:14:53 logcenter sshd[8536]: Failed password for root from 192.168.1.173 port 49364 ssh2";
        
        LogFormat logFormat = new LogFormat();
        logFormat.setFormatId("test");
        logFormat.setFormatName("Test");
        
        // 원본 패턴
        String originalPattern = "%{DATE_FORMAT18:event_time} %{SKIP}%{DATE_FORMAT3} %{DATA:hostname} %{WORD:program}\\[%{INT:pid}\\]: Failed password for %{DATA:user} from %{IP:src_ip} port %{INT:src_port} %{SKIP}";
        
        System.out.println("로그: " + logLine);
        System.out.println("\n원본 패턴 테스트:");
        System.out.println("패턴: " + originalPattern);
        
        logFormat.setGrokPattern(originalPattern);
        MatchResult result = matcher.match(logLine, logFormat);
        
        System.out.println("매칭 성공: " + (result.isCompleteMatch() || result.isPartialMatch()));
        if (result.isCompleteMatch() || result.isPartialMatch()) {
            System.out.println("추출된 필드: " + result.getExtractedFields());
        }
        
        // 단순화된 패턴들 테스트
        String[] testPatterns = {
            "%{DATE_FORMAT18:event_time}",
            "%{DATE_FORMAT18:event_time} <86>Oct  4 14:14:53",
            "%{DATE_FORMAT18:event_time} <86>%{DATE_FORMAT3:date}",
            "%{DATE_FORMAT18:event_time} %{DATA:prefix}%{DATE_FORMAT3:date}",
            "%{DATE_FORMAT18:event_time} .*Oct  4 14:14:53"
        };
        
        System.out.println("\n\n단계별 패턴 테스트:");
        for (String pattern : testPatterns) {
            System.out.println("\n패턴: " + pattern);
            logFormat.setGrokPattern(pattern);
            result = matcher.match(logLine, logFormat);
            System.out.println("매칭 성공: " + (result.isCompleteMatch() || result.isPartialMatch()));
            if (result.isCompleteMatch() || result.isPartialMatch()) {
                System.out.println("추출된 필드: " + result.getExtractedFields());
            }
        }
    }
}