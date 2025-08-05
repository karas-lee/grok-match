package com.logcenter.recommender.grok;

import com.logcenter.recommender.cache.PersistentCacheManager;
import com.logcenter.recommender.config.AppConfig;
import com.logcenter.recommender.model.LogFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 캐시 기능이 추가된 패턴 저장소
 * 로그 포맷 데이터를 영구 캐시에 저장하여 로딩 시간 단축
 */
public class CachedPatternRepository extends FilePatternRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(CachedPatternRepository.class);
    
    private final PersistentCacheManager cacheManager;
    private final String logFormatsPath;
    
    /**
     * 생성자
     */
    public CachedPatternRepository(PersistentCacheManager cacheManager) {
        super();
        this.cacheManager = cacheManager;
        this.logFormatsPath = AppConfig.getInstance()
                .getString(AppConfig.LOG_FORMATS_PATH, "setting_logformat.json");
    }
    
    /**
     * 초기화 (캐시 활용)
     * @return 초기화 성공 여부
     */
    @Override
    public boolean initialize() {
        if (cacheManager == null || !cacheManager.isEnabled()) {
            // 캐시가 비활성화된 경우 기본 초기화
            return super.initialize();
        }
        
        try {
            // 1. 캐시에서 로드 시도
            List<LogFormat> cachedFormats = cacheManager.loadLogFormats();
            
            // 2. 캐시가 유효한지 확인
            if (cachedFormats != null && 
                cacheManager.isResourceCacheValid(logFormatsPath)) {
                
                logger.info("캐시에서 로그 포맷을 로드합니다");
                
                // 캐시된 포맷 설정
                boolean success = setLogFormats(cachedFormats);
                
                if (success) {
                    logger.info("캐시에서 {}개의 로그 포맷을 로드했습니다", cachedFormats.size());
                    return true;
                }
            }
            
            // 3. 캐시 미스 - 일반 초기화
            logger.info("캐시가 없거나 유효하지 않아 파일에서 로드합니다");
            boolean success = super.initialize();
            
            // 4. 초기화 성공 시 캐시에 저장
            if (success) {
                List<LogFormat> formats = getAllFormats();
                cacheManager.saveLogFormats(formats);
                cacheManager.saveResourceChecksum(logFormatsPath);
                logger.info("로그 포맷을 캐시에 저장했습니다");
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("캐시 로드 중 오류 발생, 일반 초기화로 대체", e);
            return super.initialize();
        }
    }
    
    /**
     * 포맷 재로드 (캐시 갱신 포함)
     * @return 로드된 포맷 개수
     */
    @Override
    public int reloadFormats() {
        int count = super.reloadFormats();
        
        // 재로드 성공 시 캐시 갱신
        if (count > 0 && cacheManager != null && cacheManager.isEnabled()) {
            try {
                List<LogFormat> formats = getAllFormats();
                cacheManager.saveLogFormats(formats);
                cacheManager.saveResourceChecksum(logFormatsPath);
                logger.info("재로드된 로그 포맷을 캐시에 저장했습니다");
            } catch (Exception e) {
                logger.error("캐시 저장 실패", e);
            }
        }
        
        return count;
    }
    
    /**
     * 캐시 무효화
     */
    public void invalidateCache() {
        if (cacheManager != null && cacheManager.isEnabled()) {
            cacheManager.invalidate();
            logger.info("로그 포맷 캐시를 무효화했습니다");
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
        
        logger.info("=== 로그 포맷 캐시 통계 ===");
        logger.info("캐시 디렉토리: {}", cacheManager.getCacheDir());
        logger.info("로그 포맷 캐시 유효: {}", 
            cacheManager.isResourceCacheValid(logFormatsPath));
        logger.info("========================");
    }
    
    /**
     * 캐시 관리자 반환
     */
    public PersistentCacheManager getCacheManager() {
        return cacheManager;
    }
}