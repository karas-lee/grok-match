package com.logcenter.recommender.service;

import com.logcenter.recommender.model.FormatRecommendation;
import com.logcenter.recommender.model.LogFormat;
import java.util.List;
import java.util.Map;

/**
 * 로그 포맷 추천 서비스 인터페이스
 * 로그 샘플을 분석하여 가장 적합한 로그 포맷을 추천
 */
public interface LogFormatRecommender {
    
    /**
     * 단일 로그 샘플에 대한 포맷 추천
     * @param logSample 로그 샘플
     * @return 추천 결과 리스트 (신뢰도 순 정렬)
     */
    List<FormatRecommendation> recommend(String logSample);
    
    /**
     * 여러 로그 샘플에 대한 포맷 추천
     * @param logSamples 로그 샘플 리스트
     * @return 추천 결과 리스트 (신뢰도 순 정렬)
     */
    List<FormatRecommendation> recommendBatch(List<String> logSamples);
    
    /**
     * 특정 그룹 내에서만 포맷 추천
     * @param logSample 로그 샘플
     * @param groupName 그룹명 (예: FIREWALL, WEBSERVER)
     * @return 추천 결과 리스트
     */
    List<FormatRecommendation> recommendInGroup(String logSample, String groupName);
    
    /**
     * 추천 옵션 설정
     * @param options 추천 옵션
     */
    void setOptions(RecommendOptions options);
    
    /**
     * 사용 가능한 로그 포맷 목록 조회
     * @return 로그 포맷 리스트
     */
    List<LogFormat> getAvailableFormats();
    
    /**
     * 특정 그룹의 로그 포맷 목록 조회
     * @param groupName 그룹명
     * @return 로그 포맷 리스트
     */
    List<LogFormat> getFormatsByGroup(String groupName);
    
    /**
     * 추천 서비스 초기화
     * @return 초기화 성공 여부
     */
    boolean initialize();
    
    /**
     * 포맷 데이터 재로드
     * @return 재로드된 포맷 개수
     */
    int reloadFormats();
    
    /**
     * 추천 옵션을 포함한 추천
     * @param logSample 로그 샘플
     * @param options 추천 옵션
     * @return 추천 결과 리스트
     */
    List<FormatRecommendation> recommend(String logSample, RecommendOptions options);
    
    /**
     * 배치 추천 (배치별 결과 반환)
     * @param logSamples 로그 샘플 리스트
     * @param options 추천 옵션
     * @return 각 로그 샘플별 추천 결과 리스트
     */
    List<List<FormatRecommendation>> recommendBatch(List<String> logSamples, RecommendOptions options);
    
    /**
     * 그룹 통계 조회
     * @return 그룹별 포맷 개수
     */
    Map<String, Integer> getGroupStatistics();
    
    /**
     * 벤더 통계 조회
     * @return 벤더별 포맷 개수
     */
    Map<String, Integer> getVendorStatistics();
    
    /**
     * 서비스 종료
     */
    void shutdown();
    
    /**
     * 추천 옵션 클래스
     */
    public static class RecommendOptions {
        private int maxResults = 10;              // 최대 결과 개수
        private double minConfidence = 0.0;       // 최소 신뢰도
        private boolean includePartialMatches = true;  // 부분 매칭 포함
        private boolean enableCaching = true;     // 캐싱 활성화
        private int cacheSize = 1000;            // 캐시 크기
        private long cacheExpireTime = 3600000;  // 캐시 만료 시간 (밀리초)
        private boolean parallelProcessing = true; // 병렬 처리 활성화
        private int parallelThreads = 0;          // 병렬 스레드 수 (0=자동)
        private String groupFilter = null;        // 그룹 필터
        private String vendorFilter = null;       // 벤더 필터
        
        // Getters and Setters
        public int getMaxResults() {
            return maxResults;
        }
        
        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }
        
        public double getMinConfidence() {
            return minConfidence;
        }
        
        public void setMinConfidence(double minConfidence) {
            this.minConfidence = minConfidence;
        }
        
        public boolean isIncludePartialMatches() {
            return includePartialMatches;
        }
        
        public void setIncludePartialMatches(boolean includePartialMatches) {
            this.includePartialMatches = includePartialMatches;
        }
        
        public boolean isEnableCaching() {
            return enableCaching;
        }
        
        public void setEnableCaching(boolean enableCaching) {
            this.enableCaching = enableCaching;
        }
        
        public int getCacheSize() {
            return cacheSize;
        }
        
        public void setCacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
        }
        
        public long getCacheExpireTime() {
            return cacheExpireTime;
        }
        
        public void setCacheExpireTime(long cacheExpireTime) {
            this.cacheExpireTime = cacheExpireTime;
        }
        
        public boolean isParallelProcessing() {
            return parallelProcessing;
        }
        
        public void setParallelProcessing(boolean parallelProcessing) {
            this.parallelProcessing = parallelProcessing;
        }
        
        public int getParallelThreads() {
            return parallelThreads;
        }
        
        public void setParallelThreads(int parallelThreads) {
            this.parallelThreads = parallelThreads;
        }
        
        public String getGroupFilter() {
            return groupFilter;
        }
        
        public void setGroupFilter(String groupFilter) {
            this.groupFilter = groupFilter;
        }
        
        public String getVendorFilter() {
            return vendorFilter;
        }
        
        public void setVendorFilter(String vendorFilter) {
            this.vendorFilter = vendorFilter;
        }
        
        /**
         * 빌더 패턴 지원
         */
        public static class Builder {
            private final RecommendOptions options = new RecommendOptions();
            
            public Builder maxResults(int maxResults) {
                options.setMaxResults(maxResults);
                return this;
            }
            
            public Builder minConfidence(double minConfidence) {
                options.setMinConfidence(minConfidence);
                return this;
            }
            
            public Builder includePartialMatches(boolean include) {
                options.setIncludePartialMatches(include);
                return this;
            }
            
            public Builder enableCaching(boolean enable) {
                options.setEnableCaching(enable);
                return this;
            }
            
            public Builder cacheSize(int size) {
                options.setCacheSize(size);
                return this;
            }
            
            public Builder cacheExpireTime(long expireTime) {
                options.setCacheExpireTime(expireTime);
                return this;
            }
            
            public Builder parallelProcessing(boolean enable) {
                options.setParallelProcessing(enable);
                return this;
            }
            
            public Builder parallelThreads(int threads) {
                options.setParallelThreads(threads);
                return this;
            }
            
            public RecommendOptions build() {
                return options;
            }
        }
    }
}