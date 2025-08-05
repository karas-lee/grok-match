package com.logcenter.recommender.grok.validator;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Validator 구현체들의 단위 테스트
 */
public class ValidatorTest {
    
    @Test
    public void testIPValidator() {
        IPValidator validator = new IPValidator();
        
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
        
        // IPv6 (기본적으로 허용)
        assertTrue(validator.validate("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
        assertTrue(validator.validate("::1"));
        assertTrue(validator.validate("fe80::"));
        
        // IPv4 전용 모드
        IPValidator ipv4Only = new IPValidator(false);
        assertTrue(ipv4Only.validate("192.168.1.1"));
        assertFalse(ipv4Only.validate("2001:0db8:85a3::8a2e:0370:7334"));
        
        // null/empty
        assertFalse(validator.validate(null));
        assertFalse(validator.validate(""));
        assertFalse(validator.validate("   "));
    }
    
    @Test
    public void testPortValidator() {
        PortValidator validator = new PortValidator();
        
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
        
        // 0 포트 비허용 모드
        PortValidator noZero = new PortValidator(false);
        assertFalse(noZero.validate("0"));
        assertTrue(noZero.validate("1"));
        
        // null/empty
        assertFalse(validator.validate(null));
        assertFalse(validator.validate(""));
        
        // 포트 분류 테스트
        assertTrue(PortValidator.isWellKnownPort(80));
        assertTrue(PortValidator.isRegisteredPort(8080));
        assertTrue(PortValidator.isDynamicPort(50000));
    }
    
    @Test
    public void testTimestampValidator() {
        TimestampValidator validator = new TimestampValidator();
        
        // 유효한 타임스탬프
        assertTrue(validator.validate("2024-01-15 10:30:45"));
        assertTrue(validator.validate("2024/01/15 10:30:45"));
        assertTrue(validator.validate("15/Jan/2024:10:30:45 +0900"));
        // assertTrue(validator.validate("Jan 15 10:30:45")); // 연도 없이는 파싱 불가
        assertTrue(validator.validate("20240115103045"));
        assertTrue(validator.validate("2024-01-15T10:30:45.123Z"));
        
        // Unix timestamp
        assertTrue(validator.validate("1705286400")); // 2024-01-15
        assertTrue(validator.validate("1705286400000")); // 밀리초
        
        // 유효하지 않은 타임스탬프
        assertFalse(validator.validate("not a date"));
        assertFalse(validator.validate("2024-13-45")); // 잘못된 월
        assertFalse(validator.validate("99999999999999999")); // 범위 초과
        
        // null/empty
        assertFalse(validator.validate(null));
        assertFalse(validator.validate(""));
    }
    
    @Test
    public void testHTTPStatusValidator() {
        HTTPStatusValidator validator = new HTTPStatusValidator();
        
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
        
        // 엄격 모드
        HTTPStatusValidator strict = new HTTPStatusValidator(true);
        assertTrue(strict.validate("200")); // 표준 코드
        assertTrue(strict.validate("404")); // 표준 코드
        assertFalse(strict.validate("299")); // 비표준 코드
        
        // null/empty
        assertFalse(validator.validate(null));
        assertFalse(validator.validate(""));
        
        // 카테고리 테스트
        assertEquals("2xx Success", HTTPStatusValidator.getCategory(200));
        assertEquals("4xx Client Error", HTTPStatusValidator.getCategory(404));
        assertEquals("5xx Server Error", HTTPStatusValidator.getCategory(500));
    }
}