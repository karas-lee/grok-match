package com.logcenter.recommender.test;

import com.logcenter.recommender.filter.PatternFilter;
import com.logcenter.recommender.grok.FilePatternRepository;
import com.logcenter.recommender.grok.GrokCompilerWrapper;
import com.logcenter.recommender.matcher.AdvancedLogMatcher;
import com.logcenter.recommender.model.FormatRecommendation;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.service.LogFormatRecommender;
import com.logcenter.recommender.service.LogFormatRecommenderImpl;

import java.util.List;

/**
 * 필터링된 패턴으로 테스트
 */
public class FilteredPatternTest {
    
    public static void main(String[] args) {
        System.out.println("=== 패턴 필터링 테스트 ===\n");
        
        // 너무 일반적인 패턴 확인
        String[] genericPatterns = {
            "^%{LOG_TIME:log_time} %{MESSAGE:message}$",
            "^%{MESSAGE:message}$",
            "^%{GREEDYDATA:data}$"
        };
        
        System.out.println("일반적인 패턴 테스트:");
        for (String pattern : genericPatterns) {
            boolean isGeneric = PatternFilter.isOverlyGeneric(pattern);
            System.out.printf("패턴: %-50s -> %s\n", 
                pattern, isGeneric ? "일반적임" : "구체적임");
        }
        
        System.out.println("\n구체적인 패턴 테스트:");
        String[] specificPatterns = {
            "^%{DATE_FORMAT1:log_time} %{SRC_IP:src_ip} %{DST_IP:dst_ip} %{ACTION:action}$",
            "^%{IP:client_ip} - - \\[%{HTTPDATE:timestamp}\\] \"%{METHOD:method} %{PATH:path} HTTP/%{NUMBER:http_version}\" %{NUMBER:status} %{NUMBER:bytes}$"
        };
        
        for (String pattern : specificPatterns) {
            boolean isGeneric = PatternFilter.isOverlyGeneric(pattern);
            double score = PatternFilter.getSpecificityScore(pattern);
            System.out.printf("패턴: %-100s -> %s (점수: %.2f)\n", 
                pattern, isGeneric ? "일반적임" : "구체적임", score);
        }
        
        System.out.println("\n=== 실제 로그 추천 테스트 (필터링 적용) ===\n");
        
        // 테스트 로그
        String[] testLogs = {
            "2025-08-05 10:15:30 This is just a simple message",
            "192.168.1.100 - - [05/Aug/2025:10:15:30 +0900] \"GET /index.html HTTP/1.1\" 200 1234",
            "Aug  5 10:15:30 server01 sshd[1234]: Accepted password for user1 from 192.168.1.100 port 22 ssh2"
        };
        
        try {
            // 추천 서비스 초기화
            FilePatternRepository repository = new FilePatternRepository();
            GrokCompilerWrapper grokCompiler = new GrokCompilerWrapper();
            LogFormatRecommender recommender = new LogFormatRecommenderImpl(repository, grokCompiler);
            recommender.initialize();
            
            // 각 로그에 대해 추천
            for (String log : testLogs) {
                System.out.println("로그: " + log);
                
                LogFormatRecommender.RecommendOptions options = new LogFormatRecommender.RecommendOptions();
                options.setMaxResults(3);
                
                List<FormatRecommendation> recommendations = recommender.recommend(log, options);
                
                if (recommendations.isEmpty()) {
                    System.out.println("  -> 일치하는 포맷이 없습니다.");
                } else {
                    for (FormatRecommendation rec : recommendations) {
                        System.out.printf("  -> %s (신뢰도: %.1f%%)\n", 
                            rec.getFormatName(), rec.getConfidence());
                    }
                }
                System.out.println();
            }
            
            // 통계 출력
            System.out.println("\n=== 포맷 통계 ===");
            List<LogFormat> allFormats = recommender.getAvailableFormats();
            int genericCount = 0;
            
            for (LogFormat format : allFormats) {
                if (format.getGrokPattern() != null && 
                    PatternFilter.isOverlyGeneric(format.getGrokPattern())) {
                    genericCount++;
                }
            }
            
            System.out.printf("전체 포맷: %d개\n", allFormats.size());
            System.out.printf("일반적인 패턴 포맷: %d개 (%.1f%%)\n", 
                genericCount, (genericCount * 100.0 / allFormats.size()));
            System.out.printf("구체적인 패턴 포맷: %d개 (%.1f%%)\n", 
                allFormats.size() - genericCount, 
                ((allFormats.size() - genericCount) * 100.0 / allFormats.size()));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}