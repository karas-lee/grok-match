package com.logcenter.recommender.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * API 설정 관리 클래스
 */
public class ApiConfiguration {
    
    private static final String CONFIG_FILE = "api.properties";
    private static final String ENV_API_URL = "LOGCENTER_API_URL";
    private static final String ENV_API_KEY = "LOGCENTER_API_KEY";
    
    private final Properties properties;
    
    public ApiConfiguration() {
        this.properties = new Properties();
        loadConfiguration();
    }
    
    /**
     * 설정 로드
     * 우선순위: 환경변수 > 설정파일 > 기본값
     */
    private void loadConfiguration() {
        // 설정 파일 로드 시도
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException e) {
            // 설정 파일이 없어도 계속 진행
        }
        
        // 환경변수로 오버라이드
        String envApiUrl = System.getenv(ENV_API_URL);
        if (envApiUrl != null) {
            properties.setProperty("api.url", envApiUrl);
        }
        
        String envApiKey = System.getenv(ENV_API_KEY);
        if (envApiKey != null) {
            properties.setProperty("api.key", envApiKey);
        }
    }
    
    /**
     * API URL 반환
     */
    public String getApiUrl() {
        return properties.getProperty("api.url", "http://localhost:8080");
    }
    
    /**
     * API 키 반환
     */
    public String getApiKey() {
        return properties.getProperty("api.key", "");
    }
    
    /**
     * API 사용 여부
     */
    public boolean isApiEnabled() {
        String enabled = properties.getProperty("api.enabled", "false");
        return Boolean.parseBoolean(enabled);
    }
    
    /**
     * 연결 타임아웃 (초)
     */
    public int getConnectionTimeout() {
        String timeout = properties.getProperty("api.connection.timeout", "5");
        return Integer.parseInt(timeout) * 1000;
    }
    
    /**
     * 읽기 타임아웃 (초)
     */
    public int getReadTimeout() {
        String timeout = properties.getProperty("api.read.timeout", "30");
        return Integer.parseInt(timeout) * 1000;
    }
    
    /**
     * 최대 재시도 횟수
     */
    public int getMaxRetries() {
        String retries = properties.getProperty("api.max.retries", "3");
        return Integer.parseInt(retries);
    }
    
    /**
     * 캐시 사용 여부
     */
    public boolean isCacheEnabled() {
        String enabled = properties.getProperty("api.cache.enabled", "true");
        return Boolean.parseBoolean(enabled);
    }
}