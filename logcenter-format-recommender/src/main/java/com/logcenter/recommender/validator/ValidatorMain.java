package com.logcenter.recommender.validator;

import com.logcenter.recommender.model.ValidationResult;
import com.logcenter.recommender.model.ValidationResult.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 로그 포맷 검증 도구 메인 클래스
 * 명령줄에서 독립적으로 실행 가능
 */
public class ValidatorMain {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidatorMain.class);
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  로그 포맷 검증 도구 v1.0.0");
        System.out.println("========================================\n");
        
        // 옵션 파싱
        String resourcePath = "/setting_logformat.json";
        String outputFormat = "text"; // text, html, json
        String outputFile = null;
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-f":
                case "--file":
                    if (i + 1 < args.length) {
                        resourcePath = args[++i];
                    }
                    break;
                case "-o":
                case "--output":
                    if (i + 1 < args.length) {
                        outputFile = args[++i];
                    }
                    break;
                case "--format":
                    if (i + 1 < args.length) {
                        outputFormat = args[++i].toLowerCase();
                    }
                    break;
                case "-h":
                case "--help":
                    printHelp();
                    return;
            }
        }
        
        // 검증 실행
        LogFormatValidator validator = new LogFormatValidator();
        ValidationReportGenerator reportGenerator = new ValidationReportGenerator();
        
        try {
            System.out.println("포맷 파일 로드 중: " + resourcePath);
            
            // 검증 수행
            List<ValidationResult> results = validator.validateAllFormats(resourcePath);
            
            if (results.isEmpty()) {
                System.err.println("검증할 포맷이 없습니다.");
                System.exit(1);
            }
            
            // 리포트 생성
            String report;
            switch (outputFormat) {
                case "html":
                    report = reportGenerator.generateHtmlReport(results);
                    break;
                case "json":
                    report = reportGenerator.generateJsonReport(results);
                    break;
                default:
                    report = reportGenerator.generateTextReport(results);
                    break;
            }
            
            // 결과 출력
            if (outputFile != null) {
                try {
                    reportGenerator.saveToFile(report, outputFile);
                    System.out.println("\n리포트가 저장되었습니다: " + outputFile);
                } catch (IOException e) {
                    System.err.println("리포트 저장 실패: " + e.getMessage());
                    System.out.println("\n" + report);
                }
            } else {
                System.out.println("\n" + report);
            }
            
            // 종료 코드 설정 (실패가 있으면 1, 없으면 0)
            long failCount = results.stream()
                .filter(r -> r.getStatus() == Status.FAIL)
                .count();
            
            if (failCount > 0) {
                System.err.println(String.format("\n⚠ 경고: %d개의 포맷에서 오류가 발견되었습니다.", failCount));
                
                // 실패한 포맷 목록 출력
                List<String> failedFormats = results.stream()
                    .filter(r -> r.getStatus() == Status.FAIL)
                    .map(r -> String.format("%s (%s)", r.getFormatName(), r.getExpName()))
                    .collect(Collectors.toList());
                
                System.err.println("실패 포맷:");
                failedFormats.forEach(f -> System.err.println("  - " + f));
                
                System.exit(1);
            } else {
                System.out.println("\n✓ 모든 포맷이 검증을 통과했습니다.");
            }
            
        } finally {
            validator.shutdown();
        }
    }
    
    /**
     * 도움말 출력
     */
    private static void printHelp() {
        System.out.println("사용법: java -cp <classpath> com.logcenter.recommender.validator.ValidatorMain [options]");
        System.out.println();
        System.out.println("옵션:");
        System.out.println("  -f, --file <path>    검증할 포맷 파일 경로 (기본값: /setting_logformat.json)");
        System.out.println("  -o, --output <file>  결과를 파일로 저장");
        System.out.println("  --format <type>      출력 형식: text, html, json (기본값: text)");
        System.out.println("  -h, --help           도움말 표시");
        System.out.println();
        System.out.println("예시:");
        System.out.println("  # 기본 검증");
        System.out.println("  java -jar validator.jar");
        System.out.println();
        System.out.println("  # HTML 리포트 생성");
        System.out.println("  java -jar validator.jar --format html -o report.html");
        System.out.println();
        System.out.println("  # JSON 형식으로 출력");
        System.out.println("  java -jar validator.jar --format json");
    }
}