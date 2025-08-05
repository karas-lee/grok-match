package com.logcenter.recommender.cli;

import com.logcenter.recommender.grok.FilePatternRepository;
import com.logcenter.recommender.grok.GrokCompilerWrapper;
import com.logcenter.recommender.model.FormatRecommendation;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.service.LogFormatRecommender;
import com.logcenter.recommender.service.LogFormatRecommenderImpl;
import picocli.CommandLine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * CLI 명령어 구현
 * Picocli 프레임워크를 사용한 명령행 인터페이스
 */
@Command(
    name = "logformat-recommender",
    mixinStandardHelpOptions = true,
    version = "LogCenter Format Recommender 1.0.0",
    description = "SIEM 로그 포맷 추천 도구",
    footer = "\n로그 샘플을 분석하여 가장 적합한 로그 포맷을 추천합니다.",
    exitCodeOnExecutionException = 1,
    exitCodeOnInvalidInput = 1
)
public class CliCommand implements Callable<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(CliCommand.class);
    
    @Parameters(
        index = "0",
        description = "분석할 로그 샘플 (텍스트 또는 파일 경로)",
        arity = "0..1"
    )
    private String logInput;
    
    @Option(
        names = {"-f", "--file"},
        description = "입력이 파일 경로임을 명시"
    )
    private boolean isFile;
    
    @Option(
        names = {"-d", "--directory"},
        description = "디렉토리의 모든 로그 파일 분석"
    )
    private boolean isDirectory;
    
    @Option(
        names = {"-g", "--group"},
        description = "특정 그룹으로 필터링 (예: FIREWALL, IPS, WAF)"
    )
    private String groupFilter;
    
    @Option(
        names = {"-v", "--vendor"},
        description = "특정 벤더로 필터링 (예: CISCO, FORTINET)"
    )
    private String vendorFilter;
    
    @Option(
        names = {"-n", "--top"},
        description = "상위 N개 결과만 표시 (기본값: 5)",
        defaultValue = "5"
    )
    private int topN;
    
    @Option(
        names = {"-m", "--min-confidence"},
        description = "최소 신뢰도 임계값 (기본값: 70)",
        defaultValue = "70"
    )
    private int minConfidence;
    
    @Option(
        names = {"-o", "--output"},
        description = "출력 형식: text, json, csv (기본값: text)",
        defaultValue = "text"
    )
    private OutputFormat outputFormat;
    
    @Option(
        names = {"--detail"},
        description = "상세 정보 표시"
    )
    private boolean showDetail;
    
    @Option(
        names = {"--stats"},
        description = "통계 정보 표시"
    )
    private boolean showStats;
    
    @Option(
        names = {"--list-formats"},
        description = "사용 가능한 모든 로그 포맷 목록 표시"
    )
    private boolean listFormats;
    
    @Option(
        names = {"--list-groups"},
        description = "사용 가능한 모든 그룹 목록 표시"
    )
    private boolean listGroups;
    
    @Option(
        names = {"--list-vendors"},
        description = "사용 가능한 모든 벤더 목록 표시"
    )
    private boolean listVendors;
    
    private LogFormatRecommender recommender;
    private OutputFormatter formatter;
    
    @Override
    public Integer call() throws Exception {
        try {
            // 서비스 초기화
            initializeService();
            
            // 목록 표시 명령 처리
            if (listFormats) {
                return listAllFormats();
            }
            if (listGroups) {
                return listAllGroups();
            }
            if (listVendors) {
                return listAllVendors();
            }
            
            // 로그 입력 확인
            if (logInput == null) {
                System.err.println("오류: 로그 입력이 필요합니다.");
                return 1;
            }
            
            // 로그 분석 수행
            if (isDirectory) {
                return analyzeDirectory();
            } else if (isFile || new File(logInput).exists()) {
                return analyzeFile();
            } else {
                return analyzeText();
            }
            
        } catch (Exception e) {
            logger.error("실행 중 오류 발생", e);
            System.err.println("오류: " + e.getMessage());
            return 1;
        } finally {
            if (recommender != null) {
                recommender.shutdown();
            }
        }
    }
    
    /**
     * 서비스 초기화
     */
    private void initializeService() {
        // 패턴 저장소 생성
        FilePatternRepository repository = new FilePatternRepository();
        
        // Grok 컴파일러 생성
        GrokCompilerWrapper grokCompiler = new GrokCompilerWrapper();
        
        // 추천 서비스 생성
        recommender = new LogFormatRecommenderImpl(repository, grokCompiler);
        
        // 초기화
        if (!recommender.initialize()) {
            throw new RuntimeException("추천 서비스 초기화 실패");
        }
        
        // 출력 포맷터 생성
        formatter = new OutputFormatter(outputFormat, showDetail);
        
        logger.info("서비스 초기화 완료");
    }
    
    /**
     * 텍스트 로그 분석
     */
    private Integer analyzeText() {
        logger.info("텍스트 로그 분석 시작");
        
        List<FormatRecommendation> recommendations = recommender.recommend(
            logInput, 
            createRecommendOptions()
        );
        
        formatter.printRecommendations(recommendations, showStats);
        
        return 0;
    }
    
    /**
     * 파일 로그 분석
     */
    private Integer analyzeFile() throws IOException {
        logger.info("파일 로그 분석 시작: {}", logInput);
        
        Path path = Paths.get(logInput);
        if (!Files.exists(path)) {
            System.err.println("오류: 파일을 찾을 수 없습니다: " + logInput);
            return 1;
        }
        
        // 파일 읽기
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        
        // 각 라인 분석 (배치 처리)
        List<String> logSamples = new ArrayList<>();
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                logSamples.add(line.trim());
                if (logSamples.size() >= 100) { // 배치 크기
                    break;
                }
            }
        }
        
        // 배치 추천
        List<List<FormatRecommendation>> batchResults = recommender.recommendBatch(
            logSamples, 
            createRecommendOptions()
        );
        
        // 결과 집계 및 출력
        formatter.printBatchResults(batchResults, path.getFileName().toString(), showStats);
        
        return 0;
    }
    
    /**
     * 디렉토리 내 로그 파일 분석
     */
    private Integer analyzeDirectory() throws IOException {
        logger.info("디렉토리 분석 시작: {}", logInput);
        
        Path dirPath = Paths.get(logInput);
        if (!Files.isDirectory(dirPath)) {
            System.err.println("오류: 디렉토리가 아닙니다: " + logInput);
            return 1;
        }
        
        // 로그 파일 찾기
        List<Path> logFiles = new ArrayList<>();
        Files.walk(dirPath, 1)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".log") || 
                        p.toString().endsWith(".txt"))
            .forEach(logFiles::add);
        
        if (logFiles.isEmpty()) {
            System.err.println("로그 파일을 찾을 수 없습니다.");
            return 1;
        }
        
        System.out.println(logFiles.size() + "개의 로그 파일을 발견했습니다.\n");
        
        // 각 파일 분석
        for (Path logFile : logFiles) {
            System.out.println("\n=== " + logFile.getFileName() + " ===");
            logInput = logFile.toString();
            analyzeFile();
        }
        
        return 0;
    }
    
    /**
     * 모든 포맷 목록 표시
     */
    private Integer listAllFormats() {
        try {
            List<LogFormat> formats = recommender.getAvailableFormats();
            formatter.printFormatList(formats);
            return 0;
        } catch (Exception e) {
            logger.error("포맷 목록 출력 중 오류", e);
            System.err.println("오류: 포맷 목록을 가져올 수 없습니다 - " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * 모든 그룹 목록 표시
     */
    private Integer listAllGroups() {
        try {
            formatter.printGroupStatistics(recommender.getGroupStatistics());
            return 0;
        } catch (Exception e) {
            logger.error("그룹 목록 출력 중 오류", e);
            System.err.println("오류: 그룹 목록을 가져올 수 없습니다 - " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * 모든 벤더 목록 표시
     */
    private Integer listAllVendors() {
        try {
            formatter.printVendorStatistics(recommender.getVendorStatistics());
            return 0;
        } catch (Exception e) {
            logger.error("벤더 목록 출력 중 오류", e);
            System.err.println("오류: 벤더 목록을 가져올 수 없습니다 - " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * 추천 옵션 생성
     */
    private LogFormatRecommender.RecommendOptions createRecommendOptions() {
        LogFormatRecommender.RecommendOptions options = 
            new LogFormatRecommender.RecommendOptions();
        
        options.setMaxResults(topN);
        options.setMinConfidence(minConfidence);
        
        if (groupFilter != null) {
            options.setGroupFilter(groupFilter);
        }
        if (vendorFilter != null) {
            options.setVendorFilter(vendorFilter);
        }
        
        return options;
    }
}