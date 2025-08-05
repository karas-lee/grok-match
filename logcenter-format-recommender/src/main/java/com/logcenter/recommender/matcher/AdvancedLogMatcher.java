package com.logcenter.recommender.matcher;

import com.logcenter.recommender.grok.FieldValidator;
import com.logcenter.recommender.grok.GrokCompilerWrapper;
import com.logcenter.recommender.grok.validator.*;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.model.MatchResult;
import com.logcenter.recommender.filter.PatternFilter;
import com.logcenter.recommender.util.GrokPatternParser;
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
        
        // 모든 LogType의 모든 패턴을 확인
        if (logFormat.getLogTypes() != null) {
            for (LogFormat.LogType logType : logFormat.getLogTypes()) {
                if (logType.getPatterns() != null) {
                    for (LogFormat.Pattern pattern : logType.getPatterns()) {
                        if (pattern.getGrokExp() != null) {
                            MatchResult result = matchPattern(normalizedLog, pattern.getGrokExp(), 
                                logFormat, pattern.getExpName());
                            if (result.isCompleteMatch()) {
                                return result;
                            }
                        }
                    }
                }
            }
        }
        
        // 기본 패턴으로 시도 (하위 호환성)
        String grokPattern = logFormat.getGrokPattern();
        
        if (grokPattern == null || grokPattern.trim().isEmpty()) {
            return MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
        }
        
        return matchPattern(normalizedLog, grokPattern, logFormat, null);
    }
    
    /**
     * 단일 패턴 매칭
     */
    private MatchResult matchPattern(String logLine, String grokPattern, LogFormat logFormat, String patternName) {
        // 너무 일반적인 패턴 필터링
        if (PatternFilter.isOverlyGeneric(grokPattern)) {
            logger.debug("너무 일반적인 패턴 건너뛰기 - 포맷: {}, 패턴: {}", 
                logFormat.getFormatName(), grokPattern);
            return MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
        }
        
        // Grok 패턴 컴파일
        Grok grok = grokCompiler.compileSafe(grokPattern);
        if (grok == null) {
            return MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
        }
        
        // 패턴 매칭
        Match grokMatch = grok.match(logLine);
        Map<String, Object> originalCaptures = grokMatch.capture();
        
        logger.debug("로그 포맷 {}: 원본 캡처 결과 - {}", logFormat.getFormatId(), originalCaptures);
        
        // 원본 캡처가 비어있으면 매칭 실패
        if (originalCaptures == null || originalCaptures.isEmpty()) {
            return MatchResult.noMatch(logFormat.getFormatId(), logFormat.getFormatName());
        }
        
        // 그룹명이 지정된 필드만 필터링
        Map<String, Object> filteredCaptures = filterNamedGroups(originalCaptures, grokPattern);
        logger.debug("로그 포맷 {}: 필터링 후 캡처 결과 - {}", logFormat.getFormatId(), filteredCaptures);
        
        // 필드 검증 (필터링된 결과에 대해)
        if (options.isValidateFields()) {
            filteredCaptures = validateFields(filteredCaptures);
        }
        
        // 매칭 결과 평가 (원본 캡처 기준)
        boolean isComplete = evaluateCompleteMatch(logLine, originalCaptures, logFormat);
        
        if (isComplete) {
            MatchResult result = MatchResult.completeMatch(
                logFormat.getFormatId(),
                logFormat.getFormatName(),
                filteredCaptures  // 필터링된 결과 사용
            );
            
            // 그룹 가중치 적용
            applyGroupWeight(result, logFormat);
            result.setGrokExpression(grokPattern);
            
            return result;
            
        } else if (options.isPartialMatchEnabled()) {
            double matchScore = calculateAdvancedMatchScore(logLine, originalCaptures, logFormat);
            
            if (matchScore > 0.3) { // 최소 임계값
                MatchResult result = MatchResult.partialMatch(
                    logFormat.getFormatId(),
                    logFormat.getFormatName(),
                    filteredCaptures,  // 필터링된 결과 사용
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
        
        // 최소 필드 수 기준 (3개 초과)
        if (captures.size() <= 3) {
            // 특별히 구체적인 필드가 있는 경우 예외 처리
            Set<String> specificFields = new HashSet<>(Arrays.asList(
                "src_ip", "dst_ip", "src_port", "dst_port",
                "protocol", "action", "rule_id", "attack_id",
                "src", "dst", "source", "destination"
            ));
            
            long specificCount = captures.keySet().stream()
                .filter(key -> specificFields.contains(key.toLowerCase()))
                .count();
            
            // 구체적인 필드가 2개 이상이면 예외적으로 허용
            if (specificCount < 2) {
                return false;
            }
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
        
        // 1. 필드 수 점수 (40%) - 가중치 증가
        double fieldScore = Math.min(captures.size() / 8.0, 1.0) * 0.4;
        
        // 2. 구체적인 필드 점수 (20%) - 새로 추가
        double specificFieldScore = calculateSpecificFieldScore(captures) * 0.2;
        
        // 3. 커버리지 점수 (20%) - 가중치 감소
        int captureLength = captures.values().stream()
            .filter(Objects::nonNull)
            .mapToInt(v -> v.toString().length())
            .sum();
        double coverageScore = Math.min((double) captureLength / logLine.length(), 1.0) * 0.2;
        
        // 4. 검증된 필드 점수 (10%) - 가중치 감소
        long validatedCount = captures.keySet().stream()
            .filter(key -> fieldValidators.containsKey(key.toUpperCase()))
            .count();
        double validationScore = (captures.size() > 0) ? 
            (double) validatedCount / captures.size() * 0.1 : 0.0;
        
        // 5. 필수 필드 점수 (10%) - 가중치 감소
        double requiredScore = 0.0;
        List<String> required = logFormat.getRequiredFields();
        if (required != null && !required.isEmpty()) {
            long foundRequired = required.stream()
                .filter(captures::containsKey)
                .count();
            requiredScore = (double) foundRequired / required.size() * 0.1;
        } else {
            requiredScore = 0.1;
        }
        
        score = fieldScore + specificFieldScore + coverageScore + validationScore + requiredScore;
        
        return Math.min(score, 1.0);
    }
    
    /**
     * 구체적인 필드 점수 계산
     */
    private double calculateSpecificFieldScore(Map<String, Object> captures) {
        double score = 0.0;
        
        // 구체적인 필드 목록
        Set<String> specificFields = new HashSet<>(Arrays.asList(
            "src_ip", "dst_ip", "src_port", "dst_port",
            "protocol", "action", "rule_id", "attack_id",
            "user_id", "session_id", "event_id",
            "src", "dst", "source", "destination"
        ));
        
        // 일반적인 필드
        Set<String> genericFields = new HashSet<>(Arrays.asList(
            "message", "data", "text", "info", "description"
        ));
        
        int specificCount = 0;
        int genericCount = 0;
        
        for (String field : captures.keySet()) {
            String lowerField = field.toLowerCase();
            if (specificFields.contains(lowerField)) {
                specificCount++;
            } else if (genericFields.contains(lowerField)) {
                genericCount++;
            }
        }
        
        // 구체적인 필드가 많을수록 높은 점수
        if (specificCount > 0) {
            score = Math.min(specificCount / 4.0, 1.0);
        }
        
        // 일반적인 필드만 있으면 점수 감소
        if (specificCount == 0 && genericCount > 0) {
            score = 0.2;
        }
        
        return score;
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
            // 필드 수와 구체성에 따라 신뢰도 차등 적용
            for (MatchResult result : results) {
                if (result.isCompleteMatch()) {
                    Map<String, Object> fields = result.getExtractedFields();
                    int fieldCount = fields.size();
                    
                    // 구체적인 필드 수 계산
                    Set<String> specificFields = new HashSet<>(Arrays.asList(
                        "src_ip", "dst_ip", "src_port", "dst_port",
                        "protocol", "action", "rule_id", "attack_id",
                        "user_id", "session_id", "event_id",
                        "src", "dst", "source", "destination"
                    ));
                    
                    long specificCount = fields.keySet().stream()
                        .filter(key -> specificFields.contains(key.toLowerCase()))
                        .count();
                    
                    // 기본 신뢰도 설정
                    double baseConfidence = 90.0;
                    
                    // 필드 수에 따른 가산점 (최대 5점)
                    double fieldBonus = Math.min(fieldCount * 0.5, 5.0);
                    
                    // 구체적인 필드에 따른 가산점 (최대 3점)
                    double specificBonus = Math.min(specificCount * 1.0, 3.0);
                    
                    double adjustedConfidence = baseConfidence + fieldBonus + specificBonus;
                    
                    // 최대 98% 제한
                    result.setConfidence(Math.min(adjustedConfidence, 98.0));
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
    
    /**
     * 그룹명이 지정된 필드만 필터링
     * Grok 패턴에서 그룹명이 없는 패턴은 제거
     */
    private Map<String, Object> filterNamedGroups(Map<String, Object> captures, String grokPattern) {
        if (captures == null || captures.isEmpty()) {
            return captures;
        }
        
        // Grok 패턴에서 명시적으로 지정된 필드명 추출
        Set<String> namedFields = GrokPatternParser.extractNamedFields(grokPattern);
        logger.debug("명시적으로 지정된 필드명: {}", namedFields);
        logger.debug("필터링 전 캡처된 필드: {}", captures);
        
        Map<String, Object> filtered = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : captures.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 빈 값인 경우 제외
            if (value == null || value.toString().trim().isEmpty()) {
                logger.debug("빈 값 필터링: {} = {}", key, value);
                continue;
            }
            
            // 명시적으로 지정된 필드명만 포함
            if (namedFields.contains(key)) {
                filtered.put(key, value);
            } else {
                logger.debug("명시되지 않은 필드 필터링: {} = {}", key, value);
            }
        }
        
        logger.debug("필터링 후 필드: {}", filtered);
        
        return filtered;
    }
}