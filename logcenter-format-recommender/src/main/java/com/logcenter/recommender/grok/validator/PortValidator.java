package com.logcenter.recommender.grok.validator;

import com.logcenter.recommender.grok.FieldValidator;

/**
 * 포트 번호 검증기
 * TCP/UDP 포트 번호의 유효성을 검증 (0-65535)
 */
public class PortValidator implements FieldValidator {
    
    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 65535;
    
    private final boolean allowZero;
    
    /**
     * 기본 생성자 (0 포트 허용)
     */
    public PortValidator() {
        this(true);
    }
    
    /**
     * 0 포트 허용 여부를 지정하는 생성자
     * @param allowZero 0 포트 허용 여부
     */
    public PortValidator(boolean allowZero) {
        this.allowZero = allowZero;
    }
    
    @Override
    public boolean validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        try {
            int port = Integer.parseInt(value.trim());
            
            if (!allowZero && port == 0) {
                return false;
            }
            
            return port >= MIN_PORT && port <= MAX_PORT;
            
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    @Override
    public String getFieldType() {
        return "PORT";
    }
    
    @Override
    public String getDescription() {
        return allowZero ? "포트 번호 (0-65535)" : "포트 번호 (1-65535)";
    }
    
    /**
     * Well-known 포트인지 확인 (0-1023)
     * @param port 포트 번호
     * @return Well-known 포트 여부
     */
    public static boolean isWellKnownPort(int port) {
        return port >= 0 && port <= 1023;
    }
    
    /**
     * Registered 포트인지 확인 (1024-49151)
     * @param port 포트 번호
     * @return Registered 포트 여부
     */
    public static boolean isRegisteredPort(int port) {
        return port >= 1024 && port <= 49151;
    }
    
    /**
     * Dynamic/Private 포트인지 확인 (49152-65535)
     * @param port 포트 번호
     * @return Dynamic/Private 포트 여부
     */
    public static boolean isDynamicPort(int port) {
        return port >= 49152 && port <= 65535;
    }
}