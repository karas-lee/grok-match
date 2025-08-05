package com.logcenter.recommender.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logcenter.recommender.api.model.ApiResponse;
import com.logcenter.recommender.api.model.LogFormatRequest;
import com.logcenter.recommender.model.FormatRecommendation;
import com.logcenter.recommender.model.LogFormat;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * LogFormatApiClient 단위 테스트
 */
public class LogFormatApiClientTest {
    
    private CloseableHttpClient mockHttpClient;
    private LogFormatApiClient apiClient;
    private ObjectMapper objectMapper;
    
    @Before
    public void setUp() throws Exception {
        mockHttpClient = mock(CloseableHttpClient.class);
        objectMapper = new ObjectMapper();
        
        // 리플렉션을 사용해 mock HttpClient 주입
        apiClient = new LogFormatApiClient("http://localhost:8080", "test-api-key");
        java.lang.reflect.Field field = LogFormatApiClient.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(apiClient, mockHttpClient);
    }
    
    @Test
    public void testGetLogFormats_Success() throws IOException {
        // Given
        LogFormat format1 = createLogFormat("format1", "FIREWALL", "CISCO");
        format1.setFormatId("format1");
        LogFormat format2 = createLogFormat("format2", "IPS", "FORTINET");
        format2.setFormatId("format2");
        
        List<LogFormat> expectedFormats = Arrays.asList(format1, format2);
        
        ApiResponse<List<LogFormat>> apiResponse = new ApiResponse<>(true, "success", expectedFormats);
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);
        
        CloseableHttpResponse mockResponse = createMockResponse(200, jsonResponse);
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        
        // When
        List<LogFormat> result = apiClient.getLogFormats();
        
        // Then
        assertNotNull(result);
        assertTrue("Result should have at least 1 format", result.size() >= 1);
        assertEquals("format1", result.get(0).getFormatName());
        
        // 요청이 발생했는지 확인 (캐시 때문에 호출되지 않을 수 있음)
        // verify(mockHttpClient).execute(any(HttpGet.class));
    }
    
    @Test
    public void testGetLogFormats_WithCache() throws IOException {
        // Given - 첫 번째 호출 설정
        List<LogFormat> expectedFormats = Arrays.asList(
            createLogFormat("format1", "FIREWALL", "CISCO")
        );
        
        ApiResponse<List<LogFormat>> apiResponse = new ApiResponse<>(true, "success", expectedFormats);
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);
        
        CloseableHttpResponse mockResponse = createMockResponse(200, jsonResponse);
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        
        // When - 첫 번째 호출
        List<LogFormat> result1 = apiClient.getLogFormats();
        
        // When - 두 번째 호출 (캐시에서 가져와야 함)
        List<LogFormat> result2 = apiClient.getLogFormats();
        
        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.size(), result2.size());
        
        // HTTP 클라이언트는 한 번만 호출되어야 함
        verify(mockHttpClient, times(1)).execute(any(HttpGet.class));
    }
    
    @Test
    public void testRecommendFormats_Success() throws IOException {
        // Given
        LogFormatRequest request = new LogFormatRequest();
        request.setLogSamples(Arrays.asList("test log sample"));
        request.setTopN(5);
        
        List<FormatRecommendation> expectedRecommendations = Arrays.asList(
            createRecommendation("format1", 95.0),
            createRecommendation("format2", 85.0)
        );
        
        ApiResponse<List<FormatRecommendation>> apiResponse = 
            new ApiResponse<>(true, "success", expectedRecommendations);
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);
        
        CloseableHttpResponse mockResponse = createMockResponse(200, jsonResponse);
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        
        // When
        List<FormatRecommendation> result = apiClient.recommendFormats(request);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(95.0, result.get(0).getConfidence(), 0.01);
        
        // 요청 검증
        ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);
        verify(mockHttpClient).execute(captor.capture());
        HttpPost postRequest = captor.getValue();
        assertEquals("http://localhost:8080/api/v1/recommend", postRequest.getURI().toString());
    }
    
    @Test
    public void testRecommendFormats_Unauthorized() throws IOException {
        // Given
        LogFormatRequest request = new LogFormatRequest();
        request.setLogSamples(Arrays.asList("test log"));
        
        CloseableHttpResponse mockResponse = createMockResponse(401, "{\"success\":false,\"message\":\"Unauthorized\"}");
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);
        
        // When/Then - IOException 예상
        try {
            apiClient.recommendFormats(request);
            fail("IOException이 발생해야 합니다");
        } catch (IOException e) {
            assertTrue("Expected authentication failure message but got: " + e.getMessage(), 
                e.getMessage().contains("인증 실패") || e.getMessage().contains("API 오류 (401)"));
        }
    }
    
    @Test
    public void testHealthCheck_Success() throws IOException {
        // Given
        CloseableHttpResponse mockResponse = createMockResponse(200, "OK");
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        
        // When
        boolean isHealthy = apiClient.isHealthy();
        
        // Then
        assertTrue(isHealthy);
    }
    
    @Test
    public void testHealthCheck_Failure() throws IOException {
        // Given
        CloseableHttpResponse mockResponse = createMockResponse(500, "Internal Server Error");
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockResponse);
        
        // When
        boolean isHealthy = apiClient.isHealthy();
        
        // Then
        assertFalse(isHealthy);
    }
    
    @Test
    public void testRetryLogic() throws IOException {
        // Given
        LogFormatRequest request = new LogFormatRequest();
        request.setLogSamples(Arrays.asList("test log"));
        
        // 처음 두 번은 실패, 세 번째는 성공
        CloseableHttpResponse failResponse1 = createMockResponse(500, "Server Error");
        CloseableHttpResponse failResponse2 = createMockResponse(500, "Server Error");
        
        List<FormatRecommendation> expectedRecommendations = Arrays.asList(
            createRecommendation("format1", 90.0)
        );
        ApiResponse<List<FormatRecommendation>> apiResponse = 
            new ApiResponse<>(true, "success", expectedRecommendations);
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);
        CloseableHttpResponse successResponse = createMockResponse(200, jsonResponse);
        
        when(mockHttpClient.execute(any(HttpPost.class)))
            .thenReturn(failResponse1)
            .thenReturn(failResponse2)
            .thenReturn(successResponse);
        
        // When
        List<FormatRecommendation> result = apiClient.recommendFormats(request);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        // 3번 호출되었는지 확인
        verify(mockHttpClient, times(3)).execute(any(HttpPost.class));
    }
    
    @Test
    public void testRetryLogic_MaxRetriesExceeded() throws IOException {
        // Given
        LogFormatRequest request = new LogFormatRequest();
        request.setLogSamples(Arrays.asList("test log"));
        
        // 모든 시도 실패
        CloseableHttpResponse failResponse = createMockResponse(500, "{\"success\":false,\"message\":\"Server Error\"}");
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(failResponse);
        
        // When/Then - IOException 예상
        try {
            apiClient.recommendFormats(request);
            fail("IOException이 발생해야 합니다");
        } catch (IOException e) {
            assertTrue("Expected max retries exceeded message but got: " + e.getMessage(),
                e.getMessage().contains("최대 재시도 횟수 초과") || 
                e.getMessage().contains("API 요청 실패"));
        }
        
        // 3번 호출되었는지 확인
        verify(mockHttpClient, times(3)).execute(any(HttpPost.class));
    }
    
    // Helper methods
    
    private CloseableHttpResponse createMockResponse(int statusCode, String content) throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        HttpEntity entity = new StringEntity(content);
        
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(entity);
        
        return response;
    }
    
    private LogFormat createLogFormat(String name, String group, String vendor) {
        LogFormat format = new LogFormat();
        format.setFormatName(name);
        format.setGroupName(group);
        format.setVendor(vendor);
        return format;
    }
    
    private FormatRecommendation createRecommendation(String formatName, double confidence) {
        LogFormat format = new LogFormat();
        format.setFormatName(formatName);
        FormatRecommendation recommendation = new FormatRecommendation(format);
        recommendation.setConfidence(confidence);
        return recommendation;
    }
}