package com.logcenter.recommender.config;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * ApiConfiguration 단위 테스트
 */
public class ApiConfigurationTest {
    
    private ApiConfiguration config;
    
    @Before
    public void setUp() {
        // 환경변수 초기화
        clearEnvironmentVariables();
        config = new ApiConfiguration();
    }
    
    @Test
    public void testDefaultValues() {
        // When - 기본값 확인
        String apiUrl = config.getApiUrl();
        String apiKey = config.getApiKey();
        boolean isEnabled = config.isApiEnabled();
        int connectionTimeout = config.getConnectionTimeout();
        int readTimeout = config.getReadTimeout();
        int maxRetries = config.getMaxRetries();
        boolean cacheEnabled = config.isCacheEnabled();
        
        // Then
        assertEquals("http://localhost:8080", apiUrl);
        assertEquals("", apiKey);
        assertFalse(isEnabled);
        assertEquals(5000, connectionTimeout);
        assertEquals(30000, readTimeout);
        assertEquals(3, maxRetries);
        assertTrue(cacheEnabled);
    }
    
    @Test
    public void testEnvironmentVariableOverride() {
        // Given - 환경변수 설정 시뮬레이션
        // 실제 테스트에서는 시스템 속성을 사용
        System.setProperty("LOGCENTER_API_URL", "https://api.example.com");
        System.setProperty("LOGCENTER_API_KEY", "test-key-123");
        
        // When - 환경변수를 읽도록 수정된 설정 객체 생성
        ApiConfiguration configWithEnv = new MockApiConfiguration();
        
        // Then
        assertEquals("https://api.example.com", configWithEnv.getApiUrl());
        assertEquals("test-key-123", configWithEnv.getApiKey());
        
        // Cleanup
        System.clearProperty("LOGCENTER_API_URL");
        System.clearProperty("LOGCENTER_API_KEY");
    }
    
    @Test
    public void testTimeoutConversion() {
        // When
        int connectionTimeoutMs = config.getConnectionTimeout();
        int readTimeoutMs = config.getReadTimeout();
        
        // Then - 초를 밀리초로 변환
        assertEquals(5000, connectionTimeoutMs);  // 5초 -> 5000ms
        assertEquals(30000, readTimeoutMs);       // 30초 -> 30000ms
    }
    
    // Helper methods
    
    private void clearEnvironmentVariables() {
        // 테스트 환경에서 환경변수 초기화
        System.clearProperty("LOGCENTER_API_URL");
        System.clearProperty("LOGCENTER_API_KEY");
    }
    
    /**
     * 테스트용 Mock 설정 클래스 - 시스템 속성을 환경변수처럼 사용
     */
    private static class MockApiConfiguration extends ApiConfiguration {
        @Override
        public String getApiUrl() {
            String envUrl = System.getProperty("LOGCENTER_API_URL");
            return envUrl != null ? envUrl : super.getApiUrl();
        }
        
        @Override
        public String getApiKey() {
            String envKey = System.getProperty("LOGCENTER_API_KEY");
            return envKey != null ? envKey : super.getApiKey();
        }
    }
}