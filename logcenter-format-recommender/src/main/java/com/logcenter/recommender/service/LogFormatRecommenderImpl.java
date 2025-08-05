package com.logcenter.recommender.service;

import com.logcenter.recommender.grok.GrokCompilerWrapper;
import com.logcenter.recommender.grok.PatternRepository;
import com.logcenter.recommender.matcher.AdvancedLogMatcher;
import com.logcenter.recommender.matcher.LogMatcher;
import com.logcenter.recommender.model.FormatRecommendation;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.model.MatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 로그 포맷 추천 서비스 구현체
 */
public class LogFormatRecommenderImpl implements LogFormatRecommender {
    
    private static final Logger logger = LoggerFactory.getLogger(LogFormatRecommenderImpl.class);
    
    private final PatternRepository patternRepository;
    private final GrokCompilerWrapper grokCompiler;
    private final LogMatcher logMatcher;
    private final ExecutorService executorService;
    private RecommendOptions options;
    
    // 캐시 (옵션에 따라 사용)
    private final Map<String, List<FormatRecommendation>> cache;
    private final Map<String, Long> cacheTimestamps;
    
    /**
     * 생성자
     */
    public LogFormatRecommenderImpl(PatternRepository patternRepository, 
                                   GrokCompilerWrapper grokCompiler) {
        this.patternRepository = patternRepository;
        this.grokCompiler = grokCompiler;
        this.logMatcher = new AdvancedLogMatcher(grokCompiler);
        this.options = new RecommendOptions();
        
        // 병렬 처리용 스레드 풀
        int threads = options.getParallelThreads() > 0 ? 
            options.getParallelThreads() : Runtime.getRuntime().availableProcessors();
        this.executorService = Executors.newFixedThreadPool(threads);
        
        // 캐시 초기화
        this.cache = new ConcurrentHashMap<>();
        this.cacheTimestamps = new ConcurrentHashMap<>();
    }
    
    @Override
    public boolean initialize() {
        try {
            // Grok 패턴 로드
            boolean standardLoaded = grokCompiler.loadStandardPatterns();
            int customLoaded = grokCompiler.loadCustomPatterns();
            
            if (!standardLoaded || customLoaded == 0) {
                logger.error("Grok 패턴 로드 실패");
                return false;
            }
            
            // 패턴 저장소 초기화
            boolean repoInitialized = patternRepository.initialize();
            if (!repoInitialized) {
                logger.error("패턴 저장소 초기화 실패");
                return false;
            }
            
            logger.info("로그 포맷 추천 서비스 초기화 완료");
            return true;
            
        } catch (Exception e) {
            logger.error("초기화 중 오류 발생", e);
            return false;
        }
    }
    
    @Override
    public List<FormatRecommendation> recommend(String logSample) {
        if (logSample == null || logSample.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // 캐시 확인
        if (options.isEnableCaching()) {
            List<FormatRecommendation> cached = getCachedResult(logSample);
            if (cached != null) {
                return cached;
            }
        }
        
        // 모든 포맷에 대해 매칭 수행
        List<LogFormat> formats = patternRepository.getAllFormats();
        List<FormatRecommendation> recommendations = performMatching(logSample, formats);
        
        // 캐싱
        if (options.isEnableCaching() && !recommendations.isEmpty()) {
            cacheResult(logSample, recommendations);
        }
        
        return recommendations;
    }
    
    @Override
    public List<FormatRecommendation> recommendBatch(List<String> logSamples) {
        if (logSamples == null || logSamples.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 각 샘플에 대한 추천 결과를 병합
        Map<String, FormatRecommendation> mergedResults = new ConcurrentHashMap<>();
        
        // 병렬 처리
        List<CompletableFuture<Void>> futures = logSamples.stream()
            .map(sample -> CompletableFuture.runAsync(() -> {
                List<FormatRecommendation> results = recommend(sample);
                
                // 결과 병합
                for (FormatRecommendation result : results) {
                    mergedResults.merge(result.getLogFormat().getFormatId(), result,
                        (existing, newResult) -> {
                            // 평균 신뢰도 계산
                            double avgConfidence = (existing.getConfidence() + newResult.getConfidence()) / 2.0;
                            existing.setConfidence(avgConfidence);
                            existing.setMatchCount(existing.getMatchCount() + 1);
                            return existing;
                        });
                }
            }, executorService))
            .collect(Collectors.toList());
        
        // 모든 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // 결과 정렬 및 필터링
        return mergedResults.values().stream()
            .sorted(Comparator.comparingDouble(FormatRecommendation::getConfidence).reversed())
            .limit(options.getMaxResults())
            .collect(Collectors.toList());
    }
    
    @Override
    public List<FormatRecommendation> recommendInGroup(String logSample, String groupName) {
        if (logSample == null || groupName == null) {
            return Collections.emptyList();
        }
        
        // 특정 그룹의 포맷만 가져오기
        List<LogFormat> formats = patternRepository.getFormatsByGroup(groupName);
        
        if (formats.isEmpty()) {
            logger.warn("그룹 '{}'에 포맷이 없습니다", groupName);
            return Collections.emptyList();
        }
        
        return performMatching(logSample, formats);
    }
    
    @Override
    public void setOptions(RecommendOptions options) {
        this.options = options != null ? options : new RecommendOptions();
        
        // 스레드 풀 재설정
        if (options != null && options.getParallelThreads() > 0) {
            executorService.shutdown();
            ((ThreadPoolExecutor) executorService).setCorePoolSize(options.getParallelThreads());
        }
        
        // 캐시 크기 조정
        if (cache.size() > options.getCacheSize()) {
            clearOldCache();
        }
    }
    
    @Override
    public List<LogFormat> getAvailableFormats() {
        return patternRepository.getAllFormats();
    }
    
    @Override
    public List<LogFormat> getFormatsByGroup(String groupName) {
        return patternRepository.getFormatsByGroup(groupName);
    }
    
    @Override
    public int reloadFormats() {
        // 캐시 초기화
        cache.clear();
        cacheTimestamps.clear();
        
        // 포맷 재로드
        return patternRepository.reloadFormats();
    }
    
    /**
     * 실제 매칭 수행
     */
    private List<FormatRecommendation> performMatching(String logSample, List<LogFormat> formats) {
        List<FormatRecommendation> recommendations = new ArrayList<>();
        
        if (options.isParallelProcessing()) {
            // 병렬 매칭
            List<CompletableFuture<FormatRecommendation>> futures = formats.stream()
                .map(format -> CompletableFuture.supplyAsync(() -> 
                    matchAndCreateRecommendation(logSample, format), executorService))
                .collect(Collectors.toList());
            
            // 결과 수집
            for (CompletableFuture<FormatRecommendation> future : futures) {
                try {
                    FormatRecommendation rec = future.get();
                    if (rec != null) {
                        recommendations.add(rec);
                    }
                } catch (Exception e) {
                    logger.debug("매칭 실패", e);
                }
            }
        } else {
            // 순차 매칭
            for (LogFormat format : formats) {
                FormatRecommendation rec = matchAndCreateRecommendation(logSample, format);
                if (rec != null) {
                    recommendations.add(rec);
                }
            }
        }
        
        // 결과 필터링 및 정렬
        return recommendations.stream()
            .filter(rec -> rec.getConfidence() >= options.getMinConfidence())
            .filter(rec -> options.isIncludePartialMatches() || rec.isCompleteMatch())
            .sorted(Comparator.comparingDouble(FormatRecommendation::getConfidence).reversed())
            .limit(options.getMaxResults())
            .collect(Collectors.toList());
    }
    
    /**
     * 단일 포맷에 대한 매칭 및 추천 생성
     */
    private FormatRecommendation matchAndCreateRecommendation(String logSample, LogFormat format) {
        try {
            // 매칭 수행
            MatchResult matchResult = logMatcher.match(logSample, format);
            
            if (matchResult == null || 
                (!matchResult.isCompleteMatch() && !matchResult.isPartialMatch())) {
                return null;
            }
            
            // 추천 결과 생성
            FormatRecommendation recommendation = new FormatRecommendation();
            recommendation.setLogFormat(format);
            recommendation.setConfidence(matchResult.getConfidence());
            recommendation.setCompleteMatch(matchResult.isCompleteMatch());
            recommendation.setPartialMatch(matchResult.isPartialMatch());
            recommendation.setMatchedFields(matchResult.getExtractedFields());
            recommendation.setMatchTime(matchResult.getMatchTime());
            recommendation.setMatchCount(1);
            
            // 추가 정보 설정
            if (matchResult.getMatchDetails() != null) {
                recommendation.setMatchDetails(matchResult.getMatchDetails());
            }
            
            // 그룹 정보
            recommendation.setGroupName(format.getGroupName());
            recommendation.setVendor(format.getVendor());
            
            return recommendation;
            
        } catch (Exception e) {
            logger.debug("포맷 {} 매칭 중 오류", format.getFormatId(), e);
            return null;
        }
    }
    
    /**
     * 캐시에서 결과 가져오기
     */
    private List<FormatRecommendation> getCachedResult(String logSample) {
        String cacheKey = generateCacheKey(logSample);
        Long timestamp = cacheTimestamps.get(cacheKey);
        
        if (timestamp != null) {
            long age = System.currentTimeMillis() - timestamp;
            if (age < options.getCacheExpireTime()) {
                return cache.get(cacheKey);
            } else {
                // 만료된 캐시 제거
                cache.remove(cacheKey);
                cacheTimestamps.remove(cacheKey);
            }
        }
        
        return null;
    }
    
    /**
     * 결과 캐싱
     */
    private void cacheResult(String logSample, List<FormatRecommendation> result) {
        // 캐시 크기 확인
        if (cache.size() >= options.getCacheSize()) {
            clearOldCache();
        }
        
        String cacheKey = generateCacheKey(logSample);
        cache.put(cacheKey, new ArrayList<>(result));
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
    }
    
    /**
     * 오래된 캐시 제거
     */
    private void clearOldCache() {
        long currentTime = System.currentTimeMillis();
        
        // 만료된 항목 제거
        cacheTimestamps.entrySet().removeIf(entry -> {
            long age = currentTime - entry.getValue();
            if (age > options.getCacheExpireTime()) {
                cache.remove(entry.getKey());
                return true;
            }
            return false;
        });
        
        // 여전히 크기가 초과하면 가장 오래된 항목 제거
        if (cache.size() >= options.getCacheSize()) {
            int toRemove = cache.size() - options.getCacheSize() / 2;
            
            cacheTimestamps.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(toRemove)
                .map(Map.Entry::getKey)
                .forEach(key -> {
                    cache.remove(key);
                    cacheTimestamps.remove(key);
                });
        }
    }
    
    /**
     * 캐시 키 생성
     */
    private String generateCacheKey(String logSample) {
        // 간단한 해시 사용
        return String.valueOf(logSample.hashCode());
    }
    
    /**
     * 리소스 정리
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        if (logMatcher instanceof AdvancedLogMatcher) {
            ((AdvancedLogMatcher) logMatcher).shutdown();
        }
    }
    
    @Override
    public List<FormatRecommendation> recommend(String logSample, RecommendOptions options) {
        if (options != null) {
            this.options = options;
        }
        return recommend(logSample);
    }
    
    @Override
    public List<List<FormatRecommendation>> recommendBatch(List<String> logSamples, RecommendOptions options) {
        if (options != null) {
            this.options = options;
        }
        
        List<List<FormatRecommendation>> results = new ArrayList<>();
        
        if (logSamples == null || logSamples.isEmpty()) {
            return results;
        }
        
        // 병렬 처리 활성화 여부 확인
        if (options != null && options.isParallelProcessing() && logSamples.size() > 10) {
            // 병렬 처리
            results = logSamples.parallelStream()
                .map(sample -> recommend(sample, options))
                .collect(Collectors.toList());
        } else {
            // 순차 처리
            for (String sample : logSamples) {
                results.add(recommend(sample, options));
            }
        }
        
        return results;
    }
    
    @Override
    public Map<String, Integer> getGroupStatistics() {
        return patternRepository.getGroupStatistics();
    }
    
    @Override
    public Map<String, Integer> getVendorStatistics() {
        return patternRepository.getVendorStatistics();
    }
}