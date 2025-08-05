package com.logcenter.recommender.matcher;

import com.logcenter.recommender.grok.FieldValidator;
import com.logcenter.recommender.grok.GrokCompilerWrapper;
import com.logcenter.recommender.grok.validator.*;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.model.MatchResult;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 고급 로그 매처 구현
 * 필드 검증, 병렬 처리, 그룹별 가중치 등 고급 기능 제공
 */
public class AdvancedLogMatcher implements LogMatcher {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedLogMatcher.class);
    
    private final GrokCompilerWrapper grokCompiler;
    private final ExecutorService executorService;
    private final Map<String, FieldValidator> fieldValidators;
    private final Map<String, Double> groupWeights;
    private MatchOptions options;
    private final MatchStatistics statistics;
    
    public AdvancedLogMatcher(GrokCompilerWrapper grokCompiler) {
        this.grokCompiler = grokCompiler;
        this.executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
        this.fieldValidators = initializeValidators();
        this.groupWeights = initializeGroupWeights();
        this.options = new MatchOptions();
        this.statistics = new MatchStatistics();
    }
    
    /**
     * 필드 검증기 초기화
     */
    private Map<String, FieldValidator> initializeValidators() {
        Map<String, FieldValidator> validators = new HashMap<>();
        
        // IP 검증기
        validators.put("IP", new IPValidator());
        validators.put("SRC_IP", new IPValidator());
        validators.put("DST_IP", new IPValidator());
        validators.put("CLIENT_IP", new IPValidator());
        validators.put("SERVER_IP", new IPValidator());
        
        // 포트 검증기
        validators.put("PORT", new PortValidator());
        validators.put("SRC_PORT", new PortValidator());
        validators.put("DST_PORT", new PortValidator());
        validators.put("CLIENT_PORT", new PortValidator());
        validators.put("SERVER_PORT", new PortValidator());
        
        // 타임스탬프 검증기
        validators.put("TIMESTAMP", new TimestampValidator());
        validators.put("DATE", new TimestampValidator());
        validators.put("TIME", new TimestampValidator());
        validators.put("DATETIME", new TimestampValidator());
        validators.put("LOG_TIME", new TimestampValidator());
        
        // HTTP 상태 코드 검증기
        validators.put("HTTP_STATUS", new HTTPStatusValidator());
        validators.put("STATUS_CODE", new HTTPStatusValidator());
        validators.put("RESPONSE_CODE", new HTTPStatusValidator());
        
        return validators;
    }
    
    /**
     * 그룹별 가중치 초기화
     */
    private Map<String, Double> initializeGroupWeights() {
        Map<String, Double> weights = new HashMap<>();
        
        // 그룹별 가중치 설정 (더 중요한 그룹일수록 높은 가중치)
        weights.put("FIREWALL", 1.2);
        weights.put("IPS", 1.2);
        weights.put("WAF", 1.1);
        weights.put("WEBSERVER", 1.0);
        weights.put("SYSTEM", 0.9);
        weights.put("APPLICATION", 0.8);
        weights.put("DEFAULT", 1.0);
        
        return weights;
    }
    
    @Override
    public MatchResult match(String logLine, LogFormat logFormat) {
        if (logLine == null || logFormat == null) {
            return MatchResult.noMatch(null, null);
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 타임아웃 설정으로 매칭 실행
            Future<MatchResult> future = executorService.submit(() -> 
                performMatch(logLine, logFormat)
            );
            
            MatchResult result = future.get(options.getMaxMatchTime(), TimeUnit.MILLISECONDS);
            
            long matchTime = System.currentTimeMillis() - startTime;
            result.setMatchTime(matchTime);
            
            if (options.isCollectStats()) {
                statistics.recordMatch(result);
            }
            
            return result;
            
        } catch (TimeoutException e) {
            logger.warn("매칭 타임아웃: {} ms 초과", options.getMaxMatchTime());
            return MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
        } catch (Exception e) {
            logger.error("매칭 중 오류 발생", e);
            return MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
        }
    }
    
    /**
     * 실제 매칭 수행
     */
    private MatchResult performMatch(String logLine, LogFormat logFormat) {
        String normalizedLog = normalizeLogLine(logLine);
        String grokPattern = logFormat.getGrokPattern();
        
        if (grokPattern == null || grokPattern.trim().isEmpty()) {
            return MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
        }
        
        // Grok 패턴 컴파일
        Grok grok = grokCompiler.compileSafe(grokPattern);
        if (grok == null) {
            return MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
        }
        
        // 패턴 매칭
        Match grokMatch = grok.match(normalizedLog);
        Map<String, Object> captures = grokMatch.capture();
        
        if (captures == null || captures.isEmpty()) {
            return MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
        }
        
        // 필드 검증
        if (options.isValidateFields()) {
            captures = validateFields(captures);
        }
        
        // 매칭 결과 평가
        boolean isComplete = evaluateCompleteMatch(normalizedLog, captures, logFormat);
        
        if (isComplete) {
            MatchResult result = MatchResult.completeMatch(
                logFormat.getFormatId(),
                logFormat.getFormatName(),
                captures
            );
            
            // 그룹 가중치 적용
            applyGroupWeight(result, logFormat);
            result.setGrokExpression(grokPattern);
            
            return result;
            
        } else if (options.isPartialMatchEnabled()) {
            double matchScore = calculateAdvancedMatchScore(normalizedLog, captures, logFormat);
            
            if (matchScore > 0.3) { // 최소 임계값
                MatchResult result = MatchResult.partialMatch(
                    logFormat.getFormatId(),
                    logFormat.getFormatName(),
                    captures,
                    matchScore
                );
                
                // 그룹 가중치 적용
                applyGroupWeight(result, logFormat);
                result.setGrokExpression(grokPattern);
                
                return result;
            }
        }
        
        return MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
    }
    
    @Override
    public List<MatchResult> matchAll(String logLine, List<LogFormat> logFormats) {
        if (logLine == null || logFormats == null || logFormats.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 병렬 처리로 매칭 수행
        List<CompletableFuture<MatchResult>> futures = logFormats.stream()
            .map(format -> CompletableFuture.supplyAsync(
                () -> match(logLine, format),
                executorService
            ))
            .collect(Collectors.toList());
        
        // 결과 수집
        List<MatchResult> results = futures.stream()
            .map(future -> {
                try {
                    return future.get(options.getMaxMatchTime(), TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    logger.debug("매칭 실패: {}", e.getMessage());
                    return null;
                }
            })
            .filter(result -> result != null && 
                (result.isCompleteMatch() || result.isPartialMatch()))
            .sorted((r1, r2) -> Double.compare(r2.getConfidence(), r1.getConfidence()))
            .collect(Collectors.toList());
        
        // 다중 완전 매칭 시 신뢰도 조정
        adjustMultipleMatchConfidence(results);
        
        return results;
    }
    
    @Override
    public MatchResult matchMultiLine(List<String> logLines, LogFormat logFormat) {
        if (logLines == null || logLines.isEmpty() || logFormat == null) {
            return MatchResult.noMatch(null, null);
        }
        
        // 각 라인을 개별 매칭
        List<MatchResult> lineResults = new ArrayList<>();
        for (String line : logLines) {
            MatchResult result = match(line, logFormat);
            if (result.isCompleteMatch() || result.isPartialMatch()) {
                lineResults.add(result);
            }
        }
        
        // 결과 통합
        if (lineResults.isEmpty()) {
            return MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
        }
        
        // 평균 신뢰도 계산
        double avgConfidence = lineResults.stream()
            .mapToDouble(MatchResult::getConfidence)
            .average()
            .orElse(0.0);
        
        // 통합 필드
        Map<String, Object> mergedFields = new HashMap<>();
        for (MatchResult result : lineResults) {
            mergedFields.putAll(result.getExtractedFields());
        }
        
        // 통합 결과 생성
        MatchResult result = new MatchResult(logFormat.getFormatId(), logFormat.getFormatName());
        result.setExtractedFields(mergedFields);
        result.setConfidence(avgConfidence);
        result.setCompleteMatch(avgConfidence >= 90.0);
        result.setPartialMatch(avgConfidence > 0 && avgConfidence < 90.0);
        
        return result;
    }
    
    @Override
    public Map<String, Object> extractFields(String logLine) {
        if (logLine == null || logLine.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, Object> fields = new HashMap<>();
        
        // 커스텀 패턴을 사용한 필드 추출
        Map<String, com.logcenter.recommender.model.GrokPattern> customPatterns = 
            grokCompiler.getAllCustomPatterns();
        
        for (Map.Entry<String, com.logcenter.recommender.model.GrokPattern> entry : 
             customPatterns.entrySet()) {
            
            String patternName = entry.getKey();
            String pattern = entry.getValue().getPattern();
            
            try {
                java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher matcher = regex.matcher(logLine);
                
                if (matcher.find()) {
                    fields.put(patternName, matcher.group());
                }
            } catch (Exception e) {
                logger.debug("패턴 매칭 실패: {}", patternName);
            }
        }
        
        return fields;
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
    }
    
    /**
     * 필드 검증
     */
    private Map<String, Object> validateFields(Map<String, Object> fields) {
        Map<String, Object> validatedFields = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            
            if (fieldValue == null) {
                continue;
            }
            
            // 검증기 찾기
            FieldValidator validator = fieldValidators.get(fieldName.toUpperCase());
            if (validator != null) {
                String stringValue = fieldValue.toString();
                if (validator.validate(stringValue)) {
                    validatedFields.put(fieldName, fieldValue);
                } else {
                    logger.debug("필드 검증 실패: {} = {}", fieldName, fieldValue);
                }
            } else {
                // 검증기가 없는 필드는 그대로 포함
                validatedFields.put(fieldName, fieldValue);
            }
        }
        
        return validatedFields;
    }
    
    /**
     * 완전 매칭 평가
     */
    private boolean evaluateCompleteMatch(String logLine, Map<String, Object> captures, 
                                         LogFormat logFormat) {
        if (captures.isEmpty()) {
            return false;
        }
        
        // 필수 필드 확인
        List<String> requiredFields = logFormat.getRequiredFields();
        if (requiredFields != null && !requiredFields.isEmpty()) {
            for (String required : requiredFields) {
                if (!captures.containsKey(required)) {
                    return false;
                }
            }
        }
        
        // 캡처 커버리지 계산
        int totalLength = 0;
        for (Object value : captures.values()) {
            if (value != null) {
                totalLength += value.toString().length();
            }
        }
        
        double coverage = (double) totalLength / logLine.length();
        return coverage >= 0.75; // 75% 이상 커버리지
    }
    
    /**
     * 고급 매칭 점수 계산
     */
    private double calculateAdvancedMatchScore(String logLine, Map<String, Object> captures,
                                              LogFormat logFormat) {
        double score = 0.0;
        
        // 1. 필드 수 점수 (30%)
        double fieldScore = Math.min(captures.size() / 10.0, 1.0) * 0.3;
        
        // 2. 커버리지 점수 (30%)
        int captureLength = captures.values().stream()
            .filter(Objects::nonNull)
            .mapToInt(v -> v.toString().length())
            .sum();
        double coverageScore = Math.min((double) captureLength / logLine.length(), 1.0) * 0.3;
        
        // 3. 검증된 필드 점수 (20%)
        long validatedCount = captures.keySet().stream()
            .filter(key -> fieldValidators.containsKey(key.toUpperCase()))
            .count();
        double validationScore = (captures.size() > 0) ? 
            (double) validatedCount / captures.size() * 0.2 : 0.0;
        
        // 4. 필수 필드 점수 (20%)
        double requiredScore = 0.0;
        List<String> required = logFormat.getRequiredFields();
        if (required != null && !required.isEmpty()) {
            long foundRequired = required.stream()
                .filter(captures::containsKey)
                .count();
            requiredScore = (double) foundRequired / required.size() * 0.2;
        } else {
            requiredScore = 0.2; // 필수 필드가 없으면 만점
        }
        
        score = fieldScore + coverageScore + validationScore + requiredScore;
        
        return Math.min(score, 1.0);
    }
    
    /**
     * 그룹 가중치 적용
     */
    private void applyGroupWeight(MatchResult result, LogFormat logFormat) {
        String group = logFormat.getFormatGroup();
        if (group != null) {
            Double weight = groupWeights.getOrDefault(group.toUpperCase(), 1.0);
            double adjustedConfidence = result.getConfidence() * weight;
            
            // 최대값 제한
            if (result.isCompleteMatch()) {
                adjustedConfidence = Math.min(adjustedConfidence, 98.0);
            } else {
                adjustedConfidence = Math.min(adjustedConfidence, 70.0);
            }
            
            result.setConfidence(adjustedConfidence);
        }
    }
    
    /**
     * 다중 완전 매칭 시 신뢰도 조정
     */
    private void adjustMultipleMatchConfidence(List<MatchResult> results) {
        long completeMatchCount = results.stream()
            .filter(MatchResult::isCompleteMatch)
            .count();
        
        if (completeMatchCount > 1) {
            // 다중 완전 매칭 시 95-97% 범위로 조정
            for (MatchResult result : results) {
                if (result.isCompleteMatch()) {
                    double adjusted = 95.0 + (result.getConfidence() - 95.0) * 0.4;
                    result.setConfidence(Math.min(adjusted, 97.0));
                }
            }
        }
    }
    
    /**
     * 로그 라인 정규화
     */
    private String normalizeLogLine(String logLine) {
        if (logLine == null) {
            return "";
        }
        
        String normalized = logLine.trim();
        
        if (options.isCaseInsensitive()) {
            normalized = normalized.toLowerCase();
        }
        
        return normalized;
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
    }
}