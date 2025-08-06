import com.logcenter.recommender.grok.GrokCompilerWrapper;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.Match;
import java.util.*;
import java.util.concurrent.*;

public class TestSimpleTimeout {
    
    public static void main(String[] args) {
        System.out.println("=== 단순 타임아웃 테스트 ===\n");
        
        // GrokCompilerWrapper 생성
        GrokCompilerWrapper grokCompiler = new GrokCompilerWrapper();
        
        // 표준 및 커스텀 패턴 로드
        grokCompiler.loadStandardPatterns();
        int customCount = grokCompiler.loadCustomPatterns();
        System.out.println("커스텀 패턴 " + customCount + "개 로드\n");
        
        // 테스트할 패턴과 샘플
        String pattern = "^\\[%{DATE_FORMAT1:log_time}\\] %{SRC_IP:src_ip} \"%{METHOD:method} %{URL:url}\" %{STATUS:status} %{DURATION:duration}ms$";
        String sample = "[2016.11.08 13:25:44] 203.233.74.11 \"POST /index_sso.jsp\" 200 3ms";
        
        System.out.println("패턴: " + pattern);
        System.out.println("샘플: " + sample);
        System.out.println();
        
        // 1. 직접 매칭 테스트
        System.out.println("1. 직접 매칭 테스트:");
        try {
            long startTime = System.currentTimeMillis();
            Grok grok = grokCompiler.compile(pattern);
            Match match = grok.match(sample);
            Map<String, Object> captures = match.capture();
            long elapsed = System.currentTimeMillis() - startTime;
            
            System.out.println("  매칭 성공! 시간: " + elapsed + "ms");
            System.out.println("  추출된 필드: " + captures.keySet());
        } catch (Exception e) {
            System.out.println("  매칭 실패: " + e.getMessage());
        }
        
        // 2. ExecutorService를 사용한 테스트 (Validator와 같은 방식)
        System.out.println("\n2. ExecutorService를 사용한 테스트:");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            long startTime = System.currentTimeMillis();
            final Grok grok = grokCompiler.compile(pattern);
            
            Future<Map<String, Object>> future = executor.submit(() -> {
                Match match = grok.match(sample);
                return match.capture();
            });
            
            Map<String, Object> captures = future.get(10000, TimeUnit.MILLISECONDS);
            long elapsed = System.currentTimeMillis() - startTime;
            
            System.out.println("  매칭 성공! 시간: " + elapsed + "ms");
            System.out.println("  추출된 필드: " + captures.keySet());
        } catch (TimeoutException e) {
            System.out.println("  타임아웃 발생!");
        } catch (Exception e) {
            System.out.println("  매칭 실패: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
        
        // 3. 병렬 처리 테스트
        System.out.println("\n3. 병렬 처리 테스트 (10개 동시):");
        ExecutorService parallelExecutor = Executors.newFixedThreadPool(4);
        try {
            long startTime = System.currentTimeMillis();
            final Grok grok = grokCompiler.compile(pattern);
            
            // 10개의 동일한 작업을 동시에 실행
            List<Future<Map<String, Object>>> futures = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                futures.add(parallelExecutor.submit(() -> {
                    Match match = grok.match(sample);
                    return match.capture();
                }));
            }
            
            // 모든 결과 기다리기
            int successCount = 0;
            int timeoutCount = 0;
            for (Future<Map<String, Object>> future : futures) {
                try {
                    future.get(10000, TimeUnit.MILLISECONDS);
                    successCount++;
                } catch (TimeoutException e) {
                    timeoutCount++;
                }
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("  완료! 총 시간: " + elapsed + "ms");
            System.out.println("  성공: " + successCount + ", 타임아웃: " + timeoutCount);
        } catch (Exception e) {
            System.out.println("  테스트 실패: " + e.getMessage());
        } finally {
            parallelExecutor.shutdown();
        }
    }
}