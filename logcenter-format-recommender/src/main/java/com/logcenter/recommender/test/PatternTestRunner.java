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
 * 모든 패턴의 샘플 로그 매칭을 테스트하는 클래스
 */
public class PatternTestRunner {
    
    private static final String LOG_FORMATS_FILE = "setting_logformat.json";
    
    public static void main(String[] args) {
        PatternTestRunner runner = new PatternTestRunner();
        runner.testAllPatterns();
    }
    
    public void testAllPatterns() {
        System.out.println("=== Grok 패턴 테스트 시작 ===\n");
        
        // Grok 컴파일러 초기화
        GrokCompilerWrapper grokCompiler = new GrokCompilerWrapper();
        grokCompiler.loadStandardPatterns();
        int customLoaded = grokCompiler.loadCustomPatterns();
        System.out.println("커스텀 패턴 로드: " + customLoaded + "개\n");
        
        // JSON 파일 로드
        List<PatternTest> patterns = loadPatterns();
        System.out.println("총 패턴 수: " + patterns.size() + "개\n");
        
        // 결과 통계
        int totalCount = 0;
        int successCount = 0;
        int compileFailCount = 0;
        int matchFailCount = 0;
        List<String> failedPatterns = new ArrayList<>();
        Map<String, Integer> errorTypeCount = new HashMap<>();
        
        // 각 패턴 테스트
        for (PatternTest test : patterns) {
            totalCount++;
            
            try {
                // Grok 패턴 컴파일
                Grok grok = grokCompiler.compileSafe(test.grokExp);
                
                if (grok == null) {
                    compileFailCount++;
                    failedPatterns.add(String.format("[컴파일 실패] %s", test.expName));
                    errorTypeCount.merge("컴파일 실패", 1, Integer::sum);
                    continue;
                }
                
                // 샘플 로그 매칭
                Match match = grok.match(test.sampleLog);
                Map<String, Object> capture = match.capture();
                
                if (capture == null || capture.isEmpty()) {
                    matchFailCount++;
                    failedPatterns.add(String.format("[매칭 실패] %s", test.expName));
                    errorTypeCount.merge("매칭 실패", 1, Integer::sum);
                } else {
                    successCount++;
                }
                
            } catch (Exception e) {
                compileFailCount++;
                failedPatterns.add(String.format("[예외] %s - %s", test.expName, e.getMessage()));
                String errorType = e.getClass().getSimpleName();
                errorTypeCount.merge(errorType, 1, Integer::sum);
            }
            
            // 진행 상황 출력 (10개마다)
            if (totalCount % 10 == 0) {
                System.out.print(".");
                if (totalCount % 100 == 0) {
                    System.out.println(" " + totalCount + "/" + patterns.size());
                }
            }
        }
        
        // 최종 결과 출력
        System.out.println("\n\n=== 테스트 결과 ===");
        System.out.println("총 패턴: " + totalCount + "개");
        System.out.println("성공: " + successCount + "개 (" + (successCount * 100.0 / totalCount) + "%)");
        System.out.println("컴파일 실패: " + compileFailCount + "개");
        System.out.println("매칭 실패: " + matchFailCount + "개");
        
        // 오류 유형별 통계
        System.out.println("\n=== 오류 유형별 통계 ===");
        for (Map.Entry<String, Integer> entry : errorTypeCount.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + "개");
        }
        
        // 실패한 패턴 목록 출력
        System.out.println("\n=== 실패한 패턴 목록 ===");
        int displayCount = Math.min(failedPatterns.size(), 50);
        for (int i = 0; i < displayCount; i++) {
            System.out.println((i + 1) + ". " + failedPatterns.get(i));
        }
        
        if (failedPatterns.size() > 50) {
            System.out.println("... 외 " + (failedPatterns.size() - 50) + "개");
        }
    }
    
    private List<PatternTest> loadPatterns() {
        List<PatternTest> patterns = new ArrayList<>();
        
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
                                PatternTest test = new PatternTest();
                                test.expName = pattern.path("exp_name").asText();
                                test.grokExp = pattern.path("grok_exp").asText();
                                test.sampleLog = pattern.path("samplelog").asText();
                                
                                if (!test.expName.isEmpty() && !test.grokExp.isEmpty() && !test.sampleLog.isEmpty()) {
                                    patterns.add(test);
                                }
                            }
                        }
                    }
                }
            }
            
        } catch (IOException e) {
            throw new RuntimeException("JSON 파일 로드 실패", e);
        }
        
        return patterns;
    }
    
    private static class PatternTest {
        String expName;
        String grokExp;
        String sampleLog;
    }
}