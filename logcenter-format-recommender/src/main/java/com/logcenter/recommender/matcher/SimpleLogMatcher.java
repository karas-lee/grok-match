package com.logcenter.recommender.matcher;

import com.logcenter.recommender.grok.GrokCompilerWrapper;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.model.MatchResult;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 간단한 로그 매처 구현
 * 기본적인 Grok 패턴 매칭 기능을 제공
 */
public class SimpleLogMatcher implements LogMatcher {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleLogMatcher.class);
    
    private final GrokCompilerWrapper grokCompiler;
    private MatchOptions options;
    private final MatchStatistics statistics;
    private final Map<String, Grok> compiledPatternCache;
    
    public SimpleLogMatcher(GrokCompilerWrapper grokCompiler) {
        this.grokCompiler = grokCompiler;
        this.options = new MatchOptions();
        this.statistics = new MatchStatistics();
        this.compiledPatternCache = new ConcurrentHashMap<>();
    }
    
    @Override
    public MatchResult match(String logLine, LogFormat logFormat) {
        if (logLine == null || logFormat == null) {
            return MatchResult.noMatch(null, null);
        }
        
        long startTime = System.currentTimeMillis();
        MatchResult result = null;
        
        try {
            // 로그 라인 정규화
            String normalizedLog = normalizeLogLine(logLine);
            
            // Grok 패턴 가져오기 및 컴파일
            String grokPattern = logFormat.getGrokPattern();
            if (grokPattern == null || grokPattern.trim().isEmpty()) {
                logger.debug("로그 포맷 {}에 Grok 패턴이 없습니다", logFormat.getFormatId());
                return MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
            }
            
            // 캐시에서 컴파일된 패턴 확인
            Grok grok = compiledPatternCache.computeIfAbsent(grokPattern, pattern -> {
                Grok compiled = grokCompiler.compileSafe(pattern);
                if (compiled == null) {
                    logger.warn("Grok 패턴 컴파일 실패: {}", pattern);
                }
                return compiled;
            });
            
            if (grok == null) {
                return MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
            }
            
            // 패턴 매칭 수행
            Match grokMatch = grok.match(normalizedLog);
            Map<String, Object> captures = grokMatch.capture();
            
            if (captures != null && !captures.isEmpty()) {
                // 완전 매칭 여부 확인
                boolean isComplete = isCompleteMatch(normalizedLog, captures);
                
                if (isComplete) {
                    result = MatchResult.completeMatch(
                        logFormat.getFormatId(),
                        logFormat.getFormatName(),
                        captures
                    );
                } else if (options.isPartialMatchEnabled()) {
                    // 부분 매칭 점수 계산
                    double matchScore = calculateMatchScore(normalizedLog, captures);
                    result = MatchResult.partialMatch(
                        logFormat.getFormatId(),
                        logFormat.getFormatName(),
                        captures,
                        matchScore
                    );
                } else {
                    result = MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
                }
                
                result.setGrokExpression(grokPattern);
            } else {
                result = MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
            }
            
        } catch (Exception e) {
            logger.error("로그 매칭 중 오류 발생: {}", e.getMessage(), e);
            result = MatchResult.noMatch(
                logFormat != null ? logFormat.getFormatId() : null,
                logFormat != null ? logFormat.getFormatName() : null
            );
        } finally {
            if (result != null) {
                long matchTime = System.currentTimeMillis() - startTime;
                result.setMatchTime(matchTime);
                
                if (options.isCollectStats()) {
                    statistics.recordMatch(result);
                }
            }
        }
        
        return result;
    }
    
    @Override
    public List<MatchResult> matchAll(String logLine, List<LogFormat> logFormats) {
        if (logLine == null || logFormats == null || logFormats.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<MatchResult> results = new ArrayList<>();
        
        for (LogFormat format : logFormats) {
            MatchResult result = match(logLine, format);
            if (result != null && (result.isCompleteMatch() || 
                (result.isPartialMatch() && options.isPartialMatchEnabled()))) {
                results.add(result);
            }
        }
        
        // 신뢰도 순으로 정렬
        results.sort((r1, r2) -> Double.compare(r2.getConfidence(), r1.getConfidence()));
        
        return results;
    }
    
    @Override
    public MatchResult matchMultiLine(List<String> logLines, LogFormat logFormat) {
        if (logLines == null || logLines.isEmpty() || logFormat == null) {
            return MatchResult.noMatch(null, null);
        }
        
        // 멀티라인 로그를 단일 문자열로 결합
        String combinedLog = String.join("\n", logLines);
        
        // 멀티라인 옵션 임시 활성화
        boolean originalMultiline = options.isMultiline();
        options.setMultiline(true);
        
        try {
            return match(combinedLog, logFormat);
        } finally {
            options.setMultiline(originalMultiline);
        }
    }
    
    @Override
    public Map<String, Object> extractFields(String logLine) {
        if (logLine == null || logLine.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, Object> extractedFields = new HashMap<>();
        
        // 기본 필드 추출 (공백 기반)
        String[] parts = logLine.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            extractedFields.put("field_" + i, parts[i]);
        }
        
        // 일반적인 패턴 검색
        // IP 주소
        java.util.regex.Pattern ipPattern = java.util.regex.Pattern.compile(
            "\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"
        );
        java.util.regex.Matcher ipMatcher = ipPattern.matcher(logLine);
        List<String> ips = new ArrayList<>();
        while (ipMatcher.find()) {
            ips.add(ipMatcher.group());
        }
        if (!ips.isEmpty()) {
            extractedFields.put("ip_addresses", ips);
        }
        
        // 타임스탬프 (간단한 예시)
        java.util.regex.Pattern timestampPattern = java.util.regex.Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}"
        );
        java.util.regex.Matcher timestampMatcher = timestampPattern.matcher(logLine);
        if (timestampMatcher.find()) {
            extractedFields.put("timestamp", timestampMatcher.group());
        }
        
        return extractedFields;
    }
    
    @Override
    public void setOptions(MatchOptions options) {
        this.options = options != null ? options : new MatchOptions();
    }
    
    @Override
    public MatchStatistics getStatistics() {
        return statistics;
    }
    
    @Override
    public void reset() {
        statistics.reset();
        compiledPatternCache.clear();
    }
    
    /**
     * 로그 라인 정규화
     */
    private String normalizeLogLine(String logLine) {
        if (logLine == null) {
            return "";
        }
        
        // 앞뒤 공백 제거
        String normalized = logLine.trim();
        
        // 대소문자 무시 옵션 처리
        if (options.isCaseInsensitive()) {
            normalized = normalized.toLowerCase();
        }
        
        // 멀티라인 옵션 처리
        if (!options.isMultiline()) {
            // 첫 번째 줄만 사용
            int newlineIndex = normalized.indexOf('\n');
            if (newlineIndex > 0) {
                normalized = normalized.substring(0, newlineIndex);
            }
        }
        
        return normalized;
    }
    
    /**
     * 완전 매칭 여부 확인
     */
    private boolean isCompleteMatch(String logLine, Map<String, Object> captures) {
        if (captures == null || captures.isEmpty()) {
            return false;
        }
        
        // 캡처된 모든 값을 연결하여 원본과 비교
        StringBuilder capturedContent = new StringBuilder();
        for (Object value : captures.values()) {
            if (value != null) {
                capturedContent.append(value.toString());
            }
        }
        
        // 공백을 제거하고 비교
        String normalizedLog = logLine.replaceAll("\\s+", "");
        String normalizedCapture = capturedContent.toString().replaceAll("\\s+", "");
        
        // 캡처된 내용이 원본의 80% 이상인 경우 완전 매칭으로 간주
        double coverage = (double) normalizedCapture.length() / normalizedLog.length();
        return coverage >= 0.8;
    }
    
    /**
     * 부분 매칭 점수 계산
     */
    private double calculateMatchScore(String logLine, Map<String, Object> captures) {
        if (captures == null || captures.isEmpty()) {
            return 0.0;
        }
        
        // 캡처된 필드 수 기반 점수
        double fieldScore = Math.min(captures.size() / 10.0, 1.0);
        
        // 캡처된 내용의 길이 기반 점수
        int totalCaptureLength = 0;
        for (Object value : captures.values()) {
            if (value != null) {
                totalCaptureLength += value.toString().length();
            }
        }
        double coverageScore = Math.min((double) totalCaptureLength / logLine.length(), 1.0);
        
        // 두 점수의 평균
        return (fieldScore + coverageScore) / 2.0;
    }
}