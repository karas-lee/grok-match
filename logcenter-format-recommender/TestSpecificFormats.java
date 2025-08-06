import com.logcenter.recommender.validator.LogFormatValidator;
import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.model.ValidationResult;
import com.logcenter.recommender.util.JacksonJsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class TestSpecificFormats {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== 특정 포맷 검증 테스트 ===\n");
        
        // setting_logformat.json 로드
        List<LogFormat> allFormats;
        try (InputStream is = TestSpecificFormats.class.getResourceAsStream("/setting_logformat.json");
             InputStreamReader reader = new InputStreamReader(is)) {
            allFormats = JacksonJsonUtils.fromJson(reader, new TypeReference<List<LogFormat>>() {});
        }
        
        // TMAXSOFT와 APACHE 포맷만 필터링
        List<LogFormat> targetFormats = allFormats.stream()
            .filter(f -> f.getFormatName() != null && 
                        (f.getFormatName().contains("TMAXSOFT") || 
                         f.getFormatName().contains("APACHE")))
            .collect(Collectors.toList());
        
        System.out.println("테스트할 포맷 수: " + targetFormats.size());
        targetFormats.forEach(f -> System.out.println("  - " + f.getFormatName()));
        System.out.println();
        
        // Validator 생성
        LogFormatValidator validator = new LogFormatValidator();
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 각 포맷을 개별적으로 검증
            for (LogFormat format : targetFormats) {
                System.out.println("\n검증 중: " + format.getFormatName());
                long formatStart = System.currentTimeMillis();
                
                List<ValidationResult> results = validator.validateFormat(format);
                
                long formatTime = System.currentTimeMillis() - formatStart;
                System.out.println("  소요 시간: " + formatTime + "ms");
                
                // 결과 요약
                long passCount = results.stream()
                    .filter(r -> r.getStatus() == ValidationResult.Status.PASS)
                    .count();
                long failCount = results.stream()
                    .filter(r -> r.getStatus() == ValidationResult.Status.FAIL)
                    .count();
                long warningCount = results.stream()
                    .filter(r -> r.getStatus() == ValidationResult.Status.WARNING)
                    .count();
                
                System.out.println("  결과: PASS=" + passCount + ", FAIL=" + failCount + ", WARNING=" + warningCount);
                
                // 타임아웃 오류 확인
                List<ValidationResult> timeoutResults = results.stream()
                    .filter(r -> r.getErrorMessages() != null && 
                                r.getErrorMessages().stream().anyMatch(e -> e.contains("타임아웃")))
                    .collect(Collectors.toList());
                
                if (!timeoutResults.isEmpty()) {
                    System.out.println("  타임아웃 발생: " + timeoutResults.size() + "개");
                    timeoutResults.forEach(r -> {
                        System.out.println("    - " + r.getExpName() + ": " + r.getErrorMessages());
                    });
                }
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("\n총 검증 시간: " + totalTime + "ms");
            
        } finally {
            validator.shutdown();
        }
    }
}