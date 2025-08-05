package com.logcenter.recommender.filter;

import com.logcenter.recommender.model.LogFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 너무 일반적인 Grok 패턴을 필터링하는 유틸리티 클래스
 * 
 * 일부 패턴은 너무 일반적이어서 거의 모든 로그와 매칭되므로,
 * 정확한 로그 포맷 추천을 위해 이런 패턴들을 제외합니다.
 */
public class PatternFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(PatternFilter.class);
    
    /**
     * 제외할 너무 일반적인 패턴들
     */
    private static final Set<String> OVERLY_GENERIC_PATTERNS = new HashSet<>(Arrays.asList(
        // 시간과 메시지만 있는 패턴
        "^%{LOG_TIME:log_time} %{MESSAGE:message}$",
        "^%{LOG_TIME:log_time}\\s+%{MESSAGE:message}$",
        
        // 메시지만 있는 패턴
        "^%{MESSAGE:message}$",
        "^.*%{MESSAGE:message}.*$",
        
        // 너무 간단한 패턴
        "^%{GREEDYDATA:data}$",
        "^%{DATA:data}$",
        "^.*$",
        
        // 로그 레벨과 메시지만 있는 패턴
        "^%{LOGLEVEL:level} %{MESSAGE:message}$",
        "^\\[%{LOGLEVEL:level}\\] %{MESSAGE:message}$"
    ));
    
    /**
     * 너무 일반적인 패턴을 포함하는 정규식들
     */
    private static final Pattern[] GENERIC_PATTERN_MATCHERS = {
        // MESSAGE 필드만 있거나 주요 필드인 경우
        Pattern.compile("^\\^?%\\{MESSAGE:[^}]+\\}\\$?$"),
        Pattern.compile("^\\^?%\\{LOG_TIME:[^}]+\\}\\s*%\\{MESSAGE:[^}]+\\}\\$?$"),
        Pattern.compile("^\\^?.*%\\{GREEDYDATA:[^}]+\\}.*\\$?$"),
        Pattern.compile("^\\^?%\\{DATA:[^}]+\\}\\$?$")
    };
    
    /**
     * 패턴이 너무 일반적인지 확인
     * 
     * @param pattern Grok 패턴 문자열
     * @return 너무 일반적이면 true, 그렇지 않으면 false
     */
    public static boolean isOverlyGeneric(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return true;
        }
        
        // 정확히 일치하는 패턴 확인
        if (OVERLY_GENERIC_PATTERNS.contains(pattern)) {
            logger.debug("패턴이 일반적인 패턴 목록에 포함됨: {}", pattern);
            return true;
        }
        
        // 패턴 매처로 확인
        for (Pattern matcher : GENERIC_PATTERN_MATCHERS) {
            if (matcher.matcher(pattern).matches()) {
                logger.debug("패턴이 일반적인 패턴과 일치함: {}", pattern);
                return true;
            }
        }
        
        // 너무 짧은 패턴 (의미있는 필드가 2개 미만)
        int fieldCount = countFields(pattern);
        if (fieldCount < 2) {
            logger.debug("패턴의 필드 수가 너무 적음: {} (필드 수: {})", pattern, fieldCount);
            return true;
        }
        
        // MESSAGE나 GREEDYDATA가 주요 필드인 경우
        if (isMessageDominant(pattern)) {
            logger.debug("MESSAGE/GREEDYDATA가 주요 필드임: {}", pattern);
            return true;
        }
        
        return false;
    }
    
    /**
     * 패턴에서 필드 개수 세기
     */
    private static int countFields(String pattern) {
        if (pattern == null) return 0;
        
        int count = 0;
        int index = 0;
        while ((index = pattern.indexOf("%{", index)) != -1) {
            count++;
            index += 2;
        }
        return count;
    }
    
    /**
     * MESSAGE나 GREEDYDATA가 주요 필드인지 확인
     */
    private static boolean isMessageDominant(String pattern) {
        if (pattern == null) return false;
        
        // 전체 필드 수
        int totalFields = countFields(pattern);
        if (totalFields == 0) return false;
        
        // MESSAGE와 GREEDYDATA 필드 수
        int messageFields = 0;
        Pattern messagePattern = Pattern.compile("%\\{(MESSAGE|GREEDYDATA|DATA):[^}]+\\}");
        java.util.regex.Matcher matcher = messagePattern.matcher(pattern);
        while (matcher.find()) {
            messageFields++;
        }
        
        // MESSAGE/GREEDYDATA 필드가 전체의 50% 이상이면 너무 일반적
        return messageFields > 0 && (double) messageFields / totalFields >= 0.5;
    }
    
    /**
     * LogFormat에서 너무 일반적인 패턴 필터링
     * 
     * @param format 로그 포맷
     * @return 사용 가능한 패턴이 있으면 true, 모두 일반적이면 false
     */
    public static boolean hasUsablePatterns(LogFormat format) {
        if (format == null || format.getLogTypes() == null) {
            return false;
        }
        
        for (LogFormat.LogType logType : format.getLogTypes()) {
            if (logType.getPatterns() != null) {
                for (LogFormat.Pattern pattern : logType.getPatterns()) {
                    if (pattern.getGrokExp() != null && !isOverlyGeneric(pattern.getGrokExp())) {
                        return true; // 사용 가능한 패턴이 하나라도 있으면 true
                    }
                }
            }
        }
        
        return false; // 모든 패턴이 너무 일반적임
    }
    
    /**
     * 패턴의 구체성 점수 계산 (0.0 ~ 1.0)
     * 점수가 높을수록 더 구체적인 패턴
     */
    public static double getSpecificityScore(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return 0.0;
        }
        
        if (isOverlyGeneric(pattern)) {
            return 0.0;
        }
        
        double score = 1.0;
        
        // MESSAGE/GREEDYDATA 필드가 있으면 점수 감소
        if (pattern.contains("%{MESSAGE:") || pattern.contains("%{GREEDYDATA:") || pattern.contains("%{DATA:")) {
            score *= 0.5;
        }
        
        // 필드 수가 많을수록 점수 증가
        int fieldCount = countFields(pattern);
        score *= Math.min(1.0, fieldCount / 10.0);
        
        // 특정 필드들이 있으면 점수 증가
        if (pattern.contains("%{IP:") || pattern.contains("%{SRC_IP:") || pattern.contains("%{DST_IP:")) {
            score *= 1.2;
        }
        if (pattern.contains("%{PORT:") || pattern.contains("%{SRC_PORT:") || pattern.contains("%{DST_PORT:")) {
            score *= 1.1;
        }
        
        return Math.min(1.0, score);
    }
}