package com.logcenter.recommender.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.logcenter.recommender.config.AppConfig;
import com.logcenter.recommender.model.GrokPattern;
import com.logcenter.recommender.model.LogFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 영구 캐시 매니저
 * 컴파일된 Grok 패턴과 로그 포맷을 디스크에 캐시하여 초기화 시간을 단축
 *
 * @since 1.0.0
 */
public class PersistentCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(PersistentCacheManager.class);

    // 캐시 파일명
    private static final String CUSTOM_PATTERNS_CACHE = "custom_patterns.cache";
    private static final String LOG_FORMATS_CACHE = "log_formats.cache";
    private static final String METADATA_FILE = "cache_metadata.json";

    // 기본 설정
    private static final int DEFAULT_TTL_DAYS = 7;
    private static final String DEFAULT_CACHE_DIR = ".logcenter/cache";

    private final Gson gson;
    private Path cacheDirectory;
    private boolean enabled;
    private int ttlDays;
    private boolean checksumEnabled;
    private Map<String, CacheMetadata> metadataMap;

    /**
     * 생성자
     */
    public PersistentCacheManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
        this.metadataMap = new HashMap<>();
    }

    /**
     * 초기화
     */
    public void initialize() {
        AppConfig config = AppConfig.getInstance();

        // 캐시 활성화 여부
        this.enabled = config.getBoolean("cache.persistent.enabled", true);

        if (!enabled) {
            logger.info("영구 캐시가 비활성화되었습니다");
            return;
        }

        // 캐시 디렉토리 설정
        String cacheDir = config.getString(AppConfig.PERSISTENT_CACHE_DIR, DEFAULT_CACHE_DIR);
        String userHome = System.getProperty("user.home");

        if (cacheDir.startsWith("~")) {
            cacheDir = cacheDir.replaceFirst("~", userHome);
        } else if (!cacheDir.startsWith("/")) {
            cacheDir = userHome + File.separator + cacheDir;
        }

        this.cacheDirectory = Paths.get(cacheDir);

        // TTL 및 체크섬 설정
        this.ttlDays = config.getInt("cache.persistent.ttl.days", DEFAULT_TTL_DAYS);
        this.checksumEnabled = config.getBoolean("cache.persistent.checksum.enabled", true);

        // 캐시 디렉토리 생성
        try {
            if (!Files.exists(cacheDirectory)) {
                Files.createDirectories(cacheDirectory);
                logger.info("캐시 디렉토리 생성: {}", cacheDirectory);
            }

            // 메타데이터 로드
            loadMetadata();

            logger.info("영구 캐시 초기화 완료: {}", cacheDirectory);
        } catch (IOException e) {
            logger.error("캐시 디렉토리 생성 실패", e);
            this.enabled = false;
        }
    }

    /**
     * 캐시 활성화 여부
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 캐시 디렉토리 경로
     */
    public String getCacheDir() {
        return cacheDirectory != null ? cacheDirectory.toString() : null;
    }

    /**
     * 커스텀 패턴 로드
     */
    public Map<String, GrokPattern> loadCustomPatterns() {
        if (!enabled) {
            return null;
        }

        Path cacheFile = cacheDirectory.resolve(CUSTOM_PATTERNS_CACHE);

        if (!Files.exists(cacheFile)) {
            logger.debug("커스텀 패턴 캐시 파일이 없습니다");
            return null;
        }

        try {
            // TTL 확인
            if (isCacheExpired(CUSTOM_PATTERNS_CACHE)) {
                logger.info("커스텀 패턴 캐시가 만료되었습니다");
                return null;
            }

            // 캐시 파일 읽기
            String json = new String(Files.readAllBytes(cacheFile), StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, GrokPattern>>(){}.getType();
            Map<String, GrokPattern> patterns = gson.fromJson(json, type);

            logger.info("캐시에서 {} 개의 커스텀 패턴을 로드했습니다", patterns.size());
            return patterns;

        } catch (IOException | RuntimeException e) {
            logger.error("커스텀 패턴 캐시 로드 실패", e);
            return null;
        }
    }

    /**
     * 커스텀 패턴 저장
     */
    public void saveCustomPatterns(Map<String, GrokPattern> patterns) {
        if (!enabled || patterns == null) {
            return;
        }

        try {
            Path cacheFile = cacheDirectory.resolve(CUSTOM_PATTERNS_CACHE);
            String json = gson.toJson(patterns);
            Files.write(cacheFile, json.getBytes(StandardCharsets.UTF_8));

            // 메타데이터 업데이트 (체크섬은 saveResourceChecksum에서 처리)
            updateMetadata(CUSTOM_PATTERNS_CACHE, null);

            logger.info("커스텀 패턴 {} 개를 캐시에 저장했습니다", patterns.size());

        } catch (IOException e) {
            logger.error("커스텀 패턴 캐시 저장 실패", e);
        }
    }

    /**
     * 로그 포맷 로드
     */
    public List<LogFormat> loadLogFormats() {
        if (!enabled) {
            return null;
        }

        Path cacheFile = cacheDirectory.resolve(LOG_FORMATS_CACHE);

        if (!Files.exists(cacheFile)) {
            logger.debug("로그 포맷 캐시 파일이 없습니다");
            return null;
        }

        try {
            // TTL 확인
            if (isCacheExpired(LOG_FORMATS_CACHE)) {
                logger.info("로그 포맷 캐시가 만료되었습니다");
                return null;
            }

            // 캐시 파일 읽기
            String json = new String(Files.readAllBytes(cacheFile), StandardCharsets.UTF_8);
            Type type = new TypeToken<List<LogFormat>>(){}.getType();
            List<LogFormat> formats = gson.fromJson(json, type);

            logger.info("캐시에서 {} 개의 로그 포맷을 로드했습니다", formats.size());
            return formats;

        } catch (IOException | RuntimeException e) {
            logger.error("로그 포맷 캐시 로드 실패", e);
            return null;
        }
    }

    /**
     * 로그 포맷 저장
     */
    public void saveLogFormats(List<LogFormat> formats) {
        if (!enabled || formats == null) {
            return;
        }

        try {
            Path cacheFile = cacheDirectory.resolve(LOG_FORMATS_CACHE);
            String json = gson.toJson(formats);
            Files.write(cacheFile, json.getBytes(StandardCharsets.UTF_8));

            // 메타데이터 업데이트 (체크섬은 saveResourceChecksum에서 처리)
            updateMetadata(LOG_FORMATS_CACHE, null);

            logger.info("로그 포맷 {} 개를 캐시에 저장했습니다", formats.size());

        } catch (IOException e) {
            logger.error("로그 포맷 캐시 저장 실패", e);
        }
    }

    /**
     * 리소스 파일의 체크섬을 계산하여 저장
     * @param resourcePath 리소스 파일 경로
     */
    public void saveResourceChecksum(String resourcePath) {
        if (!enabled || !checksumEnabled) {
            return;
        }

        try {
            // 체크섬 계산
            String checksum = calculateResourceChecksum(resourcePath);

            // 캐시 파일 이름 결정
            String cacheFileName;
            if (resourcePath.contains("custom-grok-patterns")) {
                cacheFileName = CUSTOM_PATTERNS_CACHE;
            } else if (resourcePath.contains("setting_logformat") ||
                       resourcePath.contains("GROK-PATTERN-CONVERTER")) {
                cacheFileName = LOG_FORMATS_CACHE;
            } else {
                logger.warn("알 수 없는 리소스 파일: {}", resourcePath);
                return;
            }

            // 메타데이터 업데이트
            CacheMetadata metadata = metadataMap.get(cacheFileName);
            if (metadata == null) {
                metadata = new CacheMetadata();
                metadata.setCreatedAt(LocalDateTime.now());
            }
            metadata.setChecksum(checksum);
            metadataMap.put(cacheFileName, metadata);

            // 메타데이터 파일 저장
            saveMetadata();

            logger.debug("리소스 체크섬 저장: {} -> {}", resourcePath, checksum.substring(0, 8) + "...");

        } catch (Exception e) {
            logger.error("체크섬 저장 실패: " + resourcePath, e);
        }
    }

    /**
     * 리소스 파일의 캐시 유효성 검사
     */
    public boolean isResourceCacheValid(String resourcePath) {
        if (!enabled || !checksumEnabled) {
            return true;
        }

        try {
            // 리소스 파일의 체크섬 계산
            String currentChecksum = calculateResourceChecksum(resourcePath);

            // 캐시 파일 이름 결정
            String cacheFileName;
            if (resourcePath.contains("custom-grok-patterns")) {
                cacheFileName = CUSTOM_PATTERNS_CACHE;
            } else if (resourcePath.contains("setting_logformat") ||
                       resourcePath.contains("GROK-PATTERN-CONVERTER")) {
                cacheFileName = LOG_FORMATS_CACHE;
            } else {
                return false;
            }

            // 메타데이터에서 체크섬 비교
            CacheMetadata metadata = metadataMap.get(cacheFileName);
            if (metadata == null) {
                return false;
            }

            boolean valid = currentChecksum.equals(metadata.getChecksum());
            if (!valid) {
                logger.info("리소스 파일이 변경되었습니다: {}", resourcePath);
            }

            return valid;

        } catch (Exception e) {
            logger.error("체크섬 검증 실패", e);
            return false;
        }
    }

    /**
     * 캐시 무효화 (모든 캐시 삭제)
     */
    public void invalidate() {
        if (!enabled) {
            return;
        }

        try {
            // 캐시 파일 삭제
            Files.deleteIfExists(cacheDirectory.resolve(CUSTOM_PATTERNS_CACHE));
            Files.deleteIfExists(cacheDirectory.resolve(LOG_FORMATS_CACHE));
            Files.deleteIfExists(cacheDirectory.resolve(METADATA_FILE));

            // 메타데이터 초기화
            metadataMap.clear();

            logger.info("모든 캐시가 삭제되었습니다");

        } catch (IOException e) {
            logger.error("캐시 삭제 실패", e);
        }
    }

    /**
     * 캐시 재구축
     */
    public void rebuild() {
        logger.info("캐시를 재구축합니다...");
        invalidate();
        // 실제 재구축은 다음 로드 시 자동으로 수행됨
    }

    /**
     * 캐시 만료 여부 확인
     */
    private boolean isCacheExpired(String cacheFileName) {
        CacheMetadata metadata = metadataMap.get(cacheFileName);

        if (metadata == null) {
            return true;
        }

        LocalDateTime expiry = metadata.getCreatedAt().plus(ttlDays, ChronoUnit.DAYS);
        boolean expired = LocalDateTime.now().isAfter(expiry);

        if (expired) {
            logger.debug("캐시 만료: {} (생성일: {})", cacheFileName, metadata.getCreatedAt());
        }

        return expired;
    }

    /**
     * 메타데이터 로드
     */
    private void loadMetadata() {
        Path metadataFile = cacheDirectory.resolve(METADATA_FILE);

        if (!Files.exists(metadataFile)) {
            return;
        }

        try {
            String json = new String(Files.readAllBytes(metadataFile), StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, CacheMetadata>>(){}.getType();
            metadataMap = gson.fromJson(json, type);

            if (metadataMap == null) {
                metadataMap = new HashMap<>();
            }

            logger.debug("캐시 메타데이터 로드 완료");

        } catch (IOException | RuntimeException e) {
            logger.error("메타데이터 로드 실패", e);
            metadataMap = new HashMap<>();
        }
    }

    /**
     * 메타데이터 업데이트
     */
    private void updateMetadata(String cacheFileName, String checksum) {
        CacheMetadata metadata = new CacheMetadata();
        metadata.setCreatedAt(LocalDateTime.now());
        metadata.setChecksum(checksum);

        metadataMap.put(cacheFileName, metadata);

        // 메타데이터 파일 저장
        saveMetadata();
    }

    /**
     * 메타데이터 파일 저장
     */
    private void saveMetadata() {
        try {
            Path metadataFile = cacheDirectory.resolve(METADATA_FILE);
            String json = gson.toJson(metadataMap);
            Files.write(metadataFile, json.getBytes(StandardCharsets.UTF_8));

        } catch (IOException e) {
            logger.error("메타데이터 저장 실패", e);
        }
    }

    /**
     * 리소스 파일의 체크섬 계산
     */
    private String calculateResourceChecksum(String resourcePath) throws IOException, NoSuchAlgorithmException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if (is == null) {
            throw new IOException("리소스 파일을 찾을 수 없습니다: " + resourcePath);
        }

        try (BufferedInputStream bis = new BufferedInputStream(is)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;

            while ((read = bis.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }

            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        }
    }

    /**
     * 캐시 메타데이터 내부 클래스
     */
    private static class CacheMetadata {
        private LocalDateTime createdAt;
        private String checksum;

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public String getChecksum() {
            return checksum;
        }

        public void setChecksum(String checksum) {
            this.checksum = checksum;
        }
    }
}
