package com.logcenter.recommender.grok;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 필드 검증기 추상 클래스
 * Grok으로 추출된 필드값의 유효성과 의미적 일치를 검증
 */
public abstract class FieldValidator {
    protected static final Logger logger = LoggerFactory.getLogger(FieldValidator.class);
    
    /**
     * 필드값 검증
     * @param value 검증할 값
     * @return 유효성 여부
     */
    public abstract boolean validate(String value);
    
    /**
     * 필드명과 값의 의미적 일치 여부 검증
     * @param fieldName 필드명
     * @param fieldValue 필드값
     * @return 의미적으로 일치하면 true
     */
    public abstract boolean validateSemantics(String fieldName, String fieldValue);
    
    /**
     * 필드 타입 반환
     * @return 필드 타입 (예: IP, PORT, TIMESTAMP)
     */
    public abstract String getFieldType();
    
    /**
     * 검증 규칙 설명
     * @return 검증 규칙 설명
     */
    public abstract String getDescription();
    
    /**
     * 필드 타입별 검증기 생성 팩토리 메소드
     */
    public static Map<String, FieldValidator> createValidators() {
        Map<String, FieldValidator> validators = new HashMap<>();
        
        // IP 주소 검증기
        IPFieldValidator ipValidator = new IPFieldValidator();
        validators.put("src_ip", ipValidator);
        validators.put("dst_ip", ipValidator);
        validators.put("source_ip", ipValidator);
        validators.put("dest_ip", ipValidator);
        validators.put("client_ip", ipValidator);
        validators.put("server_ip", ipValidator);
        validators.put("host_ip", ipValidator);
        validators.put("device_ip", ipValidator);
        validators.put("nat_src_ip", ipValidator);
        validators.put("nat_dst_ip", ipValidator);
        
        // 포트 검증기
        PortFieldValidator portValidator = new PortFieldValidator();
        validators.put("src_port", portValidator);
        validators.put("dst_port", portValidator);
        validators.put("source_port", portValidator);
        validators.put("dest_port", portValidator);
        validators.put("client_port", portValidator);
        validators.put("server_port", portValidator);
        validators.put("nat_src_port", portValidator);
        validators.put("nat_dst_port", portValidator);
        
        // 타임스탬프 검증기
        TimestampFieldValidator timestampValidator = new TimestampFieldValidator();
        validators.put("log_time", timestampValidator);
        validators.put("timestamp", timestampValidator);
        validators.put("date", timestampValidator);
        validators.put("time", timestampValidator);
        validators.put("datetime", timestampValidator);
        validators.put("event_time", timestampValidator);
        validators.put("start_time", timestampValidator);
        validators.put("end_time", timestampValidator);
        
        // 프로토콜 검증기
        ProtocolFieldValidator protocolValidator = new ProtocolFieldValidator();
        validators.put("protocol", protocolValidator);
        validators.put("proto", protocolValidator);
        
        // 액션 검증기
        ActionFieldValidator actionValidator = new ActionFieldValidator();
        validators.put("action", actionValidator);
        validators.put("event_action", actionValidator);
        validators.put("rule_action", actionValidator);
        
        // 디바이스명 검증기
        DeviceNameFieldValidator deviceValidator = new DeviceNameFieldValidator();
        validators.put("device_name", deviceValidator);
        validators.put("host_name", deviceValidator);
        validators.put("hostname", deviceValidator);
        validators.put("computer_name", deviceValidator);
        validators.put("terminal_name", deviceValidator);
        
        // HTTP 상태 코드 검증기
        HTTPStatusFieldValidator httpValidator = new HTTPStatusFieldValidator();
        validators.put("http_status", httpValidator);
        validators.put("status_code", httpValidator);
        validators.put("response_code", httpValidator);
        
        // 이벤트 ID 검증기
        EventIDFieldValidator eventValidator = new EventIDFieldValidator();
        validators.put("event_id", eventValidator);
        validators.put("rule_id", eventValidator);
        validators.put("attack_id", eventValidator);
        validators.put("session_id", eventValidator);
        
        return validators;
    }
    
    /**
     * IP 주소 필드 검증기
     */
    public static class IPFieldValidator extends FieldValidator {
        private static final Pattern IP_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(" +
            "25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(" +
            "25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(" +
            "25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        );
        
        @Override
        public boolean validate(String value) {
            if (value == null || value.isEmpty()) return false;
            if ("-".equals(value) || "?.?.?.?".equals(value)) return true;
            return IP_PATTERN.matcher(value).matches();
        }
        
        @Override
        public boolean validateSemantics(String fieldName, String fieldValue) {
            // IP 필드에 시간값이나 단순 숫자가 들어오면 거부
            if (fieldValue.contains(":") && !fieldValue.contains(".")) {
                logger.debug("IP 필드 {}에 시간값 의심: {}", fieldName, fieldValue);
                return false;
            }
            
            // 단순 숫자만 있는 경우 거부 (포트 번호로 의심)
            if (fieldValue.matches("^\\d+$")) {
                try {
                    int num = Integer.parseInt(fieldValue);
                    if (num < 256 || num > 65535) {
                        logger.debug("IP 필드 {}에 단순 숫자: {}", fieldName, fieldValue);
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            
            return validate(fieldValue);
        }
        
        @Override
        public String getFieldType() {
            return "IP_ADDRESS";
        }
        
        @Override
        public String getDescription() {
            return "IPv4 주소 형식 검증";
        }
    }
    
    /**
     * 포트 필드 검증기
     */
    public static class PortFieldValidator extends FieldValidator {
        @Override
        public boolean validate(String value) {
            if (value == null || value.isEmpty()) return false;
            if ("-".equals(value)) return true;
            
            try {
                int port = Integer.parseInt(value);
                return port >= 0 && port <= 65535;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        @Override
        public boolean validateSemantics(String fieldName, String fieldValue) {
            // 포트 필드에 IP 주소가 들어오면 거부
            if (fieldValue.contains(".")) {
                logger.debug("포트 필드 {}에 IP 주소 의심: {}", fieldName, fieldValue);
                return false;
            }
            
            // 포트 필드에 시간값이 들어오면 거부
            if (fieldValue.contains(":")) {
                logger.debug("포트 필드 {}에 시간값 의심: {}", fieldName, fieldValue);
                return false;
            }
            
            return validate(fieldValue);
        }
        
        @Override
        public String getFieldType() {
            return "PORT_NUMBER";
        }
        
        @Override
        public String getDescription() {
            return "포트 번호 (0-65535) 검증";
        }
    }
    
    /**
     * 타임스탬프 필드 검증기
     */
    public static class TimestampFieldValidator extends FieldValidator {
        private static final Pattern[] TIME_PATTERNS = {
            Pattern.compile("^\\d{14}$"),  // YYYYMMDDHHmmss
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}"),  // ISO format
            Pattern.compile("^\\d{2}:\\d{2}:\\d{2}"),  // HH:mm:ss
            Pattern.compile("^\\d{2}:\\d{2}:\\d{2}\\.\\d+"),  // HH:mm:ss.microseconds
            Pattern.compile("^[A-Za-z]{3} \\d{1,2} \\d{2}:\\d{2}:\\d{2}")  // Mar 13 13:48:22
        };
        
        @Override
        public boolean validate(String value) {
            if (value == null || value.isEmpty()) return false;
            
            for (Pattern pattern : TIME_PATTERNS) {
                if (pattern.matcher(value).find()) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public boolean validateSemantics(String fieldName, String fieldValue) {
            // 타임스탬프 필드에 IP 주소가 들어오면 거부
            if (fieldValue.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                logger.debug("타임스탬프 필드 {}에 IP 주소: {}", fieldName, fieldValue);
                return false;
            }
            
            // 타임스탬프 필드에 단순 작은 숫자가 들어오면 거부
            if (fieldValue.matches("^\\d{1,5}$")) {
                logger.debug("타임스탬프 필드 {}에 작은 숫자: {}", fieldName, fieldValue);
                return false;
            }
            
            return validate(fieldValue);
        }
        
        @Override
        public String getFieldType() {
            return "TIMESTAMP";
        }
        
        @Override
        public String getDescription() {
            return "날짜/시간 형식 검증";
        }
    }
    
    /**
     * 프로토콜 필드 검증기
     */
    public static class ProtocolFieldValidator extends FieldValidator {
        private static final String[] VALID_PROTOCOLS = {
            "tcp", "udp", "icmp", "http", "https", "ftp", "ssh", 
            "telnet", "smtp", "pop3", "imap", "dns", "dhcp", "arp",
            "TCP", "UDP", "ICMP", "HTTP", "HTTPS", "FTP", "SSH",
            "PR", "esp", "ah", "gre", "igmp", "ospf"
        };
        
        @Override
        public boolean validate(String value) {
            if (value == null || value.isEmpty()) return false;
            
            for (String protocol : VALID_PROTOCOLS) {
                if (protocol.equalsIgnoreCase(value)) {
                    return true;
                }
            }
            
            // 프로토콜 번호도 허용 (1-255)
            try {
                int protoNum = Integer.parseInt(value);
                return protoNum >= 0 && protoNum <= 255;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        @Override
        public boolean validateSemantics(String fieldName, String fieldValue) {
            // 프로토콜 필드에 IP 주소가 들어오면 거부
            if (fieldValue.contains(".") && fieldValue.matches(".*\\d+\\.\\d+\\.\\d+\\.\\d+.*")) {
                logger.debug("프로토콜 필드 {}에 IP 주소: {}", fieldName, fieldValue);
                return false;
            }
            
            // 프로토콜 필드에 시간값이 들어오면 거부
            if (fieldValue.contains(":") && fieldValue.matches(".*\\d{2}:\\d{2}.*")) {
                logger.debug("프로토콜 필드 {}에 시간값: {}", fieldName, fieldValue);
                return false;
            }
            
            return validate(fieldValue);
        }
        
        @Override
        public String getFieldType() {
            return "PROTOCOL";
        }
        
        @Override
        public String getDescription() {
            return "네트워크 프로토콜 검증";
        }
    }
    
    /**
     * 액션 필드 검증기
     */
    public static class ActionFieldValidator extends FieldValidator {
        private static final String[] VALID_ACTIONS = {
            "allow", "deny", "drop", "accept", "reject", "block",
            "permit", "pass", "alert", "log", "monitor", "detect",
            "ALLOW", "DENY", "DROP", "ACCEPT", "REJECT", "BLOCK",
            "p", "b", "a", "d"  // 축약형
        };
        
        @Override
        public boolean validate(String value) {
            if (value == null || value.isEmpty()) return false;
            
            String lowerValue = value.toLowerCase();
            for (String action : VALID_ACTIONS) {
                if (action.equalsIgnoreCase(value) || lowerValue.contains(action.toLowerCase())) {
                    return true;
                }
            }
            
            // 숫자 액션 코드도 허용 (0-255)
            try {
                int actionCode = Integer.parseInt(value);
                return actionCode >= 0 && actionCode <= 255;
            } catch (NumberFormatException e) {
                return true; // 알 수 없는 액션도 일단 허용
            }
        }
        
        @Override
        public boolean validateSemantics(String fieldName, String fieldValue) {
            // 액션 필드에 날짜/시간이 들어오면 거부
            if (fieldValue.matches("^\\d{2}:\\d{2}.*") || 
                fieldValue.matches("^\\d{4}-\\d{2}-\\d{2}.*")) {
                logger.debug("액션 필드 {}에 날짜/시간: {}", fieldName, fieldValue);
                return false;
            }
            
            // 액션 필드에 IP 주소가 들어오면 거부
            if (fieldValue.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                logger.debug("액션 필드 {}에 IP 주소: {}", fieldName, fieldValue);
                return false;
            }
            
            return true;
        }
        
        @Override
        public String getFieldType() {
            return "ACTION";
        }
        
        @Override
        public String getDescription() {
            return "보안 액션 검증";
        }
    }
    
    /**
     * 디바이스명 필드 검증기
     */
    public static class DeviceNameFieldValidator extends FieldValidator {
        @Override
        public boolean validate(String value) {
            if (value == null || value.isEmpty()) return false;
            
            // 디바이스명은 일반적으로 영문자, 숫자, 하이픈, 언더스코어 포함
            return value.matches("^[a-zA-Z0-9\\-_.]+$");
        }
        
        @Override
        public boolean validateSemantics(String fieldName, String fieldValue) {
            // 디바이스명 필드에 시간값이 들어오면 거부
            if (fieldValue.matches("^\\d{2}:\\d{2}:\\d{2}.*")) {
                logger.debug("디바이스명 필드 {}에 시간값: {}", fieldName, fieldValue);
                return false;
            }
            
            // 디바이스명 필드에 날짜가 들어오면 거부
            if (fieldValue.matches("^\\d{4}-\\d{2}-\\d{2}.*") ||
                fieldValue.matches("^\\d{14}$")) {
                logger.debug("디바이스명 필드 {}에 날짜: {}", fieldName, fieldValue);
                return false;
            }
            
            // 디바이스명 필드에 순수 IP 주소가 들어오면 거부
            if (fieldValue.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                logger.debug("디바이스명 필드 {}에 IP 주소: {}", fieldName, fieldValue);
                return false;
            }
            
            return true;
        }
        
        @Override
        public String getFieldType() {
            return "DEVICE_NAME";
        }
        
        @Override
        public String getDescription() {
            return "디바이스/호스트명 검증";
        }
    }
    
    /**
     * HTTP 상태 코드 필드 검증기
     */
    public static class HTTPStatusFieldValidator extends FieldValidator {
        @Override
        public boolean validate(String value) {
            if (value == null || value.isEmpty()) return false;
            
            try {
                int status = Integer.parseInt(value);
                // HTTP 상태 코드는 100-599 범위
                return status >= 100 && status <= 599;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        @Override
        public boolean validateSemantics(String fieldName, String fieldValue) {
            return validate(fieldValue);
        }
        
        @Override
        public String getFieldType() {
            return "HTTP_STATUS";
        }
        
        @Override
        public String getDescription() {
            return "HTTP 상태 코드 (100-599) 검증";
        }
    }
    
    /**
     * 이벤트 ID 필드 검증기
     */
    public static class EventIDFieldValidator extends FieldValidator {
        @Override
        public boolean validate(String value) {
            if (value == null || value.isEmpty()) return false;
            
            // 이벤트 ID는 숫자 또는 영숫자 조합
            return value.matches("^[a-zA-Z0-9\\-_]+$");
        }
        
        @Override
        public boolean validateSemantics(String fieldName, String fieldValue) {
            // 이벤트 ID 필드에 시간값이 들어오면 거부
            if (fieldValue.matches("^\\d{2}:\\d{2}:\\d{2}.*")) {
                logger.debug("이벤트 ID 필드 {}에 시간값: {}", fieldName, fieldValue);
                return false;
            }
            
            // 이벤트 ID 필드에 IP 주소가 들어오면 거부
            if (fieldValue.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                logger.debug("이벤트 ID 필드 {}에 IP 주소: {}", fieldName, fieldValue);
                return false;
            }
            
            return true;
        }
        
        @Override
        public String getFieldType() {
            return "EVENT_ID";
        }
        
        @Override
        public String getDescription() {
            return "이벤트/규칙 ID 검증";
        }
    }
}