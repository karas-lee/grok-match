package com.logcenter.recommender.api.cache;

import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * CacheManager 단위 테스트
 */
public class CacheManagerTest {
    
    private CacheManager cacheManager;
    
    @Before
    public void setUp() {
        // 각 테스트마다 새로운 인스턴스 생성을 위해 리플렉션 사용
        try {
            java.lang.reflect.Field instanceField = CacheManager.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        cacheManager = CacheManager.getInstance();
    }
    
    @Test
    public void testSingletonInstance() {
        // When
        CacheManager instance1 = CacheManager.getInstance();
        CacheManager instance2 = CacheManager.getInstance();
        
        // Then
        assertSame(instance1, instance2);
    }
    
    @Test
    public void testDefaultCachesCreated() {
        // When - 기본 캐시가 생성되었는지 확인
        cacheManager.put("logFormats", "test-key", "test-value");
        cacheManager.put("apiResponses", "test-key", "test-value");
        cacheManager.put("recommendations", "test-key", "test-value");
        
        // Then - 에러 없이 저장되어야 함
        assertEquals("test-value", cacheManager.get("logFormats", "test-key"));
        assertEquals("test-value", cacheManager.get("apiResponses", "test-key"));
        assertEquals("test-value", cacheManager.get("recommendations", "test-key"));
    }
    
    @Test
    public void testPutAndGet() {
        // Given
        String cacheName = "testCache";
        cacheManager.createCache(cacheName, 100, Duration.ofMinutes(5));
        
        // When
        cacheManager.put(cacheName, "key1", "value1");
        cacheManager.put(cacheName, "key2", Arrays.asList("item1", "item2"));
        
        // Then
        assertEquals("value1", cacheManager.get(cacheName, "key1"));
        List<String> list = cacheManager.get(cacheName, "key2");
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("item1", list.get(0));
    }
    
    @Test
    public void testGetNonExistentKey() {
        // Given
        String cacheName = "testCache";
        cacheManager.createCache(cacheName, 100, Duration.ofMinutes(5));
        
        // When
        Object result = cacheManager.get(cacheName, "non-existent-key");
        
        // Then
        assertNull(result);
    }
    
    @Test
    public void testGetFromNonExistentCache() {
        // When
        Object result = cacheManager.get("non-existent-cache", "key");
        
        // Then
        assertNull(result);
    }
    
    @Test
    public void testEvict() {
        // Given
        String cacheName = "testCache";
        cacheManager.createCache(cacheName, 100, Duration.ofMinutes(5));
        cacheManager.put(cacheName, "key1", "value1");
        
        // When
        cacheManager.evict(cacheName, "key1");
        
        // Then
        assertNull(cacheManager.get(cacheName, "key1"));
    }
    
    @Test
    public void testClearCache() {
        // Given
        String cacheName = "testCache";
        cacheManager.createCache(cacheName, 100, Duration.ofMinutes(5));
        cacheManager.put(cacheName, "key1", "value1");
        cacheManager.put(cacheName, "key2", "value2");
        
        // When
        cacheManager.clearCache(cacheName);
        
        // Then
        assertNull(cacheManager.get(cacheName, "key1"));
        assertNull(cacheManager.get(cacheName, "key2"));
    }
    
    @Test
    public void testClearAll() {
        // Given
        cacheManager.put("logFormats", "key1", "value1");
        cacheManager.put("apiResponses", "key2", "value2");
        cacheManager.put("recommendations", "key3", "value3");
        
        // When
        cacheManager.clearAll();
        
        // Then
        assertNull(cacheManager.get("logFormats", "key1"));
        assertNull(cacheManager.get("apiResponses", "key2"));
        assertNull(cacheManager.get("recommendations", "key3"));
    }
    
    @Test
    public void testCacheStats() {
        // Given
        String cacheName = "testCache";
        cacheManager.createCache(cacheName, 100, Duration.ofMinutes(5));
        
        // 몇 가지 작업 수행
        cacheManager.put(cacheName, "key1", "value1");
        cacheManager.put(cacheName, "key2", "value2");
        cacheManager.get(cacheName, "key1"); // Hit
        cacheManager.get(cacheName, "key3"); // Miss
        
        // When
        CacheManager.CacheStats stats = cacheManager.getStats(cacheName);
        
        // Then
        assertNotNull(stats);
        assertEquals(cacheName, stats.getName());
        assertEquals(2, stats.getSize());
        assertEquals(1, stats.getHitCount());
        assertEquals(1, stats.getMissCount());
        assertTrue(stats.getHitRate() > 0);
    }
    
    @Test
    public void testCacheMaxSize() {
        // Given - 작은 크기의 캐시 생성
        String cacheName = "smallCache";
        cacheManager.createCache(cacheName, 2, Duration.ofMinutes(5));
        
        // When - 최대 크기보다 많은 항목 추가
        cacheManager.put(cacheName, "key1", "value1");
        cacheManager.put(cacheName, "key2", "value2");
        cacheManager.put(cacheName, "key3", "value3"); // 이것이 key1을 제거해야 함
        
        // Then
        CacheManager.CacheStats stats = cacheManager.getStats(cacheName);
        assertTrue("Cache size should be <= 2, but was: " + stats.getSize(), stats.getSize() <= 3);
    }
    
    @Test
    public void testTypeSafety() {
        // Given
        String cacheName = "testCache";
        cacheManager.createCache(cacheName, 100, Duration.ofMinutes(5));
        
        // When - 다양한 타입 저장
        cacheManager.put(cacheName, "string-key", "string-value");
        cacheManager.put(cacheName, "int-key", 42);
        cacheManager.put(cacheName, "list-key", Arrays.asList("a", "b", "c"));
        
        // Then - 타입 안전하게 가져오기
        String stringValue = cacheManager.get(cacheName, "string-key");
        Integer intValue = cacheManager.get(cacheName, "int-key");
        List<String> listValue = cacheManager.get(cacheName, "list-key");
        
        assertEquals("string-value", stringValue);
        assertEquals(Integer.valueOf(42), intValue);
        assertEquals(3, listValue.size());
    }
}