package com.logcenter.recommender.config;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Properties;

/**
 * AppConfig 설정 관리 클래스 단위 테스트
 */
public class AppConfigTest {
    
    private AppConfig config;
    private Properties testProperties;
    
    @Before
    public void setUp() {
        config = AppConfig.getInstance();
        config.clearCache();
        
        testProperties = new Properties();
        testProperties.setProperty("test.string", "test value");
        testProperties.setProperty("test.int", "42");
        testProperties.setProperty("test.boolean", "true");
        testProperties.setProperty("test.double", "3.14");
        
        config.setProperties(testProperties);
    }
    
    @Test
    public void testSingleton() {
        AppConfig instance1 = AppConfig.getInstance();
        AppConfig instance2 = AppConfig.getInstance();
        assertSame(instance1, instance2);
    }
    
    @Test
    public void testGetString() {
        assertEquals("test value", config.getString("test.string"));
        assertEquals("default", config.getString("nonexistent", "default"));
        
        // 기본값 테스트
        String defaultEncoding = config.getString(AppConfig.DEFAULT_ENCODING);
        assertEquals("UTF-8", defaultEncoding);
    }
    
    @Test
    public void testGetInt() {
        assertEquals(42, config.getInt("test.int"));
        assertEquals(100, config.getInt("nonexistent", 100));
        
        // 잘못된 형식
        config.setProperty("test.invalid.int", "not a number");
        assertEquals(0, config.getInt("test.invalid.int"));
        assertEquals(50, config.getInt("test.invalid.int", 50));
    }
    
    @Test
    public void testGetBoolean() {
        assertTrue(config.getBoolean("test.boolean"));
        assertFalse(config.getBoolean("nonexistent", false));
        
        // 다양한 boolean 값
        config.setProperty("test.true", "TRUE");
        config.setProperty("test.false", "false");
        assertTrue(config.getBoolean("test.true"));
        assertFalse(config.getBoolean("test.false"));
    }
    
    @Test
    public void testGetDouble() {
        assertEquals(3.14, config.getDouble("test.double"), 0.001);
        assertEquals(2.5, config.getDouble("nonexistent", 2.5), 0.001);
        
        // 잘못된 형식
        config.setProperty("test.invalid.double", "not a number");
        assertEquals(0.0, config.getDouble("test.invalid.double"), 0.001);
        assertEquals(1.5, config.getDouble("test.invalid.double", 1.5), 0.001);
    }
    
    @Test
    public void testDefaultValues() {
        // 기본값이 설정되어 있는지 확인
        assertEquals("grok-patterns/patterns", config.getString(AppConfig.GROK_PATTERNS_PATH));
        assertEquals("custom-grok-patterns", config.getString(AppConfig.CUSTOM_GROK_PATTERNS_PATH));
        assertEquals("GROK-PATTERN-CONVERTER.sql", config.getString(AppConfig.LOG_FORMATS_PATH));
        assertTrue(config.getBoolean(AppConfig.PARALLEL_PROCESSING_ENABLED));
        assertEquals(4, config.getInt(AppConfig.PARALLEL_THREAD_COUNT));
        assertTrue(config.getBoolean(AppConfig.CACHE_ENABLED));
        assertEquals(1000, config.getInt(AppConfig.CACHE_SIZE));
        assertEquals(5000, config.getInt(AppConfig.MATCH_TIMEOUT));
        assertEquals(1048576, config.getInt(AppConfig.MAX_LOG_SIZE));
        assertEquals("UTF-8", config.getString(AppConfig.DEFAULT_ENCODING));
        assertFalse(config.getBoolean(AppConfig.DEBUG_MODE));
        assertEquals("text", config.getString(AppConfig.OUTPUT_FORMAT));
        assertEquals(70.0, config.getDouble(AppConfig.CONFIDENCE_THRESHOLD), 0.001);
    }
    
    @Test
    public void testHasProperty() {
        assertTrue(config.hasProperty("test.string"));
        assertFalse(config.hasProperty("nonexistent.property"));
    }
    
    @Test
    public void testSetProperty() {
        config.setProperty("new.property", "new value");
        assertEquals("new value", config.getString("new.property"));
        
        // 캐시 확인
        config.setProperty("cached.property", "cached value");
        testProperties.remove("cached.property"); // Properties에서 제거
        assertEquals("cached value", config.getString("cached.property")); // 캐시에서 가져옴
    }
    
    @Test
    public void testConvenienceMethods() {
        assertTrue(config.isParallelProcessingEnabled());
        assertTrue(config.isCacheEnabled());
        assertFalse(config.isDebugMode());
        assertEquals(70.0, config.getConfidenceThreshold(), 0.001);
    }
    
    @Test
    public void testClearCache() {
        config.setProperty("cached.property", "value");
        assertEquals("value", config.getString("cached.property"));
        
        config.clearCache();
        
        // 캐시가 비워졌지만 Properties에는 여전히 존재
        assertEquals("value", config.getString("cached.property"));
        
        // Properties를 새로 설정하면 캐시도 초기화됨
        Properties newProps = new Properties();
        config.setProperties(newProps);
        assertEquals("default", config.getString("cached.property", "default"));
    }
}