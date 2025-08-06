import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import io.krakens.grok.api.Match;
import java.util.Map;

public class TestGrokPattern {
    public static void main(String[] args) {
        try {
            GrokCompiler grokCompiler = GrokCompiler.newInstance();
            grokCompiler.registerDefaultPatterns();
            
            // Register custom patterns
            grokCompiler.register("LOG_TIME", "\\d{14}");
            grokCompiler.register("PRI", "\\d{1,3}");
            grokCompiler.register("DATE_FORMAT3", "[A-Za-z]{3} \\d{1,2} \\d{2}:\\d{2}:\\d{2}");
            grokCompiler.register("DATE_FORMAT4", "\\d{2}:\\d{2}:\\d{2}");
            grokCompiler.register("EVENT_NAME", "[a-zA-Z]+");
            grokCompiler.register("EVENT_ID", "\\d+");
            grokCompiler.register("SKIP_NUMBER", "\\d+");
            grokCompiler.register("MESSAGE", ".*");
            grokCompiler.register("SRC_IP", "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
            grokCompiler.register("SRC_PORT", "\\d+");
            grokCompiler.register("DST_IP", "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
            grokCompiler.register("DST_PORT", "\\d+");
            grokCompiler.register("TMP", ".*");
            grokCompiler.register("SKIP", ".*?");
            
            String pattern = "^%{LOG_TIME:log_time} <%{PRI:pri}>%{DATE_FORMAT3} %{EVENT_NAME:event_name}\\[%{EVENT_ID:event_id}\\]: %{DATE_FORMAT4}.%{SKIP_NUMBER} %{MESSAGE:message} %{SRC_IP:src_ip},%{SRC_PORT:src_port} %{SKIP} %{DST_IP:dst_ip},%{DST_PORT:dst_port} %{TMP:tmp}$";
            String log = "20150313134323 <133>Mar 13 13:48:22 ipmon[836]: 13:48:22.229395 lan900 @0:16 p 192.168.1.188,5454 -> 192.168.1.199,5454 PR tcp len 20 44 -S IN";
            
            System.out.println("Pattern: " + pattern);
            System.out.println("Log: " + log);
            System.out.println();
            
            Grok grok = grokCompiler.compile(pattern);
            Match match = grok.match(log);
            Map<String, Object> capture = match.capture();
            
            if (capture.isEmpty()) {
                System.out.println("No match found!");
            } else {
                System.out.println("Match found!");
                for (Map.Entry<String, Object> entry : capture.entrySet()) {
                    System.out.println("  " + entry.getKey() + ": " + entry.getValue());
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}