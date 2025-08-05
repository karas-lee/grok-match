package com.logcenter.recommender.util;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Grok 패턴 문자열을 파싱하여 명시적으로 지정된 필드명을 추출하는 유틸리티
 */
public class GrokPatternParser {
    
    // Grok 패턴 형식: %{패턴명:필드명} 또는 %{패턴명}
    private static final Pattern GROK_PATTERN = Pattern.compile("%\\{([^:}]+)(?::([^}]+))?\\}");
    
    /**
     * Grok 패턴 문자열에서 명시적으로 지정된 필드명들을 추출
     * 
     * @param grokPattern Grok 패턴 문자열
     * @return 명시적으로 지정된 필드명 Set
     */
    public static Set<String> extractNamedFields(String grokPattern) {
        Set<String> namedFields = new HashSet<>();
        
        if (grokPattern == null || grokPattern.isEmpty()) {
            return namedFields;
        }
        
        Matcher matcher = GROK_PATTERN.matcher(grokPattern);
        
        while (matcher.find()) {
            // group(1): 패턴명, group(2): 필드명 (있는 경우)
            String fieldName = matcher.group(2);
            
            if (fieldName != null && !fieldName.trim().isEmpty()) {
                namedFields.add(fieldName.trim());
            }
        }
        
        return namedFields;
    }
    
    /**
     * 주어진 필드명이 명시적으로 지정된 필드인지 확인
     * 
     * @param grokPattern Grok 패턴 문자열
     * @param fieldName 확인할 필드명
     * @return 명시적으로 지정된 필드인 경우 true
     */
    public static boolean isNamedField(String grokPattern, String fieldName) {
        Set<String> namedFields = extractNamedFields(grokPattern);
        return namedFields.contains(fieldName);
    }
}