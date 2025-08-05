package com.logcenter.recommender.config;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 애플리케이션 설정 관리 클래스
 * 설정값 로딩, 캐싱, 기본값 처리
 */
public class AppConfig {
    
    private static final AppConfig INSTANCE = new AppConfig();
    
    private Properties properties;
    private Map<String, Object> cache;
    
    // 설정 키 상수
    public static final String GROK_PATTERNS_PATH = "grok.patterns.path";
    public static final String CUSTOM_GROK_PATTERNS_PATH = "grok.patterns.custom.path";
    public static final String LOG_FORMATS_PATH = "log.formats.path";
    public static final String PARALLEL_PROCESSING_ENABLED = "processing.parallel.enabled";
    public static final String PARALLEL_THREAD_COUNT = "processing.parallel.threads";
    public static final String CACHE_ENABLED = "cache.enabled";
    public static final String CACHE_SIZE = "cache.size";
    public static final String MATCH_TIMEOUT = "match.timeout.ms";
    public static final String MAX_LOG_SIZE = "log.max.size.bytes";
    public static final String DEFAULT_ENCODING = "log.default.encoding";
    public static final String DEBUG_MODE = "debug.mode";
    public static final String OUTPUT_FORMAT = "output.format";
    public static final String CONFIDENCE_THRESHOLD = "confidence.threshold";
    
    // 영구 캐시 설정 키
    public static final String PERSISTENT_CACHE_ENABLED = "cache.persistent.enabled";
    public static final String PERSISTENT_CACHE_DIR = "cache.persistent.dir";
    public static final String PERSISTENT_CACHE_TTL_DAYS = "cache.persistent.ttl.days";
    public static final String PERSISTENT_CACHE_CHECKSUM_ENABLED = "cache.persistent.checksum.enabled";
    
    // 기본값
    private static final Map<String, String> DEFAULT_VALUES = new ConcurrentHashMap<>();
    
    static {
        // 기본값 설정
        DEFAULT_VALUES.put(GROK_PATTERNS_PATH, "grok-patterns/patterns");
        DEFAULT_VALUES.put(CUSTOM_GROK_PATTERNS_PATH, "custom-grok-patterns");
        DEFAULT_VALUES.put(LOG_FORMATS_PATH, "setting_logformat.json");
        DEFAULT_VALUES.put(PARALLEL_PROCESSING_ENABLED, "true");
        DEFAULT_VALUES.put(PARALLEL_THREAD_COUNT, "4");
        DEFAULT_VALUES.put(CACHE_ENABLED, "true");
        DEFAULT_VALUES.put(CACHE_SIZE, "1000");
        DEFAULT_VALUES.put(MATCH_TIMEOUT, "5000");
        DEFAULT_VALUES.put(MAX_LOG_SIZE, "1048576"); // 1MB
        DEFAULT_VALUES.put(DEFAULT_ENCODING, "UTF-8");
        DEFAULT_VALUES.put(DEBUG_MODE, "false");
        DEFAULT_VALUES.put(OUTPUT_FORMAT, "text");
        DEFAULT_VALUES.put(CONFIDENCE_THRESHOLD, "70.0");
        
        // 영구 캐시 기본값
        DEFAULT_VALUES.put(PERSISTENT_CACHE_ENABLED, "true");
        DEFAULT_VALUES.put(PERSISTENT_CACHE_DIR, ".logcenter/cache");
        DEFAULT_VALUES.put(PERSISTENT_CACHE_TTL_DAYS, "7");
        DEFAULT_VALUES.put(PERSISTENT_CACHE_CHECKSUM_ENABLED, "true");
    }
    
    private AppConfig() {
        this.properties = new Properties();
        this.cache = new ConcurrentHashMap<>();
    }
    
    /**
     * 싱글톤 인스턴스 반환
     */
    public static AppConfig getInstance() {
        return INSTANCE;
    }
    
    /**
     * Properties 설정
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
        this.cache.clear(); // 캐시 초기화
    }
    
    /**
     * 문자열 설정값 조회
     */
    public String getString(String key) {
        return getString(key, DEFAULT_VALUES.get(key));
    }
    
    /**
     * 문자열 설정값 조회 (기본값 지정)
     */
    public String getString(String key, String defaultValue) {
        // 캐시 확인
        if (cache.containsKey(key)) {
            return (String) cache.get(key);
        }
        
        // 환경 변수 확인 (대문자로 변환하고 점을 언더스코어로 변경)
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null) {
            cache.put(key, envValue);
            return envValue;
        }
        
        // Properties 확인
        String value = properties.getProperty(key, defaultValue);
        cache.put(key, value);
        return value;
    }
    
    /**
     * 정수 설정값 조회
     */
    public int getInt(String key) {
        String defaultValue = DEFAULT_VALUES.get(key);
        return getInt(key, defaultValue != null ? Integer.parseInt(defaultValue) : 0);
    }
    
    /**
     * 정수 설정값 조회 (기본값 지정)
     */
    public int getInt(String key, int defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 불린 설정값 조회
     */
    public boolean getBoolean(String key) {
        String defaultValue = DEFAULT_VALUES.get(key);
        return getBoolean(key, defaultValue != null && Boolean.parseBoolean(defaultValue));
    }
    
    /**
     * 불린 설정값 조회 (기본값 지정)
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }
    
    /**
     * 더블 설정값 조회
     */
    public double getDouble(String key) {
        String defaultValue = DEFAULT_VALUES.get(key);
        return getDouble(key, defaultValue != null ? Double.parseDouble(defaultValue) : 0.0);
    }
    
    /**
     * 더블 설정값 조회 (기본값 지정)
     */
    public double getDouble(String key, double defaultValue) {
        String value = getString(key, String.valueOf(defaultValue));
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 설정값 존재 여부 확인
     */
    public boolean hasProperty(String key) {
        return System.getenv(key.toUpperCase().replace('.', '_')) != null ||
               properties.containsKey(key);
    }
    
    /**
     * 설정값 설정
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        cache.put(key, value);
    }
    
    /**
     * 캐시 초기화
     */
    public void clearCache() {
        cache.clear();
    }
    
    /**
     * 모든 설정 출력 (디버그용)
     */
    public void printAllSettings() {
        System.out.println("=== Application Configuration ===");
        for (String key : DEFAULT_VALUES.keySet()) {
            System.out.println(key + " = " + getString(key));
        }
        System.out.println("================================");
    }
    
    /**
     * 병렬 처리 사용 여부
     */
    public boolean isParallelProcessingEnabled() {
        return getBoolean(PARALLEL_PROCESSING_ENABLED);
    }
    
    /**
     * 캐시 사용 여부
     */
    public boolean isCacheEnabled() {
        return getBoolean(CACHE_ENABLED);
    }
    
    /**
     * 디버그 모드 여부
     */
    public boolean isDebugMode() {
        return getBoolean(DEBUG_MODE);
    }
    
    /**
     * 신뢰도 임계값 반환
     */
    public double getConfidenceThreshold() {
        return getDouble(CONFIDENCE_THRESHOLD);
    }
}