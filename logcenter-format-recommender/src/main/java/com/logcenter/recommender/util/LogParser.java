package com.logcenter.recommender.util;

import com.logcenter.recommender.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 로그 파싱 유틸리티 클래스
 * 로그 정규화, 인코딩 처리, 다중 라인 처리 등
 */
public class LogParser {
    
    private static final Logger logger = LoggerFactory.getLogger(LogParser.class);
    
    // 일반적인 로그 타임스탬프 패턴
    private static final List<Pattern> TIMESTAMP_PATTERNS = Arrays.asList(
        Pattern.compile("^\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}"),  // ISO 8601
        Pattern.compile("^\\d{14}\\s+"),                                      // YYYYMMDDHHmmss
        Pattern.compile("^\\w{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2}"),     // Syslog format
        Pattern.compile("^\\[\\d{4}\\.\\d{2}\\.\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\]"), // [YYYY.MM.DD HH:mm:ss]
        Pattern.compile("\\d{1,2}/\\w{3}/\\d{4}:\\d{2}:\\d{2}:\\d{2}")     // Apache format
    );
    
    // 멀티라인 로그 시작 패턴
    private static final List<Pattern> MULTILINE_START_PATTERNS = Arrays.asList(
        Pattern.compile("^\\s+at\\s+"),           // Java stack trace
        Pattern.compile("^\\s*Caused by:"),       // Java exception
        Pattern.compile("^\\s*\\.{3}\\s*\\d+\\s+more"), // Java truncated stack
        Pattern.compile("^\\s*#\\d+\\s+")         // Python traceback
    );
    
    /**
     * 로그 파일 읽기
     */
    public static List<String> readLogFile(String filePath) throws IOException {
        return readLogFile(filePath, detectEncoding(filePath));
    }
    
    /**
     * 로그 파일 읽기 (인코딩 지정)
     */
    public static List<String> readLogFile(String filePath, Charset charset) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("파일이 존재하지 않습니다: " + filePath);
        }
        
        long fileSize = Files.size(path);
        long maxSize = AppConfig.getInstance().getInt(AppConfig.MAX_LOG_SIZE);
        
        if (fileSize > maxSize) {
            logger.warn("파일 크기가 제한을 초과합니다: {} bytes (최대: {} bytes)", fileSize, maxSize);
            // 파일의 처음 부분만 읽기
            return readFirstNBytes(path, maxSize, charset);
        }
        
        return Files.readAllLines(path, charset);
    }
    
    /**
     * 파일의 처음 N 바이트만 읽기
     */
    private static List<String> readFirstNBytes(Path path, long maxBytes, Charset charset) 
            throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, charset)) {
            String line;
            long bytesRead = 0;
            while ((line = reader.readLine()) != null && bytesRead < maxBytes) {
                lines.add(line);
                bytesRead += line.getBytes(charset).length + 1; // +1 for newline
            }
        }
        return lines;
    }
    
    /**
     * 로그 문자열 정규화
     */
    public static String normalizeLog(String log) {
        if (log == null) {
            return "";
        }
        
        // 앞뒤 공백 제거
        log = log.trim();
        
        // 제어 문자 제거 (탭 제외)
        log = log.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", " ");
        
        // 연속된 공백을 하나로
        log = log.replaceAll("\\s+", " ");
        
        return log;
    }
    
    /**
     * 멀티라인 로그 병합
     */
    public static List<String> mergeMultilineLog(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> mergedLogs = new ArrayList<>();
        StringBuilder currentLog = new StringBuilder();
        
        for (String line : lines) {
            if (isNewLogEntry(line) && currentLog.length() > 0) {
                // 새로운 로그 엔트리 시작
                mergedLogs.add(currentLog.toString());
                currentLog = new StringBuilder(line);
            } else if (currentLog.length() > 0) {
                // 현재 로그에 추가 (멀티라인)
                currentLog.append("\n").append(line);
            } else {
                // 첫 번째 라인
                currentLog.append(line);
            }
        }
        
        // 마지막 로그 추가
        if (currentLog.length() > 0) {
            mergedLogs.add(currentLog.toString());
        }
        
        return mergedLogs;
    }
    
    /**
     * 새로운 로그 엔트리 시작 여부 판단
     */
    private static boolean isNewLogEntry(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        // 타임스탬프로 시작하는지 확인
        for (Pattern pattern : TIMESTAMP_PATTERNS) {
            if (pattern.matcher(line).find()) {
                return true;
            }
        }
        
        // 멀티라인 계속 패턴인지 확인
        for (Pattern pattern : MULTILINE_START_PATTERNS) {
            if (pattern.matcher(line).find()) {
                return false;
            }
        }
        
        // 기본적으로 새로운 엔트리로 간주
        return true;
    }
    
    /**
     * 인코딩 자동 감지
     */
    public static Charset detectEncoding(String filePath) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            return detectEncoding(bytes);
        } catch (IOException e) {
            logger.warn("인코딩 감지 실패, UTF-8 사용: {}", e.getMessage());
            return StandardCharsets.UTF_8;
        }
    }
    
    /**
     * 바이트 배열에서 인코딩 감지
     */
    private static Charset detectEncoding(byte[] bytes) {
        // BOM 확인
        if (bytes.length >= 3) {
            if (bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
                return StandardCharsets.UTF_8;
            }
        }
        if (bytes.length >= 2) {
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
                return StandardCharsets.UTF_16LE;
            }
            if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
                return StandardCharsets.UTF_16BE;
            }
        }
        
        // UTF-8 유효성 검사
        try {
            new String(bytes, StandardCharsets.UTF_8);
            return StandardCharsets.UTF_8;
        } catch (Exception e) {
            // UTF-8이 아닌 경우
        }
        
        // 기본값
        String defaultEncoding = AppConfig.getInstance().getString(AppConfig.DEFAULT_ENCODING);
        try {
            return Charset.forName(defaultEncoding);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }
    
    /**
     * 로그에서 타임스탬프 추출
     */
    public static String extractTimestamp(String log) {
        if (log == null) {
            return null;
        }
        
        for (Pattern pattern : TIMESTAMP_PATTERNS) {
            java.util.regex.Matcher matcher = pattern.matcher(log);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        
        return null;
    }
    
    /**
     * 로그 샘플링 (큰 로그 파일 처리용)
     */
    public static List<String> sampleLogs(List<String> logs, int sampleSize) {
        if (logs == null || logs.size() <= sampleSize) {
            return logs;
        }
        
        // 균등 샘플링
        List<String> sampled = new ArrayList<>();
        double step = (double) logs.size() / sampleSize;
        
        for (int i = 0; i < sampleSize; i++) {
            int index = (int) (i * step);
            if (index < logs.size()) {
                sampled.add(logs.get(index));
            }
        }
        
        return sampled;
    }
    
    /**
     * 로그 통계 정보 생성
     */
    public static Map<String, Object> getLogStatistics(List<String> logs) {
        Map<String, Object> stats = new HashMap<>();
        
        if (logs == null || logs.isEmpty()) {
            stats.put("totalLogs", 0);
            stats.put("avgLength", 0);
            stats.put("minLength", 0);
            stats.put("maxLength", 0);
            return stats;
        }
        
        int totalLength = 0;
        int minLength = Integer.MAX_VALUE;
        int maxLength = 0;
        
        for (String log : logs) {
            int length = log.length();
            totalLength += length;
            minLength = Math.min(minLength, length);
            maxLength = Math.max(maxLength, length);
        }
        
        stats.put("totalLogs", logs.size());
        stats.put("avgLength", totalLength / logs.size());
        stats.put("minLength", minLength);
        stats.put("maxLength", maxLength);
        
        // 타임스탬프 포함 비율
        long logsWithTimestamp = logs.stream()
            .filter(log -> extractTimestamp(log) != null)
            .count();
        stats.put("timestampRatio", (double) logsWithTimestamp / logs.size());
        
        return stats;
    }
    
    /**
     * 로그 필터링 (빈 라인 제거 등)
     */
    public static List<String> filterLogs(List<String> logs) {
        if (logs == null) {
            return new ArrayList<>();
        }
        
        return logs.stream()
            .filter(log -> log != null && !log.trim().isEmpty())
            .collect(Collectors.toList());
    }
}