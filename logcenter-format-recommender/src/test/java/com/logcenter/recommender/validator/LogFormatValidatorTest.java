package com.logcenter.recommender.validator;

import com.logcenter.recommender.model.LogFormat;
import com.logcenter.recommender.model.ValidationResult;
import com.logcenter.recommender.model.ValidationResult.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * LogFormatValidator 테스트
 */
public class LogFormatValidatorTest {
    
    private LogFormatValidator validator;
    
    @Before
    public void setUp() {
        validator = new LogFormatValidator();
    }
    
    @After
    public void tearDown() {
        if (validator != null) {
            validator.shutdown();
        }
    }
    
    @Test
    public void testValidateNormalFormat() {
        // 정상적인 포맷 생성 (유효 필드 3개 이상)
        LogFormat format = createTestFormat(
            "TEST_FORMAT_1",
            "Test Format",
            "%{LOG_TIME:log_time} %{SRC_IP:src_ip} %{SRC_PORT:src_port} %{ACTION:action}",
            "20240101120000 192.168.1.1 8080 allow"
        );
        
        List<ValidationResult> results = validator.validateFormat(format);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        ValidationResult result = results.get(0);
        assertEquals(Status.PASS, result.getStatus());
        assertFalse(result.hasErrors());
        assertNotNull(result.getExtractedFields());
        assertTrue(result.getExtractedFields().containsKey("log_time"));
        assertTrue(result.getExtractedFields().containsKey("src_ip"));
        assertTrue(result.getExtractedFields().containsKey("src_port"));
        assertTrue(result.getExtractedFields().containsKey("action"));
    }
    
    @Test
    public void testValidateFormatWithCompilationError() {
        // 컴파일 오류가 있는 포맷
        LogFormat format = createTestFormat(
            "TEST_FORMAT_2",
            "Test Format with Error",
            "%{INVALID_PATTERN:field} %{ANOTHER_INVALID:field2}",
            "test log"
        );
        
        List<ValidationResult> results = validator.validateFormat(format);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        ValidationResult result = results.get(0);
        assertEquals(Status.FAIL, result.getStatus());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrorMessages().get(0).contains("패턴 컴파일"));
    }
    
    @Test
    public void testValidateFormatWithMismatchedSample() {
        // 샘플 로그와 매칭되지 않는 포맷
        LogFormat format = createTestFormat(
            "TEST_FORMAT_3",
            "Test Format with Mismatch",
            "^%{LOG_TIME:log_time} %{SRC_IP:src_ip}$",
            "this is completely different log"
        );
        
        List<ValidationResult> results = validator.validateFormat(format);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        ValidationResult result = results.get(0);
        assertEquals(Status.FAIL, result.getStatus());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrorMessages().stream()
            .anyMatch(msg -> msg.contains("매칭되지 않음")));
    }
    
    @Test
    public void testValidateFormatWithWarnings() {
        // 경고가 발생하는 포맷 (필드 수 부족)
        LogFormat format = createTestFormat(
            "TEST_FORMAT_4",
            "Test Format with Warnings",
            "%{LOG_TIME:log_time} %{GREEDYDATA:message}",
            "20240101120000 some log message"
        );
        
        List<ValidationResult> results = validator.validateFormat(format);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        ValidationResult result = results.get(0);
        assertEquals(Status.WARNING, result.getStatus());
        assertFalse(result.hasErrors());
        assertTrue(result.hasWarnings());
        assertTrue(result.getWarningMessages().stream()
            .anyMatch(msg -> msg.contains("유효 필드 수") || msg.contains("GREEDYDATA")));
    }
    
    @Test
    public void testValidateFormatWithoutSample() {
        // 샘플 로그가 없는 포맷
        LogFormat format = createTestFormat(
            "TEST_FORMAT_5",
            "Test Format without Sample",
            "%{LOG_TIME:log_time} %{SRC_IP:src_ip} %{SRC_PORT:src_port} %{ACTION:action}",
            null
        );
        
        List<ValidationResult> results = validator.validateFormat(format);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        ValidationResult result = results.get(0);
        // 샘플이 없을 때는 WARNING 상태
        assertEquals(Status.WARNING, result.getStatus());
        assertFalse(result.hasErrors());
        assertTrue(result.hasWarnings());
        assertTrue(result.getWarningMessages().stream()
            .anyMatch(msg -> msg.contains("샘플 로그가 없")));
    }
    
    // 전체 포맷 검증 테스트는 시간이 오래 걸려서 주석 처리
    // 필요시 주석 해제하여 실행
    /*
    @Test
    public void testValidateAllFormats() {
        // 실제 파일 테스트 (작은 서브셋)
        List<ValidationResult> results = validator.validateAllFormats();
        
        assertNotNull(results);
        assertFalse("Results should not be empty", results.isEmpty());
        
        // 통계 확인
        long passCount = results.stream().filter(r -> r.getStatus() == Status.PASS).count();
        long warnCount = results.stream().filter(r -> r.getStatus() == Status.WARNING).count();
        long failCount = results.stream().filter(r -> r.getStatus() == Status.FAIL).count();
        
        System.out.println(String.format(
            "검증 결과: 전체=%d, 통과=%d, 경고=%d, 실패=%d",
            results.size(), passCount, warnCount, failCount
        ));
        
        // 최소한 일부는 통과해야 함
        assertTrue("일부 포맷은 통과해야 함", passCount > 0);
    }
    */
    
    /**
     * 테스트용 LogFormat 생성
     */
    private LogFormat createTestFormat(String formatId, String formatName, 
                                      String grokExp, String sampleLog) {
        LogFormat format = new LogFormat();
        format.setFormatId(formatId);
        format.setFormatName(formatName);
        format.setGroupName("TEST_GROUP");
        format.setVendor("TEST_VENDOR");
        
        LogFormat.LogType logType = new LogFormat.LogType();
        logType.setTypeName("Test Type");
        
        LogFormat.Pattern pattern = new LogFormat.Pattern();
        pattern.setExpName(formatId + "_PATTERN");
        pattern.setGrokExp(grokExp);
        pattern.setSampleLog(sampleLog);
        
        List<LogFormat.Pattern> patterns = new ArrayList<>();
        patterns.add(pattern);
        logType.setPatterns(patterns);
        
        List<LogFormat.LogType> logTypes = new ArrayList<>();
        logTypes.add(logType);
        format.setLogTypes(logTypes);
        
        return format;
    }
}