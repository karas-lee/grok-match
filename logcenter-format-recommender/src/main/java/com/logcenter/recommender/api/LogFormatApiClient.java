package com.logcenter.recommender.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.logcenter.recommender.api.cache.CacheManager;
import com.logcenter.recommender.api.model.ApiResponse;
import com.logcenter.recommender.api.model.LogFormatRequest;
import com.logcenter.recommender.model.FormatRecommendation;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.util.JacksonJsonUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * LogCenter API 클라이언트
 */
public class LogFormatApiClient implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(LogFormatApiClient.class);
    
    private final String baseUrl;
    private final String apiKey;
    private final CloseableHttpClient httpClient;
    private final CacheManager cacheManager;
    
    // 타임아웃 설정 (밀리초)
    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int SOCKET_TIMEOUT = 30000;
    private static final int REQUEST_TIMEOUT = 30000;
    
    // 재시도 설정
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    
    /**
     * 생성자
     * @param baseUrl API 기본 URL
     * @param apiKey API 인증 키
     */
    public LogFormatApiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.cacheManager = CacheManager.getInstance();
        
        // HTTP 클라이언트 설정
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(20);
        
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectionRequestTimeout(REQUEST_TIMEOUT)
                .build();
        
        this.httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();
        
        logger.info("API 클라이언트 초기화: {}", baseUrl);
    }
    
    /**
     * 로그 포맷 목록 조회
     */
    public List<LogFormat> getLogFormats() throws IOException {
        String cacheKey = "all_formats";
        
        // 캐시 확인
        List<LogFormat> cached = cacheManager.get("logFormats", cacheKey);
        if (cached != null) {
            logger.debug("캐시에서 로그 포맷 목록 반환");
            return cached;
        }
        
        // API 호출
        String url = baseUrl + "/api/v1/logformats";
        ApiResponse<List<LogFormat>> response = executeGetRequest(url, 
                new TypeReference<ApiResponse<List<LogFormat>>>() {});
        
        if (response.isSuccess() && response.getData() != null) {
            // 캐시 저장
            cacheManager.put("logFormats", cacheKey, response.getData());
            return response.getData();
        }
        
        throw new IOException("로그 포맷 목록 조회 실패: " + response.getMessage());
    }
    
    /**
     * 특정 그룹의 로그 포맷 조회
     */
    public List<LogFormat> getLogFormatsByGroup(String group) throws IOException {
        String cacheKey = "formats_group_" + group;
        
        // 캐시 확인
        List<LogFormat> cached = cacheManager.get("logFormats", cacheKey);
        if (cached != null) {
            logger.debug("캐시에서 그룹별 로그 포맷 반환: {}", group);
            return cached;
        }
        
        // API 호출
        String url = baseUrl + "/api/v1/logformats?group=" + group;
        ApiResponse<List<LogFormat>> response = executeGetRequest(url,
                new TypeReference<ApiResponse<List<LogFormat>>>() {});
        
        if (response.isSuccess() && response.getData() != null) {
            // 캐시 저장
            cacheManager.put("logFormats", cacheKey, response.getData());
            return response.getData();
        }
        
        throw new IOException("그룹별 로그 포맷 조회 실패: " + response.getMessage());
    }
    
    /**
     * 로그 포맷 추천 요청
     */
    public List<FormatRecommendation> recommendFormats(LogFormatRequest request) throws IOException {
        // 캐시 키 생성
        String cacheKey = generateCacheKey(request);
        
        // 캐시 확인
        List<FormatRecommendation> cached = cacheManager.get("recommendations", cacheKey);
        if (cached != null) {
            logger.debug("캐시에서 추천 결과 반환");
            return cached;
        }
        
        // API 호출
        String url = baseUrl + "/api/v1/recommend";
        ApiResponse<List<FormatRecommendation>> response = executePostRequest(url, request,
                new TypeReference<ApiResponse<List<FormatRecommendation>>>() {});
        
        if (response.isSuccess() && response.getData() != null) {
            // 캐시 저장
            cacheManager.put("recommendations", cacheKey, response.getData());
            return response.getData();
        }
        
        throw new IOException("로그 포맷 추천 실패: " + response.getMessage());
    }
    
    /**
     * GET 요청 실행
     */
    private <T> T executeGetRequest(String url, TypeReference<T> typeRef) throws IOException {
        HttpGet request = new HttpGet(url);
        addHeaders(request);
        
        return executeRequestWithRetry(request, typeRef);
    }
    
    /**
     * POST 요청 실행
     */
    private <T> T executePostRequest(String url, Object body, TypeReference<T> typeRef) throws IOException {
        HttpPost request = new HttpPost(url);
        addHeaders(request);
        
        // 요청 본문 설정
        String jsonBody = JacksonJsonUtils.toJson(body);
        StringEntity entity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);
        request.setEntity(entity);
        
        return executeRequestWithRetry(request, typeRef);
    }
    
    /**
     * 재시도 로직을 포함한 요청 실행
     */
    private <T> T executeRequestWithRetry(HttpUriRequest request, TypeReference<T> typeRef) throws IOException {
        IOException lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return executeRequest(request, typeRef);
            } catch (IOException e) {
                lastException = e;
                logger.warn("API 요청 실패 (시도 {}/{}): {}", attempt, MAX_RETRIES, e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("재시도 중 인터럽트 발생", ie);
                    }
                }
            }
        }
        
        throw new IOException("API 요청 실패 (최대 재시도 횟수 초과)", lastException);
    }
    
    /**
     * 실제 HTTP 요청 실행
     */
    private <T> T executeRequest(HttpUriRequest request, TypeReference<T> typeRef) throws IOException {
        logger.debug("API 요청: {} {}", request.getMethod(), request.getURI());
        
        HttpResponse response = httpClient.execute(request);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            
            if (entity == null) {
                throw new IOException("응답 본문이 비어있습니다");
            }
            
            String responseBody = EntityUtils.toString(entity);
            logger.debug("API 응답 ({}): {}", statusCode, responseBody);
            
            if (statusCode >= 200 && statusCode < 300) {
                // 성공 응답
                return JacksonJsonUtils.fromJson(responseBody, typeRef);
            } else if (statusCode == 401) {
                throw new IOException("인증 실패: API 키를 확인하세요");
            } else if (statusCode == 429) {
                throw new IOException("요청 제한 초과: 잠시 후 다시 시도하세요");
            } else {
                // 오류 응답 파싱 시도
                try {
                    ApiResponse<?> errorResponse = JacksonJsonUtils.fromJson(responseBody, 
                            new TypeReference<ApiResponse<?>>() {});
                    throw new IOException("API 오류 (" + statusCode + "): " + errorResponse.getMessage());
                } catch (Exception e) {
                    throw new IOException("API 오류 (" + statusCode + "): " + responseBody);
                }
            }
        } finally {
            if (response instanceof CloseableHttpResponse) {
                ((CloseableHttpResponse) response).close();
            }
        }
    }
    
    /**
     * 요청 헤더 추가
     */
    private void addHeaders(HttpUriRequest request) {
        request.addHeader("Authorization", "Bearer " + apiKey);
        request.addHeader("Accept", "application/json");
        request.addHeader("User-Agent", "LogCenter-Format-Recommender/1.0");
    }
    
    /**
     * 캐시 키 생성
     */
    private String generateCacheKey(LogFormatRequest request) {
        StringBuilder key = new StringBuilder();
        
        if (request.getLogSamples() != null && !request.getLogSamples().isEmpty()) {
            // 첫 번째 로그 샘플의 해시값 사용
            key.append(request.getLogSamples().get(0).hashCode());
        }
        
        if (request.getGroupFilter() != null) {
            key.append("_g:").append(request.getGroupFilter());
        }
        
        if (request.getVendorFilter() != null) {
            key.append("_v:").append(request.getVendorFilter());
        }
        
        key.append("_t:").append(request.getTopN());
        
        return key.toString();
    }
    
    /**
     * 연결 상태 확인
     */
    public boolean isHealthy() {
        try {
            String url = baseUrl + "/api/v1/health";
            HttpGet request = new HttpGet(url);
            addHeaders(request);
            
            HttpResponse response = httpClient.execute(request);
            try {
                return response.getStatusLine().getStatusCode() == 200;
            } finally {
                if (response instanceof CloseableHttpResponse) {
                    ((CloseableHttpResponse) response).close();
                }
            }
        } catch (Exception e) {
            logger.error("헬스 체크 실패", e);
            return false;
        }
    }
    
    /**
     * 캐시 초기화
     */
    public void clearCache() {
        cacheManager.clearCache("logFormats");
        cacheManager.clearCache("recommendations");
        logger.info("API 클라이언트 캐시 초기화");
    }
    
    /**
     * 리소스 정리
     */
    @Override
    public void close() throws Exception {
        if (httpClient != null) {
            httpClient.close();
            logger.info("API 클라이언트 종료");
        }
    }
}