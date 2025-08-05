package com.logcenter.recommender.grok;

import com.logcenter.recommender.model.GrokPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 커스텀 Grok 패턴 로더
 * custom-grok-patterns 파일에서 233개의 커스텀 패턴을 로드
 */
public class CustomPatternLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomPatternLoader.class);
    
    // 패턴 파싱을 위한 정규식 (패턴명 정규식패턴)
    private static final Pattern PATTERN_LINE = Pattern.compile("^([A-Z][A-Z0-9_]*)\\s+(.+)$");
    
    // 주석 라인 패턴
    private static final Pattern COMMENT_LINE = Pattern.compile("^\\s*#.*$");
    
    // 빈 라인 패턴
    private static final Pattern EMPTY_LINE = Pattern.compile("^\\s*$");
    
    /**
     * 리소스 경로에서 커스텀 패턴 파일 로드
     * @param resourcePath 리소스 경로 (기본값: custom-grok-patterns)
     * @return 로드된 GrokPattern 리스트
     */
    public static List<GrokPattern> loadCustomPatterns(String resourcePath) {
        List<GrokPattern> patterns = new ArrayList<>();
        
        try (InputStream inputStream = CustomPatternLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            
            if (inputStream == null) {
                logger.error("커스텀 패턴 파일을 찾을 수 없습니다: {}", resourcePath);
                return patterns;
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                
                String line;
                int lineNumber = 0;
                
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    
                    // 주석이나 빈 라인 건너뛰기
                    if (COMMENT_LINE.matcher(line).matches() || 
                        EMPTY_LINE.matcher(line).matches()) {
                        continue;
                    }
                    
                    // 패턴 파싱
                    Matcher matcher = PATTERN_LINE.matcher(line);
                    if (matcher.matches()) {
                        String patternName = matcher.group(1);
                        String patternRegex = matcher.group(2);
                        
                        GrokPattern pattern = new GrokPattern(patternName, patternRegex, "CUSTOM");
                        pattern.categorize(); // 카테고리 자동 분류
                        patterns.add(pattern);
                        
                        logger.debug("커스텀 패턴 로드: {} - {}", patternName, pattern.getCategory());
                    } else {
                        logger.warn("잘못된 패턴 형식 (라인 {}): {}", lineNumber, line);
                    }
                }
                
                logger.info("{}개의 커스텀 Grok 패턴을 로드했습니다", patterns.size());
                
            }
        } catch (IOException e) {
            logger.error("커스텀 패턴 파일 읽기 오류", e);
        }
        
        return patterns;
    }
    
    /**
     * 기본 경로에서 커스텀 패턴 로드
     * @return 로드된 GrokPattern 리스트
     */
    public static List<GrokPattern> loadCustomPatterns() {
        return loadCustomPatterns("custom-grok-patterns");
    }
    
    /**
     * 카테고리별로 패턴 그룹화
     * @param patterns 패턴 리스트
     * @return 카테고리별로 그룹화된 맵
     */
    public static Map<String, List<GrokPattern>> groupByCategory(List<GrokPattern> patterns) {
        Map<String, List<GrokPattern>> grouped = new HashMap<>();
        
        for (GrokPattern pattern : patterns) {
            String category = pattern.getCategory();
            if (category == null) {
                category = "UNKNOWN";
            }
            
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(pattern);
        }
        
        return grouped;
    }
    
    /**
     * 패턴 이름으로 검색
     * @param patterns 패턴 리스트
     * @param name 검색할 패턴 이름
     * @return 찾은 패턴, 없으면 null
     */
    public static GrokPattern findByName(List<GrokPattern> patterns, String name) {
        return patterns.stream()
            .filter(p -> p.getName().equals(name))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 패턴 통계 정보 출력
     * @param patterns 패턴 리스트
     */
    public static void printStatistics(List<GrokPattern> patterns) {
        Map<String, List<GrokPattern>> grouped = groupByCategory(patterns);
        
        System.out.println("=== 커스텀 Grok 패턴 통계 ===");
        System.out.println("총 패턴 수: " + patterns.size());
        System.out.println("\n카테고리별 분포:");
        
        grouped.forEach((category, list) -> {
            System.out.printf("  %-20s: %3d개\n", category, list.size());
        });
        
        System.out.println("=============================");
    }
    
    /**
     * 패턴 검증
     * @param patterns 검증할 패턴 리스트
     * @return 유효한 패턴 리스트
     */
    public static List<GrokPattern> validatePatterns(List<GrokPattern> patterns) {
        List<GrokPattern> validPatterns = new ArrayList<>();
        
        for (GrokPattern pattern : patterns) {
            try {
                // 정규식 컴파일 테스트
                Pattern.compile(pattern.getPattern());
                validPatterns.add(pattern);
            } catch (Exception e) {
                logger.error("잘못된 정규식 패턴: {} - {}", pattern.getName(), e.getMessage());
            }
        }
        
        int invalidCount = patterns.size() - validPatterns.size();
        if (invalidCount > 0) {
            logger.warn("{}개의 잘못된 패턴이 제거되었습니다", invalidCount);
        }
        
        return validPatterns;
    }
    
    /**
     * JAR 파일 내부에서도 작동하는 리소스 로더
     * @param resourcePath 리소스 경로
     * @return InputStream
     */
    private static InputStream getResourceAsStream(String resourcePath) {
        // 먼저 클래스로더로 시도
        InputStream stream = CustomPatternLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath);
        
        // 실패하면 현재 클래스 기준으로 시도
        if (stream == null) {
            stream = CustomPatternLoader.class.getResourceAsStream("/" + resourcePath);
        }
        
        return stream;
    }
}