import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import io.krakens.grok.api.Match;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class TestTimeoutPatterns {
    
    private static Map<String, String> loadCustomPatterns() throws Exception {
        Map<String, String> patterns = new HashMap<>();
        
        try (InputStream is = TestTimeoutPatterns.class.getResourceAsStream("/custom-grok-patterns");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    patterns.put(parts[0], parts[1]);
                }
            }
        }
        
        return patterns;
    }
    
    public static void main(String[] args) throws Exception {
        // 테스트할 패턴들
        Map<String, String> testCases = new HashMap<>();
        
        testCases.put("TMAXSOFT_JEUS_1.00_1", 
            "[2016.11.08 13:25:44] 203.233.74.11 \"POST /index_sso.jsp\" 200 3ms");
        
        testCases.put("TMAXSOFT_WEBTOB_1.00_1", 
            "10.101.12.15 - - [05/Sep/2013:00:00:05 +0900] \"POST /sc/bizcomm/Controller.jsp HTTP/1.1\" 200 6679");
        
        testCases.put("APACHE_HTTP_1.00_1", 
            "128.134.225.3 - - [25/Jul/2013:13:35:38 +0900] \"GET /01_notice/02_recruit_list.jsp?appl_type= HTTP/1.1\" 200 3323");
        
        Map<String, String> patterns = new HashMap<>();
        patterns.put("TMAXSOFT_JEUS_1.00_1", 
            "^\\[%{DATE_FORMAT1:log_time}\\] %{SRC_IP:src_ip} \"%{METHOD:method} %{URL:url}\" %{STATUS:status} %{DURATION:duration}ms$");
        
        patterns.put("TMAXSOFT_WEBTOB_1.00_1", 
            "^%{SRC_IP:src_ip} %{SKIP} %{USER_NAME:user_name} \\[%{DATE_FORMAT2:log_time} %{SKIP}\"%{METHOD:method} %{URL:url} %{PROTOCOL:protocol}\" %{STATUS:status} %{SENT_SIZE:sent_size}$");
        
        patterns.put("APACHE_HTTP_1.00_1", 
            "^%{SRC_IP:src_ip} %{SKIP} %{USER_NAME:user_name} \\[%{DATE_FORMAT2:log_time} %{SKIP}\"%{METHOD:method} %{URL:url} %{PROTOCOL:protocol}\" %{STATUS:status} %{SENT_SIZE:sent_size}$");
        
        // GrokCompiler 초기화
        GrokCompiler compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        
        // 커스텀 패턴 로드
        System.out.println("커스텀 패턴 로드 중...");
        Map<String, String> customPatterns = loadCustomPatterns();
        for (Map.Entry<String, String> entry : customPatterns.entrySet()) {
            compiler.register(entry.getKey(), entry.getValue());
        }
        System.out.println("커스텀 패턴 " + customPatterns.size() + "개 로드 완료\n");
        
        // 각 패턴 테스트
        for (Map.Entry<String, String> testCase : testCases.entrySet()) {
            String formatName = testCase.getKey();
            String sampleLog = testCase.getValue();
            String pattern = patterns.get(formatName);
            
            System.out.println("=== " + formatName + " 테스트 ===");
            System.out.println("패턴: " + pattern);
            System.out.println("샘플: " + sampleLog);
            
            try {
                long startTime = System.currentTimeMillis();
                
                // 타임아웃 설정을 위한 스레드
                Thread matchThread = new Thread(() -> {
                    try {
                        Grok grok = compiler.compile(pattern);
                        Match match = grok.match(sampleLog);
                        Map<String, Object> capture = match.capture();
                        
                        if (capture != null && !capture.isEmpty()) {
                            System.out.println("매칭 성공!");
                            System.out.println("추출된 필드:");
                            for (Map.Entry<String, Object> field : capture.entrySet()) {
                                if (field.getValue() != null) {
                                    System.out.println("  " + field.getKey() + " = " + field.getValue());
                                }
                            }
                        } else {
                            System.out.println("매칭 실패!");
                        }
                    } catch (Exception e) {
                        System.out.println("오류 발생: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
                
                matchThread.start();
                matchThread.join(10000); // 10초 타임아웃
                
                long elapsedTime = System.currentTimeMillis() - startTime;
                
                if (matchThread.isAlive()) {
                    matchThread.interrupt();
                    System.out.println("타임아웃! (10초 초과)");
                }
                
                System.out.println("소요 시간: " + elapsedTime + "ms");
                
            } catch (Exception e) {
                System.out.println("테스트 중 오류: " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.println();
        }
    }
}