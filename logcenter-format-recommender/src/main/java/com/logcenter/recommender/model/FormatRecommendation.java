package com.logcenter.recommender.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * 로그 포맷 추천 결과를 나타내는 모델 클래스
 * 추천된 포맷과 관련 정보를 포함
 */
public class FormatRecommendation implements Comparable<FormatRecommendation> {
    
    private LogFormat logFormat;              // 추천된 로그 포맷
    private double confidence;                // 추천 신뢰도 (0.0 ~ 100.0)
    private List<MatchResult> matchResults;   // 매칭 결과 목록
    private String recommendationReason;      // 추천 이유
    private int rank;                         // 추천 순위
    private long analysisTime;                // 분석 소요 시간 (밀리초)
    private boolean isExactMatch;             // 정확한 매칭 여부
    private int totalPatternsChecked;         // 검사한 총 패턴 수
    private int successfulMatches;            // 성공한 매칭 수
    
    // 추가 필드들
    private boolean completeMatch;            // 완전 매칭 여부
    private boolean partialMatch;             // 부분 매칭 여부
    private java.util.Map<String, Object> matchedFields; // 매칭된 필드들
    private List<String> missingFields;       // 누락된 필드들
    private long matchTime;                   // 매칭 시간
    private int matchCount;                   // 매칭 횟수
    private String matchDetails;              // 매칭 상세 정보
    private String groupName;                 // 그룹명
    private String vendor;                    // 벤더명
    
    public FormatRecommendation() {
        this.matchResults = new ArrayList<>();
        this.confidence = 0.0;
        this.rank = 0;
        this.isExactMatch = false;
    }
    
    public FormatRecommendation(LogFormat logFormat) {
        this();
        this.logFormat = logFormat;
    }
    
    /**
     * 매칭 결과 추가
     */
    public void addMatchResult(MatchResult matchResult) {
        if (matchResult != null) {
            this.matchResults.add(matchResult);
            updateStatistics();
        }
    }
    
    /**
     * 통계 정보 업데이트
     */
    private void updateStatistics() {
        if (matchResults.isEmpty()) {
            return;
        }
        
        // 완전 매칭 개수 계산
        long completeMatches = matchResults.stream()
            .filter(MatchResult::isCompleteMatch)
            .count();
        
        // 부분 매칭 개수 계산
        long partialMatches = matchResults.stream()
            .filter(MatchResult::isPartialMatch)
            .count();
        
        this.successfulMatches = (int)(completeMatches + partialMatches);
        
        // 신뢰도 계산 (PRD 알고리즘 기반)
        if (completeMatches == 1) {
            // 단일 완전 매칭: 98%
            this.confidence = 98.0;
            this.isExactMatch = true;
            this.recommendationReason = "단일 Grok 패턴이 완벽하게 매칭됨";
        } else if (completeMatches > 1) {
            // 다중 완전 매칭: 95-97%
            this.confidence = Math.min(95.0 + (completeMatches * 0.5), 97.0);
            this.isExactMatch = true;
            this.recommendationReason = String.format("%d개의 Grok 패턴이 완벽하게 매칭됨", completeMatches);
        } else if (partialMatches > 0) {
            // 부분 매칭: 최대 70%
            double avgScore = matchResults.stream()
                .filter(MatchResult::isPartialMatch)
                .mapToDouble(MatchResult::getMatchScore)
                .average()
                .orElse(0.0);
            this.confidence = Math.min(avgScore * 70.0, 70.0);
            this.isExactMatch = false;
            this.recommendationReason = String.format("%d개의 패턴이 부분적으로 매칭됨", partialMatches);
        } else {
            // 매칭 없음
            this.confidence = 0.0;
            this.isExactMatch = false;
            this.recommendationReason = "매칭되는 패턴이 없음";
        }
    }
    
    /**
     * 최고 매칭 결과 반환
     */
    public MatchResult getBestMatchResult() {
        return matchResults.stream()
            .filter(MatchResult::isCompleteMatch)
            .findFirst()
            .orElse(matchResults.stream()
                .filter(MatchResult::isPartialMatch)
                .max((a, b) -> Double.compare(a.getMatchScore(), b.getMatchScore()))
                .orElse(null));
    }
    
    /**
     * 추천 포맷의 그룹 정보 반환
     */
    public String getFormatGroup() {
        return logFormat != null ? logFormat.getGroupName() : null;
    }
    
    /**
     * 추천 포맷의 벤더 정보 반환
     */
    public String getFormatVendor() {
        return logFormat != null ? logFormat.getVendor() : null;
    }
    
    /**
     * 추천 결과 요약 생성
     */
    public String getSummary() {
        if (logFormat == null) {
            return "추천된 포맷 없음";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("포맷: %s (v%s)", 
            logFormat.getFormatName(), 
            logFormat.getFormatVersion()));
        summary.append(String.format(" | 신뢰도: %.1f%%", confidence));
        summary.append(String.format(" | 그룹: %s", logFormat.getGroupName()));
        summary.append(String.format(" | 벤더: %s", logFormat.getVendor()));
        
        if (isExactMatch) {
            summary.append(" | [정확한 매칭]");
        }
        
        return summary.toString();
    }
    
    @Override
    public int compareTo(FormatRecommendation other) {
        // 신뢰도 기준 내림차순 정렬
        int confidenceCompare = Double.compare(other.confidence, this.confidence);
        if (confidenceCompare != 0) {
            return confidenceCompare;
        }
        
        // 신뢰도가 같으면 완전 매칭 우선
        if (this.isExactMatch != other.isExactMatch) {
            return this.isExactMatch ? -1 : 1;
        }
        
        // 그 다음은 성공한 매칭 수로 정렬
        return Integer.compare(other.successfulMatches, this.successfulMatches);
    }
    
    // Getters and Setters
    public LogFormat getLogFormat() {
        return logFormat;
    }
    
    public void setLogFormat(LogFormat logFormat) {
        this.logFormat = logFormat;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    public List<MatchResult> getMatchResults() {
        return matchResults;
    }
    
    public void setMatchResults(List<MatchResult> matchResults) {
        this.matchResults = matchResults;
        updateStatistics();
    }
    
    public String getRecommendationReason() {
        return recommendationReason;
    }
    
    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }
    
    public int getRank() {
        return rank;
    }
    
    public void setRank(int rank) {
        this.rank = rank;
    }
    
    public long getAnalysisTime() {
        return analysisTime;
    }
    
    public void setAnalysisTime(long analysisTime) {
        this.analysisTime = analysisTime;
    }
    
    public boolean isExactMatch() {
        return isExactMatch;
    }
    
    public void setExactMatch(boolean exactMatch) {
        isExactMatch = exactMatch;
    }
    
    public int getTotalPatternsChecked() {
        return totalPatternsChecked;
    }
    
    public void setTotalPatternsChecked(int totalPatternsChecked) {
        this.totalPatternsChecked = totalPatternsChecked;
    }
    
    public int getSuccessfulMatches() {
        return successfulMatches;
    }
    
    public void setSuccessfulMatches(int successfulMatches) {
        this.successfulMatches = successfulMatches;
    }
    
    // 추가 필드들의 Getters and Setters
    public boolean isCompleteMatch() {
        return completeMatch;
    }
    
    public void setCompleteMatch(boolean completeMatch) {
        this.completeMatch = completeMatch;
    }
    
    public boolean isPartialMatch() {
        return partialMatch;
    }
    
    public void setPartialMatch(boolean partialMatch) {
        this.partialMatch = partialMatch;
    }
    
    public java.util.Map<String, Object> getMatchedFields() {
        return matchedFields;
    }
    
    public void setMatchedFields(java.util.Map<String, Object> matchedFields) {
        this.matchedFields = matchedFields;
    }
    
    public long getMatchTime() {
        return matchTime;
    }
    
    public void setMatchTime(long matchTime) {
        this.matchTime = matchTime;
    }
    
    public int getMatchCount() {
        return matchCount;
    }
    
    public void setMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }
    
    public String getMatchDetails() {
        return matchDetails;
    }
    
    public void setMatchDetails(String matchDetails) {
        this.matchDetails = matchDetails;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getVendor() {
        return vendor;
    }
    
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
    
    public List<String> getMissingFields() {
        return missingFields;
    }
    
    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }
    
    // 편의 메서드들
    public String getFormatId() {
        return logFormat != null ? logFormat.getFormatId() : null;
    }
    
    public String getFormatName() {
        return logFormat != null ? logFormat.getFormatName() : null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormatRecommendation that = (FormatRecommendation) o;
        return Objects.equals(logFormat, that.logFormat);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(logFormat);
    }
    
    @Override
    public String toString() {
        return "FormatRecommendation{" +
                "formatId='" + (logFormat != null ? logFormat.getFormatId() : "null") + '\'' +
                ", confidence=" + confidence +
                ", rank=" + rank +
                ", isExactMatch=" + isExactMatch +
                ", successfulMatches=" + successfulMatches +
                '}';
    }
}