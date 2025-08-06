package com.logcenter.recommender.validator;

import com.logcenter.recommender.grok.CustomPatternLoader;
import com.logcenter.recommender.grok.GrokCompilerWrapper;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.model.ValidationResult;
import com.logcenter.recommender.model.ValidationResult.Status;
import com.logcenter.recommender.util.JacksonJsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.Match;
import io.krakens.grok.api.exception.GrokException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 로그 포맷 검증기
 * setting_logformat.json의 모든 포맷을 검증하여 패턴 컴파일 오류와 파싱 문제를 미리 발견
 */
public class LogFormatValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(LogFormatValidator.class);
    
    private final GrokCompilerWrapper grokCompiler;
    private final int maxValidationTime = 10000; // 10초 타임아웃
    
    public LogFormatValidator() {
        this.grokCompiler = new GrokCompilerWrapper();
        logger.info("LogFormatValidator 초기화: 순차 처리 모드");
        
        // 커스텀 패턴 로드
        loadCustomPatterns();
    }
    
    /**
     * 커스텀 패턴 로드
     */
    private void loadCustomPatterns() {
        try {
            // 표준 패턴 로드
            grokCompiler.loadStandardPatterns();
            
            // 커스텀 패턴 로드
            int loadedCount = grokCompiler.loadCustomPatterns();
            logger.info("{}개의 커스텀 패턴을 로드했습니다", loadedCount);
        } catch (Exception e) {
            logger.error("커스텀 패턴 로드 실패", e);
        }
    }
    
    /**
     * 모든 로그 포맷 검증
     */
    public List<ValidationResult> validateAllFormats() {
        return validateAllFormats("/setting_logformat.json");
    }
    
    /**
     * 지정된 파일의 모든 로그 포맷 검증
     */
    public List<ValidationResult> validateAllFormats(String resourcePath) {
        List<LogFormat> formats = loadFormats(resourcePath);
        
        if (formats.isEmpty()) {
            logger.error("로드된 포맷이 없습니다");
            return Collections.emptyList();
        }
        
        logger.info("총 {}개의 포맷을 검증합니다", formats.size());
        
        // 순차 처리로 변경
        List<ValidationResult> allResults = new ArrayList<>();
        int totalFormats = formats.size();
        int processedFormats = 0;
        
        for (LogFormat format : formats) {
            processedFormats++;
            if (processedFormats % 10 == 0 || processedFormats == totalFormats) {
                logger.info("진행 상황: {}/{} 포맷 처리 완료 ({}%)", 
                    processedFormats, totalFormats, 
                    (processedFormats * 100) / totalFormats);
            }
            
            try {
                // 각 포맷을 순차적으로 검증
                List<ValidationResult> results = validateFormat(format);
                allResults.addAll(results);
            } catch (Exception e) {
                logger.error("포맷 {} 검증 중 오류: {}", format.getFormatName(), e.getMessage());
                // 오류가 발생해도 다음 포맷 계속 처리
                ValidationResult errorResult = new ValidationResult(format.getFormatId(), format.getFormatName());
                errorResult.addError("검증 중 예외 발생: " + e.getMessage());
                allResults.add(errorResult);
            }
        }
        
        logger.info("검증 완료: 총 {}개 포맷 처리", processedFormats);
        
        return allResults;
    }
    
    /**
     * 단일 로그 포맷 검증
     */
    public List<ValidationResult> validateFormat(LogFormat format) {
        List<ValidationResult> results = new ArrayList<>();
        
        if (format.getLogTypes() == null || format.getLogTypes().isEmpty()) {
            ValidationResult result = new ValidationResult(format.getFormatId(), format.getFormatName());
            result.setGroupName(format.getGroupName());
            result.setVendor(format.getVendor());
            result.addError("LogType이 정의되지 않음");
            results.add(result);
            return results;
        }
        
        // 각 LogType의 모든 패턴 검증
        for (LogFormat.LogType logType : format.getLogTypes()) {
            if (logType.getPatterns() == null || logType.getPatterns().isEmpty()) {
                ValidationResult result = new ValidationResult(format.getFormatId(), format.getFormatName());
                result.setGroupName(format.getGroupName());
                result.setVendor(format.getVendor());
                result.addWarning(String.format("LogType '%s'에 패턴이 없음", logType.getTypeName()));
                results.add(result);
                continue;
            }
            
            for (LogFormat.Pattern pattern : logType.getPatterns()) {
                ValidationResult result = validatePattern(format, logType, pattern);
                results.add(result);
            }
        }
        
        return results;
    }
    
    /**
     * 단일 패턴 검증
     */
    private ValidationResult validatePattern(LogFormat format, LogFormat.LogType logType, LogFormat.Pattern pattern) {
        long startTime = System.currentTimeMillis();
        
        ValidationResult result = new ValidationResult(format.getFormatId(), format.getFormatName());
        result.setExpName(pattern.getExpName());
        result.setGroupName(format.getGroupName());
        result.setVendor(format.getVendor());
        result.setGrokExpression(pattern.getGrokExp());
        result.setSampleLog(pattern.getSampleLog());
        
        // 1. Grok 표현식 검증
        if (pattern.getGrokExp() == null || pattern.getGrokExp().trim().isEmpty()) {
            result.addError("Grok 표현식이 비어있음");
            result.setValidationTime(System.currentTimeMillis() - startTime);
            return result;
        }
        
        // 2. 패턴 컴파일 시도
        final Grok grok;
        try {
            Grok tempGrok = grokCompiler.compileSafe(pattern.getGrokExp());
            if (tempGrok == null) {
                result.addError("패턴 컴파일 실패");
                result.setValidationTime(System.currentTimeMillis() - startTime);
                return result;
            }
            grok = tempGrok;
        } catch (Exception e) {
            result.addError(String.format("패턴 컴파일 오류: %s", e.getMessage()));
            result.setValidationTime(System.currentTimeMillis() - startTime);
            return result;
        }
        
        // 3. 샘플 로그 테스트
        if (pattern.getSampleLog() != null && !pattern.getSampleLog().trim().isEmpty()) {
            try {
                // 직접 매칭 실행 (순차 처리)
                final String sampleLog = pattern.getSampleLog();
                long matchStartTime = System.currentTimeMillis();
                
                Match match = grok.match(sampleLog);
                Map<String, Object> captures = match.capture();
                
                long matchTime = System.currentTimeMillis() - matchStartTime;
                if (matchTime > 5000) { // 5초 이상 걸리면 경고
                    logger.warn("매칭 시간이 오래 걸림: {} - {}ms", pattern.getExpName(), matchTime);
                }
                
                if (captures == null || captures.isEmpty()) {
                    result.addError("샘플 로그와 패턴이 매칭되지 않음");
                } else {
                    // 필드 추출 성공
                    result.setExtractedFields(captures);
                    
                    // 경고 체크
                    checkForWarnings(result, captures);
                    
                    if (!result.hasErrors()) {
                        result.setStatus(result.hasWarnings() ? Status.WARNING : Status.PASS);
                    }
                }
            } catch (Exception e) {
                result.addError(String.format("매칭 중 오류: %s", e.getMessage()));
            }
        } else {
            // 샘플 로그가 없으면 컴파일만 성공하면 PASS
            result.setStatus(Status.PASS);
            result.addWarning("샘플 로그가 없어 실제 매칭 테스트를 수행할 수 없음");
        }
        
        result.setValidationTime(System.currentTimeMillis() - startTime);
        return result;
    }
    
    /**
     * 경고 체크
     */
    private void checkForWarnings(ValidationResult result, Map<String, Object> captures) {
        // 필드 수가 너무 적은 경우
        Set<String> excludedFields = new HashSet<>(Arrays.asList(
            "log_time", "message", "msg", "raw_message"
        ));
        
        long effectiveFieldCount = captures.keySet().stream()
            .filter(key -> !excludedFields.contains(key.toLowerCase()))
            .count();
        
        if (effectiveFieldCount <= 2) {
            result.addWarning(String.format("유효 필드 수가 너무 적음 (%d개)", effectiveFieldCount));
        }
        
        // GREEDYDATA 사용 체크
        if (result.getGrokExpression() != null && result.getGrokExpression().contains("GREEDYDATA")) {
            result.addWarning("GREEDYDATA 패턴 사용 (성능 이슈 가능)");
        }
        
        // 너무 일반적인 패턴 체크
        if (result.getGrokExpression() != null) {
            String pattern = result.getGrokExpression();
            if (pattern.equals("^%{GREEDYDATA:message}$") || 
                pattern.equals("^.*$") ||
                pattern.equals("^.+$")) {
                result.addWarning("너무 일반적인 패턴");
            }
        }
    }
    
    /**
     * 포맷 파일 로드
     */
    private List<LogFormat> loadFormats(String resourcePath) {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                logger.error("로그 포맷 파일을 찾을 수 없습니다: {}", resourcePath);
                return Collections.emptyList();
            }
            
            // 전체 파일을 문자열로 읽기
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            
            // JSON 파싱
            TypeReference<List<LogFormat>> typeRef = new TypeReference<List<LogFormat>>() {};
            List<LogFormat> formats = JacksonJsonUtils.fromJson(content.toString(), typeRef);
            
            if (formats == null) {
                logger.warn("JSON 파싱 결과가 null입니다");
                return Collections.emptyList();
            }
            
            return formats;
            
        } catch (IOException e) {
            logger.error("로그 포맷 파일 읽기 오류", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 리소스 정리
     */
    public void shutdown() {
        // 순차 처리에서는 스레드 풀이 없으므로 특별한 종료 작업 불필요
        logger.info("LogFormatValidator 종료");
    }
}