package com.logcenter.recommender.grok;

/**
 * 필드 검증기 인터페이스
 * Grok으로 추출된 필드값의 유효성을 검증
 */
public interface FieldValidator {
    
    /**
     * 필드값 검증
     * @param value 검증할 값
     * @return 유효성 여부
     */
    boolean validate(String value);
    
    /**
     * 필드 타입 반환
     * @return 필드 타입 (예: IP, PORT, TIMESTAMP)
     */
    String getFieldType();
    
    /**
     * 검증 규칙 설명
     * @return 검증 규칙 설명
     */
    String getDescription();
}