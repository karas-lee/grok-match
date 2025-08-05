package com.logcenter.recommender.cli;

import com.logcenter.recommender.model.FormatRecommendation;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.util.JacksonJsonUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 출력 포맷터
 * 다양한 형식으로 결과를 출력
 */
public class OutputFormatter {
    
    private final OutputFormat format;
    private final boolean showDetail;
    
    public OutputFormatter(OutputFormat format, boolean showDetail) {
        this.format = format;
        this.showDetail = showDetail;
    }
    
    /**
     * 추천 결과 출력
     */
    public void printRecommendations(List<FormatRecommendation> recommendations, boolean showStats) {
        if (recommendations.isEmpty()) {
            System.out.println("매칭되는 로그 포맷을 찾을 수 없습니다.");
            return;
        }
        
        switch (format) {
            case JSON:
                printRecommendationsJson(recommendations);
                break;
            case CSV:
                printRecommendationsCsv(recommendations);
                break;
            case TEXT:
            default:
                printRecommendationsText(recommendations, showStats);
                break;
        }
    }
    
    /**
     * 배치 결과 출력
     */
    public void printBatchResults(List<List<FormatRecommendation>> batchResults, 
                                 String fileName, boolean showStats) {
        if (format == OutputFormat.JSON) {
            Map<String, Object> result = new HashMap<>();
            result.put("file", fileName);
            result.put("totalSamples", batchResults.size());
            
            // 상위 포맷 집계
            Map<String, Integer> formatCounts = new HashMap<>();
            for (List<FormatRecommendation> recommendations : batchResults) {
                if (!recommendations.isEmpty()) {
                    FormatRecommendation top = recommendations.get(0);
                    formatCounts.merge(top.getFormatId(), 1, Integer::sum);
                }
            }
            
            result.put("topFormats", formatCounts);
            System.out.println(JacksonJsonUtils.toJson(result));
            
        } else {
            System.out.println("\n파일: " + fileName);
            System.out.println("분석된 로그 라인: " + batchResults.size());
            
            // 포맷별 매칭 통계
            Map<String, Integer> formatCounts = new HashMap<>();
            int matchedCount = 0;
            
            for (List<FormatRecommendation> recommendations : batchResults) {
                if (!recommendations.isEmpty()) {
                    matchedCount++;
                    FormatRecommendation top = recommendations.get(0);
                    formatCounts.merge(top.getFormatId(), 1, Integer::sum);
                }
            }
            
            System.out.println("매칭된 로그 라인: " + matchedCount + " (" + 
                String.format("%.1f%%", (matchedCount * 100.0 / batchResults.size())) + ")");
            
            System.out.println("\n상위 매칭 포맷:");
            final int finalMatchedCount = matchedCount;
            formatCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    System.out.printf("  - %s: %d건 (%.1f%%)\n", 
                        entry.getKey(), 
                        entry.getValue(),
                        (entry.getValue() * 100.0 / finalMatchedCount));
                });
        }
    }
    
    /**
     * 텍스트 형식 출력
     */
    private void printRecommendationsText(List<FormatRecommendation> recommendations, boolean showStats) {
        System.out.println("\n=== 로그 포맷 추천 결과 ===\n");
        
        int rank = 1;
        for (FormatRecommendation rec : recommendations) {
            System.out.printf("%d. %s (신뢰도: %.1f%%)\n", 
                rank++, rec.getFormatId(), rec.getConfidence());
            
            System.out.printf("   - 포맷명: %s\n", rec.getFormatName());
            System.out.printf("   - 그룹: %s\n", rec.getGroupName());
            System.out.printf("   - 벤더: %s\n", rec.getVendor());
            
            // 매칭된 필드는 기본으로 표시
            if (rec.getMatchedFields() != null && !rec.getMatchedFields().isEmpty()) {
                System.out.println("   - 매칭된 필드:");
                rec.getMatchedFields().forEach((key, value) -> {
                    // 값이 너무 길면 축약
                    String displayValue = String.valueOf(value);
                    if (displayValue.length() > 50 && !showDetail) {
                        displayValue = displayValue.substring(0, 47) + "...";
                    }
                    System.out.println("     * " + key + ": " + displayValue);
                });
            }
            
            if (showDetail) {
                System.out.printf("   - 완전 매칭: %s\n", rec.isCompleteMatch() ? "예" : "아니오");
                System.out.printf("   - 부분 매칭: %s\n", rec.isPartialMatch() ? "예" : "아니오");
                
                if (rec.getMissingFields() != null && !rec.getMissingFields().isEmpty()) {
                    System.out.println("   - 누락된 필드:");
                    rec.getMissingFields().forEach(field -> 
                        System.out.println("     * " + field));
                }
            }
            
            System.out.println();
        }
        
        if (showStats) {
            printStatistics(recommendations);
        }
    }
    
    /**
     * JSON 형식 출력
     */
    private void printRecommendationsJson(List<FormatRecommendation> recommendations) {
        System.out.println(JacksonJsonUtils.toJson(recommendations));
    }
    
    /**
     * CSV 형식 출력
     */
    private void printRecommendationsCsv(List<FormatRecommendation> recommendations) {
        // CSV 헤더
        System.out.println("순위,포맷ID,포맷명,그룹,벤더,신뢰도,완전매칭,부분매칭");
        
        int rank = 1;
        for (FormatRecommendation rec : recommendations) {
            System.out.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",%.1f,%s,%s\n",
                rank++,
                rec.getFormatId(),
                rec.getFormatName(),
                rec.getGroupName(),
                rec.getVendor(),
                rec.getConfidence(),
                rec.isCompleteMatch() ? "Y" : "N",
                rec.isPartialMatch() ? "Y" : "N"
            );
        }
    }
    
    /**
     * 통계 정보 출력
     */
    private void printStatistics(List<FormatRecommendation> recommendations) {
        System.out.println("\n=== 통계 정보 ===");
        
        // 그룹별 분포
        Map<String, Long> groupCounts = recommendations.stream()
            .collect(Collectors.groupingBy(
                FormatRecommendation::getGroupName,
                Collectors.counting()
            ));
        
        System.out.println("\n그룹별 분포:");
        groupCounts.forEach((group, count) -> 
            System.out.printf("  - %s: %d개\n", group, count));
        
        // 신뢰도 분포
        DoubleSummaryStatistics confidenceStats = recommendations.stream()
            .mapToDouble(FormatRecommendation::getConfidence)
            .summaryStatistics();
        
        System.out.println("\n신뢰도 통계:");
        System.out.printf("  - 평균: %.1f%%\n", confidenceStats.getAverage());
        System.out.printf("  - 최대: %.1f%%\n", confidenceStats.getMax());
        System.out.printf("  - 최소: %.1f%%\n", confidenceStats.getMin());
    }
    
    /**
     * 포맷 목록 출력
     */
    public void printFormatList(List<LogFormat> formats) {
        if (format == OutputFormat.JSON) {
            System.out.println(JacksonJsonUtils.toJson(formats));
            return;
        }
        
        System.out.println("\n=== 사용 가능한 로그 포맷 ===");
        System.out.println("총 " + formats.size() + "개\n");
        
        // 그룹별로 정렬
        Map<String, List<LogFormat>> byGroup = formats.stream()
            .collect(Collectors.groupingBy(LogFormat::getGroupName));
        
        byGroup.forEach((group, groupFormats) -> {
            System.out.println("\n[" + group + "]");
            groupFormats.forEach(f -> {
                System.out.printf("  - %s: %s (%s)\n", 
                    f.getFormatId(), f.getFormatName(), f.getVendor());
            });
        });
    }
    
    /**
     * 그룹 통계 출력
     */
    public void printGroupStatistics(Map<String, Integer> groupStats) {
        if (format == OutputFormat.JSON) {
            System.out.println(JacksonJsonUtils.toJson(groupStats));
            return;
        }
        
        System.out.println("\n=== 그룹별 포맷 수 ===");
        groupStats.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> 
                System.out.printf("  - %s: %d개\n", entry.getKey(), entry.getValue()));
    }
    
    /**
     * 벤더 통계 출력
     */
    public void printVendorStatistics(Map<String, Integer> vendorStats) {
        if (format == OutputFormat.JSON) {
            System.out.println(JacksonJsonUtils.toJson(vendorStats));
            return;
        }
        
        System.out.println("\n=== 벤더별 포맷 수 ===");
        vendorStats.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> 
                System.out.printf("  - %s: %d개\n", entry.getKey(), entry.getValue()));
    }
}