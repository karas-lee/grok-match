package com.logcenter.recommender.matcher;

import com.logcenter.recommender.config.AppConfig;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.model.MatchResult;
import java.util.List;
import java.util.Map;

/**
 * 로그 매칭 인터페이스
 * 로그 샘플과 로그 포맷을 매칭하는 기능을 정의
 */
public interface LogMatcher {
    
    /**
     * 단일 로그 라인과 로그 포맷 매칭
     * @param logLine 로그 라인
     * @param logFormat 로그 포맷
     * @return 매칭 결과
     */
    MatchResult match(String logLine, LogFormat logFormat);
    
    /**
     * 단일 로그 라인과 여러 로그 포맷 매칭
     * @param logLine 로그 라인
     * @param logFormats 로그 포맷 리스트
     * @return 매칭 결과 리스트
     */
    List<MatchResult> matchAll(String logLine, List<LogFormat> logFormats);
    
    /**
     * 여러 로그 라인과 단일 로그 포맷 매칭
     * @param logLines 로그 라인 리스트
     * @param logFormat 로그 포맷
     * @return 통합 매칭 결과
     */
    MatchResult matchMultiLine(List<String> logLines, LogFormat logFormat);
    
    /**
     * 로그 라인에서 필드 추출 (패턴 없이)
     * @param logLine 로그 라인
     * @return 추출된 필드 맵
     */
    Map<String, Object> extractFields(String logLine);
    
    /**
     * 매칭 옵션 설정
     * @param options 매칭 옵션
     */
    void setOptions(MatchOptions options);
    
    /**
     * 매칭 통계 반환
     * @return 통계 정보
     */
    MatchStatistics getStatistics();
    
    /**
     * 매처 초기화
     */
    void reset();
    
    /**
     * 매칭 옵션 클래스
     */
    public static class MatchOptions {
        private boolean caseInsensitive = false;
        private boolean multiline = false;
        private boolean partialMatchEnabled = true;
        private boolean validateFields = true;
        private int maxMatchTime;
        private boolean collectStats = true;
        
        public MatchOptions() {
            // AppConfig에서 타임아웃 설정 읽기
            this.maxMatchTime = AppConfig.getInstance().getInt(AppConfig.MATCH_TIMEOUT);
        }
        
        // Getters and Setters
        public boolean isCaseInsensitive() {
            return caseInsensitive;
        }
        
        public void setCaseInsensitive(boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
        }
        
        public boolean isMultiline() {
            return multiline;
        }
        
        public void setMultiline(boolean multiline) {
            this.multiline = multiline;
        }
        
        public boolean isPartialMatchEnabled() {
            return partialMatchEnabled;
        }
        
        public void setPartialMatchEnabled(boolean partialMatchEnabled) {
            this.partialMatchEnabled = partialMatchEnabled;
        }
        
        public boolean isValidateFields() {
            return validateFields;
        }
        
        public void setValidateFields(boolean validateFields) {
            this.validateFields = validateFields;
        }
        
        public int getMaxMatchTime() {
            return maxMatchTime;
        }
        
        public void setMaxMatchTime(int maxMatchTime) {
            this.maxMatchTime = maxMatchTime;
        }
        
        public boolean isCollectStats() {
            return collectStats;
        }
        
        public void setCollectStats(boolean collectStats) {
            this.collectStats = collectStats;
        }
    }
    
    /**
     * 매칭 통계 클래스
     */
    public static class MatchStatistics {
        private long totalMatches = 0;
        private long completeMatches = 0;
        private long partialMatches = 0;
        private long failedMatches = 0;
        private long totalMatchTime = 0;
        private long averageMatchTime = 0;
        private Map<String, Long> matchesByFormat;
        
        public MatchStatistics() {
            this.matchesByFormat = new java.util.concurrent.ConcurrentHashMap<>();
        }
        
        public void recordMatch(MatchResult result) {
            totalMatches++;
            totalMatchTime += result.getMatchTime();
            
            if (result.isCompleteMatch()) {
                completeMatches++;
            } else if (result.isPartialMatch()) {
                partialMatches++;
            } else {
                failedMatches++;
            }
            
            String formatId = result.getLogFormatId();
            if (formatId != null) {
                matchesByFormat.merge(formatId, 1L, Long::sum);
            }
            
            if (totalMatches > 0) {
                averageMatchTime = totalMatchTime / totalMatches;
            }
        }
        
        // Getters
        public long getTotalMatches() {
            return totalMatches;
        }
        
        public long getCompleteMatches() {
            return completeMatches;
        }
        
        public long getPartialMatches() {
            return partialMatches;
        }
        
        public long getFailedMatches() {
            return failedMatches;
        }
        
        public long getTotalMatchTime() {
            return totalMatchTime;
        }
        
        public long getAverageMatchTime() {
            return averageMatchTime;
        }
        
        public Map<String, Long> getMatchesByFormat() {
            return new java.util.HashMap<>(matchesByFormat);
        }
        
        public double getSuccessRate() {
            if (totalMatches == 0) return 0.0;
            return (double) (completeMatches + partialMatches) / totalMatches * 100.0;
        }
        
        public void reset() {
            totalMatches = 0;
            completeMatches = 0;
            partialMatches = 0;
            failedMatches = 0;
            totalMatchTime = 0;
            averageMatchTime = 0;
            matchesByFormat.clear();
        }
    }
}