package com.logcenter.recommender.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logcenter.recommender.grok.GrokCompilerWrapper;
import com.logcenter.recommender.util.JacksonJsonUtils;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.Match;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 특정 패턴에 대한 상세 테스트
 */
public class DetailedPatternTester {
    
    private static final String LOG_FORMATS_FILE = "setting_logformat.json";
    
    public static void main(String[] args) {
        DetailedPatternTester tester = new DetailedPatternTester();
        
        // 문제가 있는 패턴들을 상세 테스트
        String[] problemPatterns = {
            "SECUI_BLUEMAX_NGF_1.00_CSV_1",
            "TREND_MICRO_TIPPINGPOINT_1.00_1", 
            "NGINX_NGINX_1.00_1",
            "MYSQL_MYSQL_1.00_1"
        };
        
        for (String patternName : problemPatterns) {
            tester.testPattern(patternName);
            System.out.println("\n" + "=".repeat(100) + "\n");
        }
    }
    
    public void testPattern(String targetPattern) {
        System.out.println("=== " + targetPattern + " 패턴 상세 테스트 ===\n");
        
        // Grok 컴파일러 초기화
        GrokCompilerWrapper grokCompiler = new GrokCompilerWrapper();
        grokCompiler.loadStandardPatterns();
        grokCompiler.loadCustomPatterns();
        
        // 패턴 로드
        PatternInfo pattern = loadPattern(targetPattern);
        if (pattern == null) {
            System.out.println("패턴을 찾을 수 없습니다: " + targetPattern);
            return;
        }
        
        System.out.println("Grok 표현식:");
        System.out.println(pattern.grokExp);
        System.out.println("\n샘플 로그:");
        System.out.println(pattern.sampleLog);
        System.out.println();
        
        // 컴파일 시도
        try {
            Grok grok = grokCompiler.compile(pattern.grokExp);
            System.out.println("✓ 패턴 컴파일 성공\n");
            
            // 매칭 시도
            Match match = grok.match(pattern.sampleLog);
            Map<String, Object> capture = match.capture();
            
            if (capture == null || capture.isEmpty()) {
                System.out.println("✗ 매칭 실패: 캡처된 필드가 없습니다\n");
                
                // 부분 매칭 테스트
                System.out.println("부분 매칭 테스트:");
                testPartialMatching(grokCompiler, pattern);
            } else {
                System.out.println("✓ 매칭 성공!\n");
                System.out.println("캡처된 필드:");
                for (Map.Entry<String, Object> entry : capture.entrySet()) {
                    if (entry.getValue() != null) {
                        System.out.printf("  %s: %s\n", entry.getKey(), entry.getValue());
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("✗ 컴파일 실패: " + e.getMessage());
            System.out.println("예외 타입: " + e.getClass().getSimpleName());
            
            // 정규화된 패턴 출력
            String normalized = com.logcenter.recommender.grok.PatternNormalizer.normalize(pattern.grokExp);
            System.out.println("\n정규화된 패턴:");
            System.out.println(normalized);
            
            // 문제 부분 식별
            identifyProblem(pattern.grokExp, e.getMessage());
        }
    }
    
    private void testPartialMatching(GrokCompilerWrapper grokCompiler, PatternInfo pattern) {
        // 패턴을 단순화해서 테스트
        String[] simplifiedPatterns = {
            "^%{TEXT1:log_time}.*",
            "^%{TEXT1:log_time}\\s<%{TEXT17:pri}>.*",
            "^.*\\[fw4_deny\\].*"
        };
        
        for (String simplePattern : simplifiedPatterns) {
            try {
                Grok grok = grokCompiler.compile(simplePattern);
                Match match = grok.match(pattern.sampleLog);
                Map<String, Object> capture = match.capture();
                
                if (capture != null && !capture.isEmpty()) {
                    System.out.println("  ✓ 부분 매칭 성공: " + simplePattern);
                } else {
                    System.out.println("  ✗ 부분 매칭 실패: " + simplePattern);
                }
            } catch (Exception e) {
                System.out.println("  ✗ 부분 패턴 컴파일 실패: " + simplePattern);
            }
        }
    }
    
    private void identifyProblem(String grokExp, String errorMsg) {
        System.out.println("\n문제 분석:");
        
        // 에러 메시지에서 인덱스 추출
        if (errorMsg.contains("near index")) {
            try {
                int index = Integer.parseInt(errorMsg.substring(errorMsg.lastIndexOf(" ") + 1).trim());
                System.out.println("오류 위치 (인덱스 " + index + "):");
                
                if (index > 0 && index < grokExp.length()) {
                    int start = Math.max(0, index - 20);
                    int end = Math.min(grokExp.length(), index + 20);
                    System.out.println("..." + grokExp.substring(start, end) + "...");
                    System.out.println(" ".repeat(index - start + 3) + "^");
                }
            } catch (Exception e) {
                // 인덱스 파싱 실패
            }
        }
        
        // 일반적인 문제 패턴 확인
        if (grokExp.contains("[(?<log_name>")) {
            System.out.println("• 잘못된 named group in bracket 패턴 발견");
        }
        if (grokExp.contains("[^\\[]*\\[")) {
            System.out.println("• 이스케이프 문제 가능성");
        }
    }
    
    private PatternInfo loadPattern(String targetPattern) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(LOG_FORMATS_FILE)) {
            if (is == null) {
                throw new RuntimeException("리소스 파일을 찾을 수 없습니다: " + LOG_FORMATS_FILE);
            }
            
            ObjectMapper mapper = JacksonJsonUtils.getMapper();
            JsonNode root = mapper.readTree(is);
            
            // JSON 배열 순회
            for (JsonNode vendor : root) {
                JsonNode logTypes = vendor.get("log_type");
                if (logTypes != null && logTypes.isArray()) {
                    for (JsonNode logType : logTypes) {
                        JsonNode patternsNode = logType.get("patterns");
                        if (patternsNode != null && patternsNode.isArray()) {
                            for (JsonNode pattern : patternsNode) {
                                String expName = pattern.path("exp_name").asText();
                                if (targetPattern.equals(expName)) {
                                    PatternInfo info = new PatternInfo();
                                    info.expName = expName;
                                    info.grokExp = pattern.path("grok_exp").asText();
                                    info.sampleLog = pattern.path("samplelog").asText();
                                    return info;
                                }
                            }
                        }
                    }
                }
            }
            
        } catch (IOException e) {
            throw new RuntimeException("JSON 파일 로드 실패", e);
        }
        
        return null;
    }
    
    private static class PatternInfo {
        String expName;
        String grokExp;
        String sampleLog;
    }
}