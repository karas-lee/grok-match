package com.logcenter.recommender.grok.validator;

import com.logcenter.recommender.grok.FieldValidator;
import java.util.regex.Pattern;

/**
 * IP 주소 검증기
 * IPv4 및 IPv6 주소의 유효성을 검증
 */
public class IPValidator implements FieldValidator {
    
    // IPv4 패턴
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    // IPv6 패턴 (간단한 버전)
    private static final Pattern IPV6_PATTERN = Pattern.compile(
        "^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|" +
        "([0-9a-fA-F]{1,4}:){1,7}:|" +
        "([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|" +
        "([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|" +
        "([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|" +
        "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|" +
        "([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|" +
        "[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|" +
        ":((:[0-9a-fA-F]{1,4}){1,7}|:)|" +
        "fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|" +
        "::(ffff(:0{1,4}){0,1}:){0,1}" +
        "((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3}" +
        "(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|" +
        "([0-9a-fA-F]{1,4}:){1,4}:" +
        "((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3}" +
        "(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))$"
    );
    
    private final boolean allowIpv6;
    
    /**
     * 기본 생성자 (IPv4와 IPv6 모두 허용)
     */
    public IPValidator() {
        this(true);
    }
    
    /**
     * IPv6 허용 여부를 지정하는 생성자
     * @param allowIpv6 IPv6 허용 여부
     */
    public IPValidator(boolean allowIpv6) {
        this.allowIpv6 = allowIpv6;
    }
    
    @Override
    public boolean validate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        
        value = value.trim();
        
        // IPv4 검증
        if (IPV4_PATTERN.matcher(value).matches()) {
            return true;
        }
        
        // IPv6 검증 (허용된 경우)
        if (allowIpv6 && IPV6_PATTERN.matcher(value).matches()) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public String getFieldType() {
        return "IP";
    }
    
    @Override
    public String getDescription() {
        return allowIpv6 ? "IPv4 또는 IPv6 주소" : "IPv4 주소";
    }
    
    /**
     * IPv4 주소인지 확인
     * @param value 검증할 값
     * @return IPv4 여부
     */
    public static boolean isIPv4(String value) {
        return value != null && IPV4_PATTERN.matcher(value).matches();
    }
    
    /**
     * IPv6 주소인지 확인
     * @param value 검증할 값
     * @return IPv6 여부
     */
    public static boolean isIPv6(String value) {
        return value != null && IPV6_PATTERN.matcher(value).matches();
    }
}