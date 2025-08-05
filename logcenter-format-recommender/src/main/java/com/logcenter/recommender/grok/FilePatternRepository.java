package com.logcenter.recommender.grok;

import com.logcenter.recommender.config.AppConfig;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.util.JacksonJsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 파일 기반 패턴 저장소 구현체
 * GROK-PATTERN-CONVERTER.sql 파일에서 로그 포맷 데이터를 로드하고 관리
 */
public class FilePatternRepository implements PatternRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(FilePatternRepository.class);
    
    private final Map<String, LogFormat> formatsById;
    private final Map<String, List<LogFormat>> formatsByGroup;
    private final Map<String, List<LogFormat>> formatsByVendor;
    private final String resourcePath;
    private boolean initialized = false;
    
    /**
     * 기본 생성자
     */
    public FilePatternRepository() {
        this(AppConfig.getInstance().getString(AppConfig.LOG_FORMATS_PATH));
    }
    
    /**
     * 리소스 경로를 지정하는 생성자
     * @param resourcePath 리소스 파일 경로
     */
    public FilePatternRepository(String resourcePath) {
        this.resourcePath = resourcePath;
        this.formatsById = new ConcurrentHashMap<>();
        this.formatsByGroup = new ConcurrentHashMap<>();
        this.formatsByVendor = new ConcurrentHashMap<>();
    }
    
    @Override
    public boolean initialize() {
        try {
            int loaded = loadFormats();
            initialized = loaded > 0;
            return initialized;
        } catch (Exception e) {
            logger.error("패턴 저장소 초기화 실패", e);
            return false;
        }
    }
    
    @Override
    public int loadFormats() {
        clear();
        
        try (InputStream inputStream = getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                logger.error("로그 포맷 파일을 찾을 수 없습니다: {}", resourcePath);
                return 0;
            }
            
            List<LogFormat> formats = parseFormatsFromFile(inputStream);
            
            // 인덱싱
            for (LogFormat format : formats) {
                // ID별 인덱싱
                formatsById.put(format.getFormatId(), format);
                
                // 그룹별 인덱싱
                String groupName = format.getGroupName();
                if (groupName != null) {
                    formatsByGroup.computeIfAbsent(groupName, k -> new ArrayList<>())
                            .add(format);
                }
                
                // 벤더별 인덱싱
                String vendor = format.getVendor();
                if (vendor != null) {
                    formatsByVendor.computeIfAbsent(vendor, k -> new ArrayList<>())
                            .add(format);
                }
            }
            
            logger.info("{}개의 로그 포맷을 로드했습니다", formats.size());
            logger.info("그룹: {}개, 벤더: {}개", 
                formatsByGroup.size(), formatsByVendor.size());
            
            return formats.size();
            
        } catch (IOException e) {
            logger.error("로그 포맷 파일 읽기 오류", e);
            return 0;
        }
    }
    
    /**
     * 파일에서 로그 포맷 파싱
     * GROK-PATTERN-CONVERTER.sql 파일은 JSON 배열 형식
     */
    private List<LogFormat> parseFormatsFromFile(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            // 전체 파일을 문자열로 읽기
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            
            // JSON 내용 가져오기
            String jsonContent = content.toString();
            
            // JSON 파싱 (Jackson은 이스케이프 시퀀스를 더 잘 처리함)
            TypeReference<List<LogFormat>> typeRef = new TypeReference<List<LogFormat>>() {};
            List<LogFormat> formats = JacksonJsonUtils.fromJson(jsonContent, typeRef);
            
            if (formats == null) {
                logger.warn("JSON 파싱 결과가 null입니다");
                return new ArrayList<>();
            }
            
            // 첫 번째 Grok 패턴 설정
            for (LogFormat format : formats) {
                // format_id가 이미 JSON에서 설정되어 있음
                
                // 첫 번째 로그 타입의 첫 번째 패턴을 대표 Grok 패턴으로 설정
                if (format.getGrokPattern() == null && format.getLogTypes() != null) {
                    for (LogFormat.LogType logType : format.getLogTypes()) {
                        if (logType.getPatterns() != null && !logType.getPatterns().isEmpty()) {
                            LogFormat.Pattern firstPattern = logType.getPatterns().get(0);
                            logger.debug("패턴 이름: {}, Grok 표현식 null 여부: {}", 
                                firstPattern.getExpName(), 
                                firstPattern.getGrokExp() == null);
                            if (firstPattern.getGrokExp() != null) {
                                // JSON에서 이미 올바르게 이스케이프된 Grok 패턴
                                format.setGrokPattern(firstPattern.getGrokExp());
                                break;
                            }
                        }
                    }
                }
                
                // 디버깅: grokPattern이 설정되었는지 확인
                if (format.getGrokPattern() == null && format.getLogTypes() != null && !format.getLogTypes().isEmpty()) {
                    logger.debug("포맷 {}에 Grok 패턴이 설정되지 않음", format.getFormatId());
                }
            }
            
            return formats;
        }
    }
    
    @Override
    public List<LogFormat> getAllFormats() {
        return new ArrayList<>(formatsById.values());
    }
    
    @Override
    public LogFormat getFormatById(String formatId) {
        return formatsById.get(formatId);
    }
    
    @Override
    public List<LogFormat> getFormatsByGroup(String groupName) {
        List<LogFormat> formats = formatsByGroup.get(groupName);
        return formats != null ? new ArrayList<>(formats) : new ArrayList<>();
    }
    
    @Override
    public List<LogFormat> getFormatsByVendor(String vendor) {
        List<LogFormat> formats = formatsByVendor.get(vendor);
        return formats != null ? new ArrayList<>(formats) : new ArrayList<>();
    }
    
    @Override
    public Map<String, Integer> getGroupStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        formatsByGroup.forEach((group, formats) -> 
            stats.put(group, formats.size()));
        return stats;
    }
    
    @Override
    public Map<String, Integer> getVendorStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        formatsByVendor.forEach((vendor, formats) -> 
            stats.put(vendor, formats.size()));
        return stats;
    }
    
    @Override
    public int reloadFormats() {
        logger.info("로그 포맷 재로드 시작...");
        return loadFormats();
    }
    
    @Override
    public int size() {
        return formatsById.size();
    }
    
    @Override
    public void clear() {
        formatsById.clear();
        formatsByGroup.clear();
        formatsByVendor.clear();
    }
    
    /**
     * 리소스 스트림 가져오기
     */
    private InputStream getResourceAsStream(String resourcePath) {
        // 클래스로더로 시도
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(resourcePath);
        
        // 실패하면 현재 클래스 기준으로 시도
        if (stream == null) {
            stream = getClass().getResourceAsStream("/" + resourcePath);
        }
        
        return stream;
    }
    
    /**
     * 초기화 상태 확인
     * @return 초기화 여부
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 모든 그룹 이름 가져오기
     * @return 그룹 이름 집합
     */
    public Set<String> getAllGroups() {
        return new HashSet<>(formatsByGroup.keySet());
    }
    
    /**
     * 모든 벤더 이름 가져오기
     * @return 벤더 이름 집합
     */
    public Set<String> getAllVendors() {
        return new HashSet<>(formatsByVendor.keySet());
    }
}