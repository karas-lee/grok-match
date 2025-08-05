package com.logcenter.recommender.grok;

import com.logcenter.recommender.config.AppConfig;
import com.logcenter.recommender.model.GrokPattern;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import io.krakens.grok.api.exception.GrokException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Grok 컴파일러 래퍼 클래스
 * 표준 및 커스텀 Grok 패턴을 로드하고 관리
 */
public class GrokCompilerWrapper {
    
    private static final Logger logger = LoggerFactory.getLogger(GrokCompilerWrapper.class);
    
    private final GrokCompiler compiler;
    private final Map<String, Grok> compiledPatterns;
    private final Map<String, GrokPattern> customPatterns;
    private boolean customPatternsLoaded = false;
    
    /**
     * 기본 생성자
     */
    public GrokCompilerWrapper() {
        this.compiler = GrokCompiler.newInstance();
        this.compiledPatterns = new ConcurrentHashMap<>();
        this.customPatterns = new ConcurrentHashMap<>();
    }
    
    /**
     * 표준 Grok 패턴 로드
     * @return 로드 성공 여부
     */
    public boolean loadStandardPatterns() {
        try {
            // java-grok 라이브러리의 기본 패턴 로드
            compiler.registerDefaultPatterns();
            logger.info("표준 Grok 패턴을 로드했습니다");
            return true;
        } catch (Exception e) {
            logger.error("표준 Grok 패턴 로드 실패", e);
            return false;
        }
    }
    
    /**
     * 커스텀 패턴 로드
     * @return 로드된 패턴 개수
     */
    public int loadCustomPatterns() {
        String customPatternPath = AppConfig.getInstance()
                .getString(AppConfig.CUSTOM_GROK_PATTERNS_PATH);
        
        List<GrokPattern> patterns = CustomPatternLoader.loadCustomPatterns(customPatternPath);
        
        int loadedCount = 0;
        for (GrokPattern pattern : patterns) {
            try {
                // 패턴 등록
                compiler.register(pattern.getName(), pattern.getPattern());
                
                // 패턴 컴파일 및 캐싱
                if (pattern.compile(compiler)) {
                    customPatterns.put(pattern.getName(), pattern);
                    loadedCount++;
                }
            } catch (GrokException e) {
                logger.error("커스텀 패턴 등록 실패: {} - {}", 
                    pattern.getName(), e.getMessage());
            }
        }
        
        customPatternsLoaded = true;
        logger.info("{}개의 커스텀 Grok 패턴을 로드했습니다", loadedCount);
        
        return loadedCount;
    }
    
    /**
     * 패턴 컴파일
     * @param grokExpression Grok 표현식
     * @return 컴파일된 Grok 객체
     * @throws GrokException 컴파일 실패 시
     */
    public Grok compile(String grokExpression) throws GrokException {
        // 캐시 확인
        Grok cached = compiledPatterns.get(grokExpression);
        if (cached != null) {
            return cached;
        }
        
        // 패턴 정규화
        String normalizedExpression = PatternNormalizer.normalize(grokExpression);
        
        // 컴파일 및 캐싱
        Grok grok = compiler.compile(normalizedExpression);
        compiledPatterns.put(grokExpression, grok);
        
        return grok;
    }
    
    
    /**
     * 안전한 컴파일 (예외 처리 포함)
     * @param grokExpression Grok 표현식
     * @return 컴파일된 Grok 객체, 실패 시 null
     */
    public Grok compileSafe(String grokExpression) {
        try {
            return compile(grokExpression);
        } catch (GrokException e) {
            logger.debug("Grok 표현식 컴파일 실패: {} - {}", 
                grokExpression, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.debug("Grok 표현식 컴파일 실패 (기타): {} - {} - {}", 
                grokExpression, e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * 패턴 등록
     * @param name 패턴 이름
     * @param pattern 패턴 정규식
     * @throws GrokException 등록 실패 시
     */
    public void registerPattern(String name, String pattern) throws GrokException {
        compiler.register(name, pattern);
    }
    
    /**
     * 여러 패턴 일괄 등록
     * @param patterns 패턴 맵 (이름 -> 정규식)
     * @return 등록된 패턴 개수
     */
    public int registerPatterns(Map<String, String> patterns) {
        int registered = 0;
        
        for (Map.Entry<String, String> entry : patterns.entrySet()) {
            try {
                registerPattern(entry.getKey(), entry.getValue());
                registered++;
            } catch (GrokException e) {
                logger.error("패턴 등록 실패: {} - {}", 
                    entry.getKey(), e.getMessage());
            }
        }
        
        return registered;
    }
    
    /**
     * 커스텀 패턴 존재 여부 확인
     * @param patternName 패턴 이름
     * @return 존재 여부
     */
    public boolean hasCustomPattern(String patternName) {
        return customPatterns.containsKey(patternName);
    }
    
    /**
     * 커스텀 패턴 가져오기
     * @param patternName 패턴 이름
     * @return GrokPattern 객체, 없으면 null
     */
    public GrokPattern getCustomPattern(String patternName) {
        return customPatterns.get(patternName);
    }
    
    /**
     * 모든 커스텀 패턴 가져오기
     * @return 커스텀 패턴 맵
     */
    public Map<String, GrokPattern> getAllCustomPatterns() {
        return new HashMap<>(customPatterns);
    }
    
    /**
     * 캐시 초기화
     */
    public void clearCache() {
        compiledPatterns.clear();
        logger.info("Grok 패턴 캐시를 초기화했습니다");
    }
    
    /**
     * 캐시 크기 반환
     * @return 캐시된 패턴 개수
     */
    public int getCacheSize() {
        return compiledPatterns.size();
    }
    
    /**
     * 패턴 재로드
     * @return 재로드 성공 여부
     */
    public boolean reloadPatterns() {
        logger.info("Grok 패턴 재로드 시작...");
        
        // 캐시 초기화
        clearCache();
        customPatterns.clear();
        customPatternsLoaded = false;
        
        // 표준 패턴 재로드
        boolean standardLoaded = loadStandardPatterns();
        
        // 커스텀 패턴 재로드
        int customLoaded = loadCustomPatterns();
        
        logger.info("Grok 패턴 재로드 완료 - 표준: {}, 커스텀: {}개", 
            standardLoaded, customLoaded);
        
        return standardLoaded && customLoaded > 0;
    }
    
    /**
     * 초기화 상태 확인
     * @return 초기화 완료 여부
     */
    public boolean isInitialized() {
        return customPatternsLoaded;
    }
    
    /**
     * 통계 정보 반환
     * @return 통계 맵
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("customPatternsLoaded", customPatternsLoaded);
        stats.put("customPatternCount", customPatterns.size());
        stats.put("cacheSize", compiledPatterns.size());
        
        // 카테고리별 통계
        Map<String, Long> categoryStats = new HashMap<>();
        for (GrokPattern pattern : customPatterns.values()) {
            String category = pattern.getCategory();
            categoryStats.merge(category, 1L, Long::sum);
        }
        stats.put("patternsByCategory", categoryStats);
        
        return stats;
    }
}