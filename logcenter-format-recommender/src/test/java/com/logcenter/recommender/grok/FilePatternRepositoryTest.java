package com.logcenter.recommender.grok;

import com.logcenter.recommender.model.LogFormat;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FilePatternRepository 단위 테스트
 */
public class FilePatternRepositoryTest {
    
    private FilePatternRepository repository;
    
    @Before
    public void setUp() {
        repository = new FilePatternRepository("setting_logformat.json");
    }
    
    @Test
    public void testInitialize() {
        // 초기화
        boolean initialized = repository.initialize();
        
        // 검증
        assertTrue("저장소 초기화 실패", initialized);
        assertTrue("초기화 상태가 true여야 합니다", repository.isInitialized());
        assertTrue("포맷이 로드되어야 합니다", repository.size() > 0);
        
        System.out.println("로드된 포맷 수: " + repository.size());
    }
    
    @Test
    public void testLoadFormats() {
        // 포맷 로드
        int loaded = repository.loadFormats();
        
        // 검증
        assertTrue("포맷이 로드되어야 합니다", loaded > 0);
        assertEquals("size()와 로드된 수가 일치해야 합니다", loaded, repository.size());
    }
    
    @Test
    public void testGetAllFormats() {
        repository.initialize();
        
        // 모든 포맷 가져오기
        List<LogFormat> formats = repository.getAllFormats();
        
        // 검증
        assertNotNull(formats);
        assertFalse("포맷 목록이 비어있으면 안됩니다", formats.isEmpty());
        
        // 첫 번째 포맷 확인
        LogFormat first = formats.get(0);
        assertNotNull("포맷 ID가 있어야 합니다", first.getFormatId());
        assertNotNull("포맷 이름이 있어야 합니다", first.getFormatName());
        
        System.out.println("첫 번째 포맷: " + first.getFormatId() + " - " + first.getFormatName());
    }
    
    @Test
    public void testGetFormatById() {
        repository.initialize();
        List<LogFormat> formats = repository.getAllFormats();
        
        if (!formats.isEmpty()) {
            // 첫 번째 포맷의 ID로 조회
            LogFormat first = formats.get(0);
            String formatId = first.getFormatId();
            
            LogFormat retrieved = repository.getFormatById(formatId);
            
            // 검증
            assertNotNull(retrieved);
            assertEquals(formatId, retrieved.getFormatId());
            assertEquals(first.getFormatName(), retrieved.getFormatName());
        }
    }
    
    @Test
    public void testGetFormatsByGroup() {
        repository.initialize();
        
        // 그룹 통계 확인
        Map<String, Integer> groupStats = repository.getGroupStatistics();
        assertNotNull(groupStats);
        
        if (!groupStats.isEmpty()) {
            // 첫 번째 그룹으로 테스트
            String groupName = groupStats.keySet().iterator().next();
            List<LogFormat> groupFormats = repository.getFormatsByGroup(groupName);
            
            // 검증
            assertNotNull(groupFormats);
            assertFalse("그룹에 포맷이 있어야 합니다", groupFormats.isEmpty());
            
            // 모든 포맷이 해당 그룹이어야 함
            for (LogFormat format : groupFormats) {
                assertEquals(groupName, format.getGroupName());
            }
            
            System.out.println("그룹 '" + groupName + "' 포맷 수: " + groupFormats.size());
        }
    }
    
    @Test
    public void testGetFormatsByVendor() {
        repository.initialize();
        
        // 벤더 통계 확인
        Map<String, Integer> vendorStats = repository.getVendorStatistics();
        assertNotNull(vendorStats);
        
        if (!vendorStats.isEmpty()) {
            // 첫 번째 벤더로 테스트
            String vendor = vendorStats.keySet().iterator().next();
            List<LogFormat> vendorFormats = repository.getFormatsByVendor(vendor);
            
            // 검증
            assertNotNull(vendorFormats);
            assertFalse("벤더에 포맷이 있어야 합니다", vendorFormats.isEmpty());
            
            // 모든 포맷이 해당 벤더여야 함
            for (LogFormat format : vendorFormats) {
                assertEquals(vendor, format.getVendor());
            }
            
            System.out.println("벤더 '" + vendor + "' 포맷 수: " + vendorFormats.size());
        }
    }
    
    @Test
    public void testGetAllGroups() {
        repository.initialize();
        
        // 모든 그룹 가져오기
        Set<String> groups = repository.getAllGroups();
        
        // 검증
        assertNotNull(groups);
        assertFalse("그룹이 있어야 합니다", groups.isEmpty());
        
        System.out.println("그룹 수: " + groups.size());
        System.out.println("그룹 목록: " + groups);
    }
    
    @Test
    public void testGetAllVendors() {
        repository.initialize();
        
        // 모든 벤더 가져오기
        Set<String> vendors = repository.getAllVendors();
        
        // 검증
        assertNotNull(vendors);
        assertFalse("벤더가 있어야 합니다", vendors.isEmpty());
        
        System.out.println("벤더 수: " + vendors.size());
        System.out.println("벤더 목록 (처음 10개): " + 
            vendors.stream().limit(10).reduce((a, b) -> a + ", " + b).orElse(""));
    }
    
    @Test
    public void testStatistics() {
        repository.initialize();
        
        // 그룹 통계
        Map<String, Integer> groupStats = repository.getGroupStatistics();
        assertNotNull(groupStats);
        
        int totalByGroup = groupStats.values().stream().mapToInt(Integer::intValue).sum();
        
        // 벤더 통계
        Map<String, Integer> vendorStats = repository.getVendorStatistics();
        assertNotNull(vendorStats);
        
        int totalByVendor = vendorStats.values().stream().mapToInt(Integer::intValue).sum();
        
        // 전체 포맷 수와 비교
        int total = repository.size();
        
        System.out.println("전체 포맷: " + total);
        System.out.println("그룹별 합계: " + totalByGroup);
        System.out.println("벤더별 합계: " + totalByVendor);
        
        // 그룹별 합계와 전체가 일치해야 함 (모든 포맷이 그룹을 가져야 함)
        assertEquals("그룹별 합계가 전체와 일치해야 합니다", total, totalByGroup);
    }
    
    @Test
    public void testReloadFormats() {
        repository.initialize();
        int initial = repository.size();
        
        // 재로드
        int reloaded = repository.reloadFormats();
        
        // 검증
        assertEquals("재로드 후 같은 수의 포맷이어야 합니다", initial, reloaded);
        assertEquals("size()도 동일해야 합니다", initial, repository.size());
    }
    
    @Test
    public void testClear() {
        repository.initialize();
        assertTrue("초기화 후 포맷이 있어야 합니다", repository.size() > 0);
        
        // 클리어
        repository.clear();
        
        // 검증
        assertEquals("클리어 후 비어있어야 합니다", 0, repository.size());
        assertTrue("모든 포맷이 제거되어야 합니다", repository.getAllFormats().isEmpty());
        assertTrue("모든 그룹이 제거되어야 합니다", repository.getAllGroups().isEmpty());
        assertTrue("모든 벤더가 제거되어야 합니다", repository.getAllVendors().isEmpty());
    }
    
    @Test
    public void testGrokPatternLoading() {
        repository.initialize();
        List<LogFormat> formats = repository.getAllFormats();
        
        System.out.println("총 포맷 수: " + formats.size());
        
        // 일부 포맷이 Grok 패턴을 가져야 함
        int formatsWithGrokPattern = 0;
        int formatsWithLogTypes = 0;
        int formatsWithPatterns = 0;
        
        for (LogFormat format : formats) {
            // 디버깅
            if (format.getLogTypes() != null && !format.getLogTypes().isEmpty()) {
                formatsWithLogTypes++;
                LogFormat.LogType firstLogType = format.getLogTypes().get(0);
                if (firstLogType.getPatterns() != null && !firstLogType.getPatterns().isEmpty()) {
                    formatsWithPatterns++;
                    LogFormat.Pattern firstPattern = firstLogType.getPatterns().get(0);
                    System.out.println("포맷 " + format.getFormatId() + 
                        ", 패턴명: " + firstPattern.getExpName() + 
                        ", Grok 표현식 null?: " + (firstPattern.getGrokExp() == null) +
                        ", Grok 표현식 길이: " + (firstPattern.getGrokExp() != null ? firstPattern.getGrokExp().length() : 0));
                }
            }
            
            System.out.println("포맷 " + format.getFormatId() + " - grokPattern: " + 
                (format.getGrokPattern() == null ? "null" : "not null"));
            
            if (format.getGrokPattern() != null && !format.getGrokPattern().isEmpty()) {
                formatsWithGrokPattern++;
                
                // 첫 번째 Grok 패턴 출력
                if (formatsWithGrokPattern == 1) {
                    System.out.println("포맷 " + format.getFormatId() + "의 Grok 패턴:");
                    System.out.println(format.getGrokPattern());
                }
            }
        }
        
        System.out.println("LogTypes가 있는 포맷 수: " + formatsWithLogTypes);
        System.out.println("Patterns가 있는 포맷 수: " + formatsWithPatterns);
        System.out.println("Grok 패턴을 가진 포맷 수: " + formatsWithGrokPattern);
        
        assertTrue("Grok 패턴을 가진 포맷이 있어야 합니다", formatsWithGrokPattern > 0);
    }
    
    @Test
    public void testNonExistentFile() {
        // 존재하지 않는 파일로 테스트
        FilePatternRepository badRepo = new FilePatternRepository("non-existent-file.sql");
        
        // 초기화 실패해야 함
        boolean initialized = badRepo.initialize();
        assertFalse("존재하지 않는 파일로 초기화되면 안됩니다", initialized);
        assertEquals("포맷이 로드되면 안됩니다", 0, badRepo.size());
    }
}