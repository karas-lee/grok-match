package com.logcenter.recommender.grok;

import com.logcenter.recommender.cache.PersistentCacheManager;
import com.logcenter.recommender.config.AppConfig;
import com.logcenter.recommender.model.GrokPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 캐시 기능이 추가된 Grok 컴파일러 래퍼
 * 영구 캐시를 활용하여 초기화 시간을 단축
 */
public class CachedGrokCompilerWrapper extends GrokCompilerWrapper {
    
    private static final Logger logger = LoggerFactory.getLogger(CachedGrokCompilerWrapper.class);
    
    private final PersistentCacheManager cacheManager;
    private final String customPatternPath;
    
    /**
     * 생성자
     */
    public CachedGrokCompilerWrapper(PersistentCacheManager cacheManager) {
        super();
        this.cacheManager = cacheManager;
        this.customPatternPath = AppConfig.getInstance()
                .getString(AppConfig.CUSTOM_GROK_PATTERNS_PATH);
    }
    
    /**
     * 커스텀 패턴 로드 (캐시 활용)
     * @return 로드된 패턴 개수
     */
    @Override
    public int loadCustomPatterns() {
        if (cacheManager == null || !cacheManager.isEnabled()) {
            // 캐시가 비활성화된 경우 기본 로드
            return super.loadCustomPatterns();
        }
        
        try {
            // 1. 캐시에서 로드 시도
            Map<String, GrokPattern> cachedPatterns = cacheManager.loadCustomPatterns();
            
            // 2. 캐시가 유효한지 확인
            if (cachedPatterns != null && 
                cacheManager.isResourceCacheValid(customPatternPath)) {
                
                logger.info("캐시에서 커스텀 패턴을 로드합니다");
                
                // 캐시된 패턴 등록
                int loadedCount = registerCachedPatterns(cachedPatterns);
                
                if (loadedCount > 0) {
                    logger.info("캐시에서 {}개의 커스텀 패턴을 로드했습니다", loadedCount);
                    return loadedCount;
                }
            }
            
            // 3. 캐시 미스 - 일반 로드
            logger.info("캐시가 없거나 유효하지 않아 파일에서 로드합니다");
            int loadedCount = super.loadCustomPatterns();
            
            // 4. 로드 성공 시 캐시에 저장
            if (loadedCount > 0) {
                Map<String, GrokPattern> patterns = getAllCustomPatterns();
                cacheManager.saveCustomPatterns(patterns);
                cacheManager.saveResourceChecksum(customPatternPath);
                logger.info("커스텀 패턴을 캐시에 저장했습니다");
            }
            
            return loadedCount;
            
        } catch (Exception e) {
            logger.error("캐시 로드 중 오류 발생, 일반 로드로 대체", e);
            return super.loadCustomPatterns();
        }
    }
    
    /**
     * 캐시된 패턴 등록
     */
    private int registerCachedPatterns(Map<String, GrokPattern> patterns) {
        int loadedCount = 0;
        
        for (Map.Entry<String, GrokPattern> entry : patterns.entrySet()) {
            try {
                GrokPattern pattern = entry.getValue();
                
                // 컴파일러에 패턴 등록
                getCompiler().register(pattern.getName(), pattern.getPattern());
                
                // 패턴 컴파일 및 저장
                if (pattern.compile(getCompiler())) {
                    getCustomPatterns().put(pattern.getName(), pattern);
                    loadedCount++;
                }
                
            } catch (Exception e) {
                logger.error("캐시된 패턴 등록 실패: {}", entry.getKey(), e);
            }
        }
        
        setCustomPatternsLoaded(true);
        return loadedCount;
    }
    
    /**
     * 캐시 관리자 반환
     */
    public PersistentCacheManager getCacheManager() {
        return cacheManager;
    }
    
    /**
     * 캐시 무효화
     */
    public void invalidateCache() {
        if (cacheManager != null && cacheManager.isEnabled()) {
            cacheManager.invalidate();
            logger.info("Grok 패턴 캐시를 무효화했습니다");
        }
    }
    
    /**
     * 캐시 통계 정보
     */
    public void printCacheStats() {
        if (cacheManager == null || !cacheManager.isEnabled()) {
            logger.info("캐시가 비활성화되어 있습니다");
            return;
        }
        
        logger.info("=== Grok 패턴 캐시 통계 ===");
        logger.info("캐시 디렉토리: {}", cacheManager.getCacheDir());
        logger.info("커스텀 패턴 캐시 유효: {}", 
            cacheManager.isResourceCacheValid(customPatternPath));
        logger.info("========================");
    }
}