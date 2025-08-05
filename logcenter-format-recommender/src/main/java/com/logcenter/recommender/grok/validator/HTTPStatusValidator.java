package com.logcenter.recommender.grok.validator;

import com.logcenter.recommender.grok.FieldValidator;
import java.util.HashSet;
import java.util.Set;

/**
 * HTTP 상태 코드 검증기
 * HTTP 상태 코드의 유효성을 검증 (100-599)
 */
public class HTTPStatusValidator implements FieldValidator {
    
    // 유효한 HTTP 상태 코드 집합
    private static final Set<Integer> VALID_STATUS_CODES = new HashSet<>();
    
    static {
        // 1xx Informational
        VALID_STATUS_CODES.add(100); // Continue
        VALID_STATUS_CODES.add(101); // Switching Protocols
        VALID_STATUS_CODES.add(102); // Processing
        VALID_STATUS_CODES.add(103); // Early Hints
        
        // 2xx Success
        VALID_STATUS_CODES.add(200); // OK
        VALID_STATUS_CODES.add(201); // Created
        VALID_STATUS_CODES.add(202); // Accepted
        VALID_STATUS_CODES.add(203); // Non-Authoritative Information
        VALID_STATUS_CODES.add(204); // No Content
        VALID_STATUS_CODES.add(205); // Reset Content
        VALID_STATUS_CODES.add(206); // Partial Content
        VALID_STATUS_CODES.add(207); // Multi-Status
        VALID_STATUS_CODES.add(208); // Already Reported
        VALID_STATUS_CODES.add(226); // IM Used
        
        // 3xx Redirection
        VALID_STATUS_CODES.add(300); // Multiple Choices
        VALID_STATUS_CODES.add(301); // Moved Permanently
        VALID_STATUS_CODES.add(302); // Found
        VALID_STATUS_CODES.add(303); // See Other
        VALID_STATUS_CODES.add(304); // Not Modified
        VALID_STATUS_CODES.add(305); // Use Proxy
        VALID_STATUS_CODES.add(307); // Temporary Redirect
        VALID_STATUS_CODES.add(308); // Permanent Redirect
        
        // 4xx Client Error
        VALID_STATUS_CODES.add(400); // Bad Request
        VALID_STATUS_CODES.add(401); // Unauthorized
        VALID_STATUS_CODES.add(402); // Payment Required
        VALID_STATUS_CODES.add(403); // Forbidden
        VALID_STATUS_CODES.add(404); // Not Found
        VALID_STATUS_CODES.add(405); // Method Not Allowed
        VALID_STATUS_CODES.add(406); // Not Acceptable
        VALID_STATUS_CODES.add(407); // Proxy Authentication Required
        VALID_STATUS_CODES.add(408); // Request Timeout
        VALID_STATUS_CODES.add(409); // Conflict
        VALID_STATUS_CODES.add(410); // Gone
        VALID_STATUS_CODES.add(411); // Length Required
        VALID_STATUS_CODES.add(412); // Precondition Failed
        VALID_STATUS_CODES.add(413); // Payload Too Large
        VALID_STATUS_CODES.add(414); // URI Too Long
        VALID_STATUS_CODES.add(415); // Unsupported Media Type
        VALID_STATUS_CODES.add(416); // Range Not Satisfiable
        VALID_STATUS_CODES.add(417); // Expectation Failed
        VALID_STATUS_CODES.add(418); // I'm a teapot
        VALID_STATUS_CODES.add(421); // Misdirected Request
        VALID_STATUS_CODES.add(422); // Unprocessable Entity
        VALID_STATUS_CODES.add(423); // Locked
        VALID_STATUS_CODES.add(424); // Failed Dependency
        VALID_STATUS_CODES.add(425); // Too Early
        VALID_STATUS_CODES.add(426); // Upgrade Required
        VALID_STATUS_CODES.add(428); // Precondition Required
        VALID_STATUS_CODES.add(429); // Too Many Requests
        VALID_STATUS_CODES.add(431); // Request Header Fields Too Large
        VALID_STATUS_CODES.add(451); // Unavailable For Legal Reasons
        
        // 5xx Server Error
        VALID_STATUS_CODES.add(500); // Internal Server Error
        VALID_STATUS_CODES.add(501); // Not Implemented
        VALID_STATUS_CODES.add(502); // Bad Gateway
        VALID_STATUS_CODES.add(503); // Service Unavailable
        VALID_STATUS_CODES.add(504); // Gateway Timeout
        VALID_STATUS_CODES.add(505); // HTTP Version Not Supported
        VALID_STATUS_CODES.add(506); // Variant Also Negotiates
        VALID_STATUS_CODES.add(507); // Insufficient Storage
        VALID_STATUS_CODES.add(508); // Loop Detected
        VALID_STATUS_CODES.add(510); // Not Extended
        VALID_STATUS_CODES.add(511); // Network Authentication Required
    }
    
    private final boolean strictMode;
    
    /**
     * 기본 생성자 (엄격 모드 비활성화)
     */
    public HTTPStatusValidator() {
        this(false);
    }
    
    /**
     * 엄격 모드를 지정하는 생성자
     * @param strictMode true면 정의된 상태 코드만 허용, false면 100-599 범위 허용
     */
    public HTTPStatusValidator(boolean strictMode) {
        this.strictMode = strictMode;
    }
    
    @Override
    public boolean validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        try {
            int statusCode = Integer.parseInt(value.trim());
            
            if (strictMode) {
                // 엄격 모드: 정의된 상태 코드만 허용
                return VALID_STATUS_CODES.contains(statusCode);
            } else {
                // 일반 모드: 100-599 범위 허용
                return statusCode >= 100 && statusCode <= 599;
            }
            
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    @Override
    public String getFieldType() {
        return "HTTP_STATUS";
    }
    
    @Override
    public String getDescription() {
        return strictMode ? "HTTP 상태 코드 (표준 코드만)" : "HTTP 상태 코드 (100-599)";
    }
    
    /**
     * 상태 코드 카테고리 반환
     * @param statusCode 상태 코드
     * @return 카테고리 (1xx, 2xx, 3xx, 4xx, 5xx) 또는 null
     */
    public static String getCategory(int statusCode) {
        if (statusCode >= 100 && statusCode < 200) {
            return "1xx Informational";
        } else if (statusCode >= 200 && statusCode < 300) {
            return "2xx Success";
        } else if (statusCode >= 300 && statusCode < 400) {
            return "3xx Redirection";
        } else if (statusCode >= 400 && statusCode < 500) {
            return "4xx Client Error";
        } else if (statusCode >= 500 && statusCode < 600) {
            return "5xx Server Error";
        }
        return null;
    }
}