package com.logcenter.recommender.validator;

import com.logcenter.recommender.model.ValidationResult;
import com.logcenter.recommender.model.ValidationResult.Status;
import com.logcenter.recommender.util.JsonUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 검증 결과 리포트 생성기
 */
public class ValidationReportGenerator {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 텍스트 리포트 생성
     */
    public String generateTextReport(List<ValidationResult> results) {
        StringBuilder report = new StringBuilder();
        
        // 헤더
        report.append("=============================================================\n");
        report.append("                로그 포맷 검증 리포트\n");
        report.append("                생성 시간: ").append(DATE_FORMAT.format(new Date())).append("\n");
        report.append("=============================================================\n\n");
        
        // 통계
        Map<Status, Long> statistics = results.stream()
            .collect(Collectors.groupingBy(ValidationResult::getStatus, Collectors.counting()));
        
        report.append("■ 검증 통계\n");
        report.append("---------------------------------------------------------\n");
        report.append(String.format("전체 포맷 수: %d\n", results.size()));
        report.append(String.format("✓ 통과: %d\n", statistics.getOrDefault(Status.PASS, 0L)));
        report.append(String.format("⚠ 경고: %d\n", statistics.getOrDefault(Status.WARNING, 0L)));
        report.append(String.format("✗ 실패: %d\n", statistics.getOrDefault(Status.FAIL, 0L)));
        report.append(String.format("— 건너뜀: %d\n", statistics.getOrDefault(Status.SKIPPED, 0L)));
        
        double successRate = calculateSuccessRate(results);
        report.append(String.format("\n성공률: %.2f%%\n\n", successRate));
        
        // 그룹별 통계
        Map<String, List<ValidationResult>> byGroup = results.stream()
            .filter(r -> r.getGroupName() != null)
            .collect(Collectors.groupingBy(ValidationResult::getGroupName));
        
        if (!byGroup.isEmpty()) {
            report.append("■ 그룹별 통계\n");
            report.append("---------------------------------------------------------\n");
            
            for (Map.Entry<String, List<ValidationResult>> entry : byGroup.entrySet()) {
                String groupName = entry.getKey();
                List<ValidationResult> groupResults = entry.getValue();
                
                long passCount = groupResults.stream().filter(r -> r.getStatus() == Status.PASS).count();
                long warnCount = groupResults.stream().filter(r -> r.getStatus() == Status.WARNING).count();
                long failCount = groupResults.stream().filter(r -> r.getStatus() == Status.FAIL).count();
                
                report.append(String.format("%-20s: 전체 %3d | 통과 %3d | 경고 %3d | 실패 %3d\n",
                    groupName, groupResults.size(), passCount, warnCount, failCount));
            }
            report.append("\n");
        }
        
        // 실패 목록
        List<ValidationResult> failures = results.stream()
            .filter(r -> r.getStatus() == Status.FAIL)
            .collect(Collectors.toList());
        
        if (!failures.isEmpty()) {
            report.append("■ 실패 포맷 상세\n");
            report.append("---------------------------------------------------------\n");
            
            for (ValidationResult result : failures) {
                report.append(String.format("\n포맷: %s (%s)\n", 
                    result.getFormatName(), result.getExpName()));
                report.append(String.format("그룹: %s\n", result.getGroupName()));
                
                if (!result.getErrorMessages().isEmpty()) {
                    report.append("오류:\n");
                    for (String error : result.getErrorMessages()) {
                        report.append("  - ").append(error).append("\n");
                    }
                }
                
                if (result.getGrokExpression() != null) {
                    report.append("패턴: ").append(result.getGrokExpression()).append("\n");
                }
                
                if (result.getSampleLog() != null) {
                    report.append("샘플: ").append(result.getSampleLog()).append("\n");
                }
            }
            report.append("\n");
        }
        
        // 경고 목록 (선택적)
        List<ValidationResult> warnings = results.stream()
            .filter(r -> r.getStatus() == Status.WARNING)
            .collect(Collectors.toList());
        
        if (!warnings.isEmpty()) {
            report.append("■ 경고 포맷 요약\n");
            report.append("---------------------------------------------------------\n");
            
            Map<String, Long> warningTypes = new HashMap<>();
            
            for (ValidationResult result : warnings) {
                for (String warning : result.getWarningMessages()) {
                    String type = extractWarningType(warning);
                    warningTypes.merge(type, 1L, Long::sum);
                }
            }
            
            for (Map.Entry<String, Long> entry : warningTypes.entrySet()) {
                report.append(String.format("  %s: %d건\n", entry.getKey(), entry.getValue()));
            }
            report.append("\n");
        }
        
        // 푸터
        report.append("=============================================================\n");
        
        return report.toString();
    }
    
    /**
     * HTML 리포트 생성
     */
    public String generateHtmlReport(List<ValidationResult> results) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<title>로그 포맷 검증 리포트</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: 'Malgun Gothic', sans-serif; margin: 20px; }\n");
        html.append("h1 { color: #333; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #f2f2f2; }\n");
        html.append(".pass { color: green; }\n");
        html.append(".warning { color: orange; }\n");
        html.append(".fail { color: red; }\n");
        html.append(".statistics { background-color: #f9f9f9; padding: 15px; margin: 20px 0; }\n");
        html.append(".error-details { background-color: #ffe5e5; padding: 10px; margin: 10px 0; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");
        
        html.append("<h1>로그 포맷 검증 리포트</h1>\n");
        html.append("<p>생성 시간: ").append(DATE_FORMAT.format(new Date())).append("</p>\n");
        
        // 통계
        Map<Status, Long> statistics = results.stream()
            .collect(Collectors.groupingBy(ValidationResult::getStatus, Collectors.counting()));
        
        double successRate = calculateSuccessRate(results);
        
        html.append("<div class='statistics'>\n");
        html.append("<h2>검증 통계</h2>\n");
        html.append("<ul>\n");
        html.append("<li>전체 포맷 수: ").append(results.size()).append("</li>\n");
        html.append("<li class='pass'>통과: ").append(statistics.getOrDefault(Status.PASS, 0L)).append("</li>\n");
        html.append("<li class='warning'>경고: ").append(statistics.getOrDefault(Status.WARNING, 0L)).append("</li>\n");
        html.append("<li class='fail'>실패: ").append(statistics.getOrDefault(Status.FAIL, 0L)).append("</li>\n");
        html.append("<li>건너뜀: ").append(statistics.getOrDefault(Status.SKIPPED, 0L)).append("</li>\n");
        html.append(String.format("<li><strong>성공률: %.2f%%</strong></li>\n", successRate));
        html.append("</ul>\n");
        html.append("</div>\n");
        
        // 상세 결과 테이블
        html.append("<h2>상세 검증 결과</h2>\n");
        html.append("<table>\n");
        html.append("<tr>\n");
        html.append("<th>포맷 ID</th>\n");
        html.append("<th>패턴명</th>\n");
        html.append("<th>그룹</th>\n");
        html.append("<th>상태</th>\n");
        html.append("<th>메시지</th>\n");
        html.append("<th>소요시간(ms)</th>\n");
        html.append("</tr>\n");
        
        for (ValidationResult result : results) {
            html.append("<tr>\n");
            html.append("<td>").append(result.getFormatId()).append("</td>\n");
            html.append("<td>").append(result.getExpName() != null ? result.getExpName() : "-").append("</td>\n");
            html.append("<td>").append(result.getGroupName() != null ? result.getGroupName() : "-").append("</td>\n");
            
            String statusClass = result.getStatus().name().toLowerCase();
            html.append(String.format("<td class='%s'>%s</td>\n", 
                statusClass, result.getStatus().getDescription()));
            
            html.append("<td>");
            if (!result.getErrorMessages().isEmpty()) {
                html.append("오류: ");
                html.append(String.join(", ", result.getErrorMessages()));
            } else if (!result.getWarningMessages().isEmpty()) {
                html.append("경고: ");
                html.append(String.join(", ", result.getWarningMessages()));
            } else {
                html.append("성공");
            }
            html.append("</td>\n");
            
            html.append("<td>").append(result.getValidationTime()).append("</td>\n");
            html.append("</tr>\n");
        }
        
        html.append("</table>\n");
        html.append("</body>\n</html>\n");
        
        return html.toString();
    }
    
    /**
     * JSON 리포트 생성
     */
    public String generateJsonReport(List<ValidationResult> results) {
        Map<String, Object> report = new HashMap<>();
        
        // 메타데이터
        report.put("generatedAt", DATE_FORMAT.format(new Date()));
        report.put("totalFormats", results.size());
        
        // 통계
        Map<Status, Long> statistics = results.stream()
            .collect(Collectors.groupingBy(ValidationResult::getStatus, Collectors.counting()));
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("passed", statistics.getOrDefault(Status.PASS, 0L));
        stats.put("warnings", statistics.getOrDefault(Status.WARNING, 0L));
        stats.put("failed", statistics.getOrDefault(Status.FAIL, 0L));
        stats.put("skipped", statistics.getOrDefault(Status.SKIPPED, 0L));
        report.put("statistics", stats);
        
        report.put("successRate", calculateSuccessRate(results));
        
        // 상세 결과
        report.put("results", results);
        
        return JsonUtils.toJson(report);
    }
    
    /**
     * 파일로 리포트 저장
     */
    public void saveToFile(String content, String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.write(content);
        }
    }
    
    /**
     * 성공률 계산
     */
    private double calculateSuccessRate(List<ValidationResult> results) {
        if (results.isEmpty()) {
            return 0.0;
        }
        
        long successCount = results.stream()
            .filter(r -> r.getStatus() == Status.PASS || r.getStatus() == Status.WARNING)
            .count();
        
        return (double) successCount / results.size() * 100.0;
    }
    
    /**
     * 경고 타입 추출
     */
    private String extractWarningType(String warning) {
        if (warning.contains("유효 필드 수")) {
            return "필드 수 부족";
        } else if (warning.contains("GREEDYDATA")) {
            return "GREEDYDATA 사용";
        } else if (warning.contains("일반적인 패턴")) {
            return "너무 일반적인 패턴";
        } else if (warning.contains("샘플 로그가 없")) {
            return "샘플 로그 누락";
        } else {
            return "기타";
        }
    }
}