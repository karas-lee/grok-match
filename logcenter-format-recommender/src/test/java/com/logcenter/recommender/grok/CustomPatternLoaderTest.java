package com.logcenter.recommender.grok;

import com.logcenter.recommender.model.GrokPattern;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
import java.util.Map;

/**
 * CustomPatternLoader 단위 테스트
 */
public class CustomPatternLoaderTest {
    
    @Test
    public void testLoadCustomPatterns() {
        List<GrokPattern> patterns = CustomPatternLoader.loadCustomPatterns("custom-grok-patterns");
        
        assertNotNull(patterns);
        assertEquals(198, patterns.size()); // 198개의 커스텀 패턴 (주석과 빈 줄 제외)
        
        // 카테고리가 설정되었는지 확인
        for (GrokPattern pattern : patterns) {
            assertNotNull(pattern.getName());
            assertNotNull(pattern.getPattern());
            assertNotNull(pattern.getCategory());
            assertEquals("CUSTOM", pattern.getType());
        }
    }
    
    @Test
    public void testGroupByCategory() {
        List<GrokPattern> patterns = CustomPatternLoader.loadCustomPatterns();
        Map<String, List<GrokPattern>> grouped = CustomPatternLoader.groupByCategory(patterns);
        
        assertNotNull(grouped);
        assertTrue(grouped.size() > 0);
        
        // 주요 카테고리 확인
        assertTrue(grouped.containsKey("TEXT"));
        assertTrue(grouped.containsKey("IP"));
        assertTrue(grouped.containsKey("PORT"));
        assertTrue(grouped.containsKey("DATE_TIME"));
        assertTrue(grouped.containsKey("NUMBER"));
        
        // TEXT 패턴 개수 확인 (TEXT1-TEXT18)
        List<GrokPattern> textPatterns = grouped.get("TEXT");
        assertNotNull(textPatterns);
        assertEquals(18, textPatterns.size());
    }
    
    @Test
    public void testFindByName() {
        List<GrokPattern> patterns = CustomPatternLoader.loadCustomPatterns();
        
        // SRC_IP 패턴 찾기
        GrokPattern srcIp = CustomPatternLoader.findByName(patterns, "SRC_IP");
        assertNotNull(srcIp);
        assertEquals("SRC_IP", srcIp.getName());
        assertEquals("IP", srcIp.getCategory());
        
        // 존재하지 않는 패턴
        GrokPattern notFound = CustomPatternLoader.findByName(patterns, "NOT_EXIST");
        assertNull(notFound);
    }
    
    @Test
    public void testValidatePatterns() {
        List<GrokPattern> patterns = CustomPatternLoader.loadCustomPatterns();
        List<GrokPattern> validPatterns = CustomPatternLoader.validatePatterns(patterns);
        
        assertNotNull(validPatterns);
        // 모든 패턴이 유효해야 함
        assertEquals(patterns.size(), validPatterns.size());
    }
    
    @Test
    public void testSpecificPatterns() {
        List<GrokPattern> patterns = CustomPatternLoader.loadCustomPatterns();
        
        // IP 패턴 확인
        GrokPattern srcIp = CustomPatternLoader.findByName(patterns, "SRC_IP");
        assertNotNull(srcIp);
        assertTrue(srcIp.getPattern().contains("25[0-5]"));
        
        // DATE_FORMAT1 패턴 확인
        GrokPattern dateFormat1 = CustomPatternLoader.findByName(patterns, "DATE_FORMAT1");
        assertNotNull(dateFormat1);
        assertEquals("DATE_TIME", dateFormat1.getCategory());
        
        // COUNT 패턴 확인
        GrokPattern count = CustomPatternLoader.findByName(patterns, "COUNT");
        assertNotNull(count);
        assertEquals("NUMBER", count.getCategory());
        
        // CISCO1 패턴 확인
        GrokPattern cisco1 = CustomPatternLoader.findByName(patterns, "CISCO1");
        assertNotNull(cisco1);
        assertEquals("VENDOR_SPECIFIC", cisco1.getCategory());
    }
    
    @Test
    public void testLoadNonExistentFile() {
        List<GrokPattern> patterns = CustomPatternLoader.loadCustomPatterns("non-existent-file");
        
        assertNotNull(patterns);
        assertTrue(patterns.isEmpty());
    }
}