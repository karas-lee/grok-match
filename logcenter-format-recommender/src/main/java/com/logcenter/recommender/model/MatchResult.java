package com.logcenter.recommender.model;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * Grok 패턴 매칭 결과를 나타내는 모델 클래스
 * 매칭 성공 여부, 추출된 필드, 신뢰도 등의 정보를 포함
 */
public class MatchResult {
    
    private String logFormatId;           // 매칭된 로그 포맷 ID
    private String patternName;           // 매칭된 패턴 이름
    private String grokExpression;        // 사용된 Grok 표현식
    private boolean isCompleteMatch;      // 완전 매칭 여부
    private boolean isPartialMatch;       // 부분 매칭 여부
    private Map<String, Object> extractedFields;  // 추출된 필드들
    private double matchScore;            // 매칭 점수 (0.0 ~ 1.0)
    private double confidence;            // 신뢰도 (0.0 ~ 100.0)
    private long matchTime;               // 매칭 소요 시간 (밀리초)
    private String matchDetails;          // 매칭 상세 정보
    
    public MatchResult() {
        this.extractedFields = new HashMap<>();
        this.isCompleteMatch = false;
        this.isPartialMatch = false;
        this.matchScore = 0.0;
        this.confidence = 0.0;
    }
    
    public MatchResult(String logFormatId, String patternName) {
        this();
        this.logFormatId = logFormatId;
        this.patternName = patternName;
    }
    
    /**
     * 완전 매칭 결과 생성
     */
    public static MatchResult completeMatch(String logFormatId, String patternName, 
                                           Map<String, Object> fields) {
        MatchResult result = new MatchResult(logFormatId, patternName);
        result.setCompleteMatch(true);
        result.setPartialMatch(false);
        result.setExtractedFields(fields);
        result.setMatchScore(1.0);
        result.setConfidence(98.0); // 단일 완전 매칭 시 98%
        return result;
    }
    
    /**
     * 부분 매칭 결과 생성
     */
    public static MatchResult partialMatch(String logFormatId, String patternName, 
                                          Map<String, Object> fields, double matchScore) {
        MatchResult result = new MatchResult(logFormatId, patternName);
        result.setCompleteMatch(false);
        result.setPartialMatch(true);
        result.setExtractedFields(fields);
        result.setMatchScore(matchScore);
        result.setConfidence(Math.min(matchScore * 70.0, 70.0)); // 최대 70%
        return result;
    }
    
    /**
     * 매칭 실패 결과 생성
     */
    public static MatchResult noMatch(String logFormatId, String patternName) {
        MatchResult result = new MatchResult(logFormatId, patternName);
        result.setCompleteMatch(false);
        result.setPartialMatch(false);
        result.setMatchScore(0.0);
        result.setConfidence(0.0);
        return result;
    }
    
    /**
     * 추출된 필드 개수 반환
     */
    public int getExtractedFieldCount() {
        return extractedFields != null ? extractedFields.size() : 0;
    }
    
    /**
     * 특정 필드 값 반환
     */
    public Object getFieldValue(String fieldName) {
        return extractedFields != null ? extractedFields.get(fieldName) : null;
    }
    
    /**
     * 특정 필드 존재 여부 확인
     */
    public boolean hasField(String fieldName) {
        return extractedFields != null && extractedFields.containsKey(fieldName);
    }
    
    /**
     * 필수 필드 검증
     */
    public boolean hasRequiredFields(String... requiredFields) {
        if (extractedFields == null || requiredFields == null) {
            return false;
        }
        
        for (String field : requiredFields) {
            if (!extractedFields.containsKey(field)) {
                return false;
            }
        }
        return true;
    }
    
    // Getters and Setters
    public String getLogFormatId() {
        return logFormatId;
    }
    
    public void setLogFormatId(String logFormatId) {
        this.logFormatId = logFormatId;
    }
    
    public String getPatternName() {
        return patternName;
    }
    
    public void setPatternName(String patternName) {
        this.patternName = patternName;
    }
    
    public String getGrokExpression() {
        return grokExpression;
    }
    
    public void setGrokExpression(String grokExpression) {
        this.grokExpression = grokExpression;
    }
    
    public boolean isCompleteMatch() {
        return isCompleteMatch;
    }
    
    public void setCompleteMatch(boolean completeMatch) {
        isCompleteMatch = completeMatch;
    }
    
    public boolean isPartialMatch() {
        return isPartialMatch;
    }
    
    public void setPartialMatch(boolean partialMatch) {
        isPartialMatch = partialMatch;
    }
    
    public Map<String, Object> getExtractedFields() {
        return extractedFields;
    }
    
    public void setExtractedFields(Map<String, Object> extractedFields) {
        this.extractedFields = extractedFields;
    }
    
    public double getMatchScore() {
        return matchScore;
    }
    
    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    public long getMatchTime() {
        return matchTime;
    }
    
    public void setMatchTime(long matchTime) {
        this.matchTime = matchTime;
    }
    
    public String getMatchDetails() {
        return matchDetails;
    }
    
    public void setMatchDetails(String matchDetails) {
        this.matchDetails = matchDetails;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchResult that = (MatchResult) o;
        return Objects.equals(logFormatId, that.logFormatId) &&
               Objects.equals(patternName, that.patternName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(logFormatId, patternName);
    }
    
    @Override
    public String toString() {
        return "MatchResult{" +
                "logFormatId='" + logFormatId + '\'' +
                ", patternName='" + patternName + '\'' +
                ", isCompleteMatch=" + isCompleteMatch +
                ", confidence=" + confidence +
                ", extractedFields=" + getExtractedFieldCount() +
                '}';
    }
}