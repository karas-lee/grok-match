package com.logcenter.recommender.grok;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Grok 패턴 정규화 유틸리티
 * 중복된 named capturing group 등의 문제를 해결
 */
public class PatternNormalizer {
    
    private static final Logger logger = LoggerFactory.getLogger(PatternNormalizer.class);
    
    // Grok 패턴 매칭을 위한 정규식
    private static final Pattern GROK_PATTERN = Pattern.compile("%\\{([^:}]+)(?::([^}]+))?\\}");
    
    /**
     * 중복된 named capturing group을 제거하고 정규화
     * 
     * @param grokExpression 원본 Grok 표현식
     * @return 정규화된 Grok 표현식
     */
    public static String normalizeDuplicateGroups(String grokExpression) {
        if (grokExpression == null || grokExpression.isEmpty()) {
            return grokExpression;
        }
        
        // 각 named group의 출현 횟수를 추적
        Map<String, Integer> groupCounts = new HashMap<>();
        
        // 모든 named group 찾기
        Matcher matcher = GROK_PATTERN.matcher(grokExpression);
        while (matcher.find()) {
            String groupName = matcher.group(2);
            if (groupName != null && !groupName.isEmpty()) {
                groupCounts.put(groupName, groupCounts.getOrDefault(groupName, 0) + 1);
            }
        }
        
        // 중복된 그룹이 있는 경우 처리
        Map<String, Integer> groupIndexes = new HashMap<>();
        StringBuffer result = new StringBuffer();
        matcher = GROK_PATTERN.matcher(grokExpression);
        
        while (matcher.find()) {
            String patternName = matcher.group(1);
            String groupName = matcher.group(2);
            
            if (groupName != null && !groupName.isEmpty() && groupCounts.get(groupName) > 1) {
                // 중복된 그룹인 경우 인덱스 추가
                int index = groupIndexes.getOrDefault(groupName, 0);
                groupIndexes.put(groupName, index + 1);
                
                if (index > 0) {
                    // 두 번째 이후 출현에는 인덱스 추가
                    String newGroupName = groupName + "_" + index;
                    matcher.appendReplacement(result, "%{" + patternName + ":" + newGroupName + "}");
                    logger.info("중복 그룹 이름 변경: {} -> {}", groupName, newGroupName);
                } else {
                    // 첫 번째 출현은 그대로
                    matcher.appendReplacement(result, matcher.group());
                }
            } else {
                // 중복되지 않은 경우 그대로
                matcher.appendReplacement(result, matcher.group());
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 빈 데이터 타입 제거
     * 
     * @param grokExpression 원본 Grok 표현식
     * @return 정규화된 Grok 표현식
     */
    public static String removeEmptyDataTypes(String grokExpression) {
        if (grokExpression == null || grokExpression.isEmpty()) {
            return grokExpression;
        }
        
        // %{PATTERN:} -> %{PATTERN} 변환
        String result = grokExpression.replaceAll("%\\{([^:}]+):\\}", "%{$1}");
        
        // %{PATTERN:FIELD:} -> %{PATTERN:FIELD} 변환 (이중 콜론)
        result = result.replaceAll("%\\{([^:}]+:[^:}]*?):\\}", "%{$1}");
        
        return result;
    }
    
    /**
     * 잘못된 named capturing group 패턴 수정
     * Grok 패턴 확장 시 생성되는 이중 괄호 문제를 해결
     * 예: (?<name>([^\>]*)) -> (?<name>[^\>]*)
     * 
     * @param regexPattern 정규식 패턴
     * @return 수정된 정규식 패턴
     */
    public static String fixMalformedNamedGroups(String regexPattern) {
        if (regexPattern == null || regexPattern.isEmpty()) {
            return regexPattern;
        }
        
        // 잘못된 named group 패턴 매칭: (?<name>([^...]*))
        // 내부 캡처링 그룹의 괄호를 제거
        Pattern malformedPattern = Pattern.compile("\\(\\?<([^>]+)>\\(([^)]*)\\)\\)");
        Matcher matcher = malformedPattern.matcher(regexPattern);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String groupName = matcher.group(1);
            String innerPattern = matcher.group(2);
            
            // 수정된 패턴으로 교체: (?<name>inner_pattern)
            String replacement = "(?<" + groupName + ">" + innerPattern + ")";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            
            logger.debug("잘못된 named group 수정: {} -> {}", matcher.group(), replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 패턴 전체 정규화
     * 
     * @param grokExpression 원본 Grok 표현식
     * @return 정규화된 Grok 표현식
     */
    public static String normalize(String grokExpression) {
        if (grokExpression == null || grokExpression.isEmpty()) {
            return grokExpression;
        }
        
        // 1. 빈 데이터 타입 제거
        String normalized = removeEmptyDataTypes(grokExpression);
        
        // 2. 중복된 named group 처리
        normalized = normalizeDuplicateGroups(normalized);
        
        // 3. 정규식 내 named group 구문 오류 수정
        normalized = fixNamedGroupSyntax(normalized);
        
        // 4. 잘못된 대괄호 내 named group 수정
        normalized = fixBracketNamedGroup(normalized);
        
        return normalized;
    }
    
    /**
     * 정규식 내 named group 구문 오류 수정
     * 
     * @param expression 원본 표현식
     * @return 수정된 표현식
     */
    private static String fixNamedGroupSyntax(String expression) {
        if (expression == null || expression.isEmpty()) {
            return expression;
        }
        
        // (?<name>...) 형태의 named group에서 > 누락 수정
        // 패턴: (?<name 다음에 >가 없고 다른 문자가 나오는 경우
        String fixed = expression.replaceAll("\\(\\?<([^>]+)([^>])\\)", "(?<$1>$2)");
        
        return fixed;
    }
    
    /**
     * 잘못된 대괄호 내 named group 수정
     * [(?<log_name>fw4_deny)]* → \[fw4_deny\] 형태로 수정
     * 
     * @param expression 원본 표현식
     * @return 수정된 표현식
     */
    private static String fixBracketNamedGroup(String expression) {
        if (expression == null || expression.isEmpty()) {
            return expression;
        }
        
        // [(?<log_name>패턴)]* 형태를 \[패턴\] 형태로 수정
        String fixed = expression.replaceAll("\\[\\(\\?<[^>]+>([^)]+)\\)\\]\\*", "\\\\[$1\\\\]");
        
        // [(?<log_name>패턴)] 형태를 \[패턴\] 형태로 수정 (별표 없는 경우)
        fixed = fixed.replaceAll("\\[\\(\\?<[^>]+>([^)]+)\\)\\]", "\\\\[$1\\\\]");
        
        return fixed;
    }
    
    /**
     * 컴파일된 정규식 패턴 정규화 (Grok 확장 후)
     * 
     * @param compiledPattern 컴파일된 정규식 패턴
     * @return 정규화된 정규식 패턴
     */
    public static String normalizeCompiledPattern(String compiledPattern) {
        if (compiledPattern == null || compiledPattern.isEmpty()) {
            return compiledPattern;
        }
        
        // 잘못된 named capturing group 수정
        String normalized = fixMalformedNamedGroups(compiledPattern);
        
        return normalized;
    }
}