import com.logcenter.recommender.validator.LogFormatValidator;
import com.logcenter.recommender.model.ValidationResult;
import java.util.List;
import java.util.stream.Collectors;

public class TestValidatorTimeout {
    
    public static void main(String[] args) {
        System.out.println("=== Validator 타임아웃 문제 분석 ===\n");
        
        // Validator 생성
        LogFormatValidator validator = new LogFormatValidator();
        
        try {
            System.out.println("전체 포맷 검증 시작...\n");
            long startTime = System.currentTimeMillis();
            
            // 전체 검증 실행
            List<ValidationResult> results = validator.validateAllFormats();
            
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("총 검증 시간: " + totalTime + "ms\n");
            
            // 타임아웃 발생 포맷만 필터링
            List<ValidationResult> timeoutResults = results.stream()
                .filter(r -> r.getErrorMessages() != null && 
                            r.getErrorMessages().stream().anyMatch(e -> e.contains("타임아웃")))
                .collect(Collectors.toList());
            
            System.out.println("타임아웃 발생 포맷 수: " + timeoutResults.size() + " / " + results.size());
            
            // 타임아웃 발생 포맷 샘플 출력
            System.out.println("\n타임아웃 발생 포맷 샘플 (최대 5개):");
            timeoutResults.stream()
                .limit(5)
                .forEach(r -> {
                    System.out.println("\n포맷: " + r.getFormatName() + " - " + r.getExpName());
                    System.out.println("그룹: " + r.getGroupName());
                    System.out.println("패턴: " + (r.getGrokExpression() != null ? 
                        r.getGrokExpression().substring(0, Math.min(100, r.getGrokExpression().length())) + "..." : "null"));
                    System.out.println("샘플: " + (r.getSampleLog() != null ? 
                        r.getSampleLog().substring(0, Math.min(100, r.getSampleLog().length())) : "null"));
                    System.out.println("검증 시간: " + r.getValidationTime() + "ms");
                });
            
            // TMAXSOFT와 APACHE 포맷 특별 분석
            System.out.println("\n=== TMAXSOFT/APACHE 포맷 분석 ===");
            results.stream()
                .filter(r -> r.getFormatName() != null && 
                            (r.getFormatName().contains("TMAXSOFT") || r.getFormatName().contains("APACHE")))
                .forEach(r -> {
                    System.out.println("\n포맷: " + r.getFormatName() + " - " + r.getExpName());
                    System.out.println("상태: " + r.getStatus());
                    System.out.println("검증 시간: " + r.getValidationTime() + "ms");
                    if (r.getErrorMessages() != null && !r.getErrorMessages().isEmpty()) {
                        System.out.println("오류: " + r.getErrorMessages());
                    }
                });
            
        } catch (Exception e) {
            System.err.println("검증 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            validator.shutdown();
        }
    }
}