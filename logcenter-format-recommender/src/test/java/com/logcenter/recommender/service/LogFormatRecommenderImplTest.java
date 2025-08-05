package com.logcenter.recommender.service;

import com.logcenter.recommender.grok.FilePatternRepository;
import com.logcenter.recommender.grok.GrokCompilerWrapper;
import com.logcenter.recommender.grok.PatternRepository;
import com.logcenter.recommender.model.FormatRecommendation;
import com.logcenter.recommender.model.LogFormat;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

/**
 * LogFormatRecommenderImpl 단위 테스트
 */
public class LogFormatRecommenderImplTest {
    
    private LogFormatRecommenderImpl recommender;
    private PatternRepository patternRepository;
    private GrokCompilerWrapper grokCompiler;
    
    @Before
    public void setUp() {
        // 테스트용 패턴 저장소 생성
        patternRepository = new FilePatternRepository("setting_logformat.json");
        
        // Grok 컴파일러 초기화
        grokCompiler = new GrokCompilerWrapper();
        
        // 추천 서비스 생성
        recommender = new LogFormatRecommenderImpl(patternRepository, grokCompiler);
        
        // 초기화
        boolean initialized = recommender.initialize();
        assertTrue("추천 서비스 초기화 실패", initialized);
    }
    
    @After
    public void tearDown() {
        recommender.shutdown();
    }
    
    @Test
    public void testInitialize() {
        // 초기화는 setUp에서 이미 수행됨
        List<LogFormat> formats = recommender.getAvailableFormats();
        assertNotNull(formats);
        assertFalse("포맷 목록이 비어있습니다", formats.isEmpty());
        
        System.out.println("사용 가능한 포맷 수: " + formats.size());
    }
    
    @Test
    public void testRecommendWithApacheLog() {
        // Apache 로그 샘플
        String apacheLog = "192.168.1.100 - - [01/Jan/2024:12:00:00 +0000] \"GET /index.html HTTP/1.1\" 200 1234";
        
        // 추천 수행
        List<FormatRecommendation> recommendations = recommender.recommend(apacheLog);
        
        // 검증
        assertNotNull(recommendations);
        assertFalse("추천 결과가 없습니다", recommendations.isEmpty());
        
        // 첫 번째 추천 확인
        FormatRecommendation top = recommendations.get(0);
        System.out.println("최상위 추천: " + top.getLogFormat().getFormatName() + 
                         " (신뢰도: " + top.getConfidence() + "%)");
        
        assertTrue("신뢰도가 너무 낮습니다", top.getConfidence() > 0);
    }
    
    @Test
    public void testRecommendWithFirewallLog() {
        // 방화벽 로그 샘플 (가상)
        String firewallLog = "2024-01-01 12:00:00 ALLOW TCP 192.168.1.100:12345 -> 10.0.0.1:443";
        
        // 추천 수행
        List<FormatRecommendation> recommendations = recommender.recommend(firewallLog);
        
        // 검증
        assertNotNull(recommendations);
        
        if (!recommendations.isEmpty()) {
            FormatRecommendation top = recommendations.get(0);
            System.out.println("방화벽 로그 추천: " + top.getLogFormat().getFormatName() + 
                             " (그룹: " + top.getGroupName() + ")");
        }
    }
    
    @Test
    public void testRecommendBatch() {
        // 여러 로그 샘플
        List<String> logSamples = Arrays.asList(
            "192.168.1.1 - - [01/Jan/2024:12:00:00 +0000] \"GET /page1 HTTP/1.1\" 200 1000",
            "192.168.1.2 - - [01/Jan/2024:12:00:01 +0000] \"POST /page2 HTTP/1.1\" 201 2000",
            "192.168.1.3 - - [01/Jan/2024:12:00:02 +0000] \"GET /page3 HTTP/1.1\" 404 500"
        );
        
        // 배치 추천
        List<FormatRecommendation> recommendations = recommender.recommendBatch(logSamples);
        
        // 검증
        assertNotNull(recommendations);
        
        // 배치 처리 시 매치 카운트가 증가해야 함
        if (!recommendations.isEmpty()) {
            FormatRecommendation top = recommendations.get(0);
            assertTrue("매치 카운트가 1보다 커야 합니다", top.getMatchCount() >= 1);
        }
    }
    
    @Test
    public void testRecommendInGroup() {
        // 특정 그룹에서만 추천
        String log = "test log sample";
        
        // 사용 가능한 그룹 확인
        List<LogFormat> allFormats = recommender.getAvailableFormats();
        String testGroup = null;
        for (LogFormat format : allFormats) {
            if (format.getGroupName() != null) {
                testGroup = format.getGroupName();
                break;
            }
        }
        
        if (testGroup != null) {
            List<FormatRecommendation> recommendations = recommender.recommendInGroup(log, testGroup);
            
            // 검증
            assertNotNull(recommendations);
            
            // 모든 추천이 해당 그룹이어야 함
            for (FormatRecommendation rec : recommendations) {
                assertEquals(testGroup, rec.getGroupName());
            }
        }
    }
    
    @Test
    public void testRecommendOptions() {
        // 옵션 설정
        LogFormatRecommender.RecommendOptions options = 
            new LogFormatRecommender.RecommendOptions.Builder()
                .maxResults(5)
                .minConfidence(50.0)
                .includePartialMatches(false)
                .build();
        
        recommender.setOptions(options);
        
        // 테스트 로그
        String log = "test log with limited options";
        List<FormatRecommendation> recommendations = recommender.recommend(log);
        
        // 검증
        assertNotNull(recommendations);
        assertTrue("결과가 최대 5개여야 합니다", recommendations.size() <= 5);
        
        // 모든 결과가 최소 신뢰도 이상이어야 함
        for (FormatRecommendation rec : recommendations) {
            assertTrue("신뢰도가 50% 이상이어야 합니다", rec.getConfidence() >= 50.0);
            
            // 부분 매칭 제외 옵션 확인
            if (!rec.isCompleteMatch()) {
                assertFalse("부분 매칭이 포함되면 안됩니다", rec.isPartialMatch());
            }
        }
    }
    
    @Test
    public void testCaching() {
        // 캐싱 활성화
        LogFormatRecommender.RecommendOptions options = 
            new LogFormatRecommender.RecommendOptions.Builder()
                .enableCaching(true)
                .cacheSize(100)
                .build();
        
        recommender.setOptions(options);
        
        String log = "cached log sample";
        
        // 첫 번째 호출 - 캐시 미스
        long start1 = System.currentTimeMillis();
        List<FormatRecommendation> result1 = recommender.recommend(log);
        long time1 = System.currentTimeMillis() - start1;
        
        // 두 번째 호출 - 캐시 히트
        long start2 = System.currentTimeMillis();
        List<FormatRecommendation> result2 = recommender.recommend(log);
        long time2 = System.currentTimeMillis() - start2;
        
        // 검증
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals("캐시된 결과가 동일해야 합니다", result1.size(), result2.size());
        
        // 캐시 히트가 더 빨라야 함 (항상 그런 것은 아니므로 로그만 출력)
        System.out.println("첫 번째 호출: " + time1 + "ms, 두 번째 호출: " + time2 + "ms");
    }
    
    @Test
    public void testGetFormatsByGroup() {
        // 그룹별 포맷 조회
        List<LogFormat> webFormats = recommender.getFormatsByGroup("Web Server");
        
        if (webFormats != null && !webFormats.isEmpty()) {
            System.out.println("Web Server 그룹 포맷 수: " + webFormats.size());
            
            // 모든 포맷이 Web Server 그룹이어야 함
            for (LogFormat format : webFormats) {
                assertEquals("Web Server", format.getGroupName());
            }
        }
    }
    
    @Test
    public void testReloadFormats() {
        // 포맷 재로드
        int reloaded = recommender.reloadFormats();
        
        // 검증
        assertTrue("재로드된 포맷이 있어야 합니다", reloaded > 0);
        System.out.println("재로드된 포맷 수: " + reloaded);
        
        // 재로드 후에도 추천이 작동해야 함
        List<FormatRecommendation> recommendations = recommender.recommend("test after reload");
        assertNotNull(recommendations);
    }
    
    @Test
    public void testNullInputs() {
        // null 입력 테스트
        List<FormatRecommendation> result1 = recommender.recommend(null);
        assertNotNull(result1);
        assertTrue(result1.isEmpty());
        
        List<FormatRecommendation> result2 = recommender.recommend("");
        assertNotNull(result2);
        assertTrue(result2.isEmpty());
        
        List<FormatRecommendation> result3 = recommender.recommendBatch(null);
        assertNotNull(result3);
        assertTrue(result3.isEmpty());
        
        List<FormatRecommendation> result4 = recommender.recommendInGroup(null, "test");
        assertNotNull(result4);
        assertTrue(result4.isEmpty());
    }
}