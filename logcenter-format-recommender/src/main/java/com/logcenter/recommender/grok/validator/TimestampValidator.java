package com.logcenter.recommender.grok.validator;

import com.logcenter.recommender.grok.FieldValidator;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 타임스탬프 검증기
 * 다양한 날짜/시간 형식의 유효성을 검증
 */
public class TimestampValidator implements FieldValidator {
    
    // 일반적인 날짜/시간 포맷터
    private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_ZONED_DATE_TIME,
        DateTimeFormatter.RFC_1123_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM dd HH:mm:ss", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM  d HH:mm:ss", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    );
    
    private final List<DateTimeFormatter> customFormatters;
    
    /**
     * 기본 생성자
     */
    public TimestampValidator() {
        this.customFormatters = null;
    }
    
    /**
     * 커스텀 포맷터를 추가로 지정하는 생성자
     * @param customFormatters 추가 포맷터 리스트
     */
    public TimestampValidator(List<DateTimeFormatter> customFormatters) {
        this.customFormatters = customFormatters;
    }
    
    @Override
    public boolean validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        value = value.trim();
        
        // 기본 포맷터로 시도
        for (DateTimeFormatter formatter : FORMATTERS) {
            if (tryParse(value, formatter)) {
                return true;
            }
        }
        
        // 커스텀 포맷터로 시도
        if (customFormatters != null) {
            for (DateTimeFormatter formatter : customFormatters) {
                if (tryParse(value, formatter)) {
                    return true;
                }
            }
        }
        
        // Unix timestamp 시도
        if (isUnixTimestamp(value)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 날짜/시간 파싱 시도
     */
    private boolean tryParse(String value, DateTimeFormatter formatter) {
        try {
            // LocalDateTime 시도
            LocalDateTime.parse(value, formatter);
            return true;
        } catch (Exception e1) {
            try {
                // ZonedDateTime 시도
                ZonedDateTime.parse(value, formatter);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }
    
    /**
     * Unix timestamp 검증 (초 또는 밀리초)
     */
    private boolean isUnixTimestamp(String value) {
        try {
            long timestamp = Long.parseLong(value);
            
            // 합리적인 범위 확인 (1970-2100년)
            if (timestamp > 0 && timestamp < 4102444800L) {
                // 초 단위
                return true;
            } else if (timestamp > 0 && timestamp < 4102444800000L) {
                // 밀리초 단위
                return true;
            }
            
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    @Override
    public String getFieldType() {
        return "TIMESTAMP";
    }
    
    @Override
    public String getDescription() {
        return "날짜/시간 (다양한 형식 지원)";
    }
}