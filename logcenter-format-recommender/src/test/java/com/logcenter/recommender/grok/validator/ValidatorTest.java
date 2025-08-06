package com.logcenter.recommender.grok.validator;

import com.logcenter.recommender.grok.FieldValidator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Validator 구현체들의 단위 테스트
 */
public class ValidatorTest {
    
    @Test
    public void testIPValidator() {
        FieldValidator.IPFieldValidator validator = new FieldValidator.IPFieldValidator();
        
        // 유효한 IPv4
        assertTrue(validator.validate("192.168.1.1"));
        assertTrue(validator.validate("10.0.0.0"));
        assertTrue(validator.validate("255.255.255.255"));
        assertTrue(validator.validate("127.0.0.1"));
        
        // 유효하지 않은 IPv4
        assertFalse(validator.validate("256.256.256.256"));
        assertFalse(validator.validate("192.168.1"));
        assertFalse(validator.validate("192.168.1.1.1"));
        assertFalse(validator.validate("abc.def.ghi.jkl"));
        
        // 특수 값
        assertTrue(validator.validate("-"));
        assertTrue(validator.validate("?.?.?.?"));
        
        // null/empty
        assertFalse(validator.validate(null));
        assertFalse(validator.validate(""));
        assertFalse(validator.validate("   "));
    }
    
    @Test
    public void testPortValidator() {
        FieldValidator.PortFieldValidator validator = new FieldValidator.PortFieldValidator();
        
        // 유효한 포트
        assertTrue(validator.validate("0"));
        assertTrue(validator.validate("80"));
        assertTrue(validator.validate("443"));
        assertTrue(validator.validate("8080"));
        assertTrue(validator.validate("65535"));
        
        // 유효하지 않은 포트
        assertFalse(validator.validate("-1"));
        assertFalse(validator.validate("65536"));
        assertFalse(validator.validate("999999"));
        assertFalse(validator.validate("abc"));
        assertFalse(validator.validate("80.0"));
        
        // 특수 값
        assertTrue(validator.validate("-"));
        
        // null/empty
        assertFalse(validator.validate(null));
        assertFalse(validator.validate(""));
    }
    
    @Test
    public void testTimestampValidator() {
        FieldValidator.TimestampFieldValidator validator = new FieldValidator.TimestampFieldValidator();
        
        // 유효한 타임스탬프
        assertTrue(validator.validate("2024-01-15 10:30:45"));
        assertTrue(validator.validate("20240115103045"));
        assertTrue(validator.validate("2024-01-15T10:30:45.123Z"));
        assertTrue(validator.validate("13:48:22"));
        assertTrue(validator.validate("13:48:22.229395"));
        assertTrue(validator.validate("Mar 13 13:48:22"));
        
        // 유효하지 않은 타임스탬프
        assertFalse(validator.validate("not a date"));
        assertFalse(validator.validate("192.168.1.1")); // IP 주소
        
        // null/empty
        assertFalse(validator.validate(null));
        assertFalse(validator.validate(""));
    }
    
    @Test
    public void testHTTPStatusValidator() {
        FieldValidator.HTTPStatusFieldValidator validator = new FieldValidator.HTTPStatusFieldValidator();
        
        // 유효한 상태 코드
        assertTrue(validator.validate("200"));
        assertTrue(validator.validate("404"));
        assertTrue(validator.validate("500"));
        assertTrue(validator.validate("301"));
        assertTrue(validator.validate("100"));
        assertTrue(validator.validate("599"));
        
        // 유효하지 않은 상태 코드 (일반 모드에서는 100-599 범위)
        assertFalse(validator.validate("99"));
        assertFalse(validator.validate("600"));
        assertFalse(validator.validate("1000"));
        assertFalse(validator.validate("-200"));
        assertFalse(validator.validate("200.0"));
        assertFalse(validator.validate("OK"));
        
        // null/empty
        assertFalse(validator.validate(null));
        assertFalse(validator.validate(""));
    }
    
    @Test
    public void testSemanticValidation() {
        // IP 필드에 시간값이 들어온 경우
        FieldValidator.IPFieldValidator ipValidator = new FieldValidator.IPFieldValidator();
        assertFalse(ipValidator.validateSemantics("src_ip", "13:48:22"));
        assertFalse(ipValidator.validateSemantics("dst_ip", "13:48:22.229395"));
        assertTrue(ipValidator.validateSemantics("src_ip", "192.168.1.188"));
        
        // 디바이스명 필드에 시간값이 들어온 경우
        FieldValidator.DeviceNameFieldValidator deviceValidator = new FieldValidator.DeviceNameFieldValidator();
        assertFalse(deviceValidator.validateSemantics("device_name", "13:48:22.229395"));
        assertTrue(deviceValidator.validateSemantics("device_name", "ipmon"));
        
        // 액션 필드에 숫자만 들어온 경우
        FieldValidator.ActionFieldValidator actionValidator = new FieldValidator.ActionFieldValidator();
        assertFalse(actionValidator.validateSemantics("action", "13:48:22"));
        assertTrue(actionValidator.validateSemantics("action", "p"));
        assertTrue(actionValidator.validateSemantics("action", "allow"));
    }
}