package com.logcenter.recommender.api.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 캐시 관리자
 * Caffeine 캐시를 사용하여 API 응답을 캐싱
 */
public class CacheManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    
    // 캐시 이름별 캐시 인스턴스 관리
    private final Map<String, Cache<String, Object>> caches = new ConcurrentHashMap<>();
    
    // 기본 캐시 설정
    private static final long DEFAULT_MAX_SIZE = 1000;
    private static final long DEFAULT_EXPIRE_MINUTES = 60;
    
    // 싱글톤 인스턴스
    private static CacheManager instance;
    
    private CacheManager() {
        // 기본 캐시 초기화
        initializeDefaultCaches();
    }
    
    /**
     * 싱글톤 인스턴스 반환
     */
    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }
    
    /**
     * 기본 캐시 초기화
     */
    private void initializeDefaultCaches() {
        // 로그 포맷 목록 캐시 (24시간)
        createCache("logFormats", 100, Duration.ofHours(24));
        
        // API 응답 캐시 (1시간)
        createCache("apiResponses", 1000, Duration.ofHours(1));
        
        // 추천 결과 캐시 (30분)
        createCache("recommendations", 500, Duration.ofMinutes(30));
    }
    
    /**
     * 새로운 캐시 생성
     */
    public void createCache(String name, long maxSize, Duration expireAfter) {
        Cache<String, Object> cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireAfter)
                .recordStats()  // 통계 기록 활성화
                .removalListener((String key, Object value, RemovalCause cause) -> {
                    logger.debug("캐시 항목 제거: {} - {}, 원인: {}", name, key, cause);
                })
                .build();
        
        caches.put(name, cache);
        logger.info("캐시 생성: {} (최대 크기: {}, 만료 시간: {})", 
                name, maxSize, expireAfter);
    }
    
    /**
     * 캐시에서 값 조회
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String cacheName, String key) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache == null) {
            logger.warn("존재하지 않는 캐시: {}", cacheName);
            return null;
        }
        
        return (T) cache.getIfPresent(key);
    }
    
    /**
     * 캐시에 값 저장
     */
    public void put(String cacheName, String key, Object value) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache == null) {
            logger.warn("존재하지 않는 캐시: {}", cacheName);
            return;
        }
        
        cache.put(key, value);
        logger.debug("캐시 저장: {} - {}", cacheName, key);
    }
    
    /**
     * 캐시에서 값 제거
     */
    public void evict(String cacheName, String key) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache == null) {
            logger.warn("존재하지 않는 캐시: {}", cacheName);
            return;
        }
        
        cache.invalidate(key);
        logger.debug("캐시 제거: {} - {}", cacheName, key);
    }
    
    /**
     * 특정 캐시 전체 초기화
     */
    public void clearCache(String cacheName) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache == null) {
            logger.warn("존재하지 않는 캐시: {}", cacheName);
            return;
        }
        
        cache.invalidateAll();
        logger.info("캐시 초기화: {}", cacheName);
    }
    
    /**
     * 모든 캐시 초기화
     */
    public void clearAll() {
        caches.forEach((name, cache) -> {
            cache.invalidateAll();
            logger.info("캐시 초기화: {}", name);
        });
    }
    
    /**
     * 캐시 통계 정보
     */
    public CacheStats getStats(String cacheName) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache == null) {
            return null;
        }
        
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = cache.stats();
        return new CacheStats(
                cacheName,
                cache.estimatedSize(),
                stats.hitCount(),
                stats.missCount(),
                stats.loadSuccessCount(),
                stats.loadFailureCount(),
                stats.evictionCount()
        );
    }
    
    /**
     * 캐시 통계 정보 클래스
     */
    public static class CacheStats {
        private final String name;
        private final long size;
        private final long hitCount;
        private final long missCount;
        private final long loadSuccessCount;
        private final long loadFailureCount;
        private final long evictionCount;
        
        public CacheStats(String name, long size, long hitCount, long missCount,
                         long loadSuccessCount, long loadFailureCount, long evictionCount) {
            this.name = name;
            this.size = size;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.loadSuccessCount = loadSuccessCount;
            this.loadFailureCount = loadFailureCount;
            this.evictionCount = evictionCount;
        }
        
        public double getHitRate() {
            long totalRequests = hitCount + missCount;
            return totalRequests == 0 ? 0.0 : (double) hitCount / totalRequests;
        }
        
        // Getters
        public String getName() { return name; }
        public long getSize() { return size; }
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public long getLoadSuccessCount() { return loadSuccessCount; }
        public long getLoadFailureCount() { return loadFailureCount; }
        public long getEvictionCount() { return evictionCount; }
        
        @Override
        public String toString() {
            return String.format("CacheStats{name='%s', size=%d, hitRate=%.2f%%, " +
                    "hits=%d, misses=%d, evictions=%d}",
                    name, size, getHitRate() * 100, hitCount, missCount, evictionCount);
        }
    }
}