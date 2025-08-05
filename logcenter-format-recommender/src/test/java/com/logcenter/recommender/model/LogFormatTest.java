package com.logcenter.recommender.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;

/**
 * LogFormat 모델 클래스 단위 테스트
 */
public class LogFormatTest {
    
    private LogFormat logFormat;
    
    @Before
    public void setUp() {
        logFormat = new LogFormat();
        logFormat.setFormatId("APACHE_HTTP_1.00");
        logFormat.setFormatName("APACHE_HTTP");
        logFormat.setFormatVersion("1.00");
        logFormat.setGroupName("Web Server");
        logFormat.setGroupId("WEB");
        logFormat.setVendor("APACHE");
        logFormat.setModel("HTTP");
        logFormat.setSmType("logformat6");
    }
    
    @Test
    public void testGettersAndSetters() {
        assertEquals("APACHE_HTTP_1.00", logFormat.getFormatId());
        assertEquals("APACHE_HTTP", logFormat.getFormatName());
        assertEquals("1.00", logFormat.getFormatVersion());
        assertEquals("Web Server", logFormat.getGroupName());
        assertEquals("WEB", logFormat.getGroupId());
        assertEquals("APACHE", logFormat.getVendor());
        assertEquals("HTTP", logFormat.getModel());
        assertEquals("logformat6", logFormat.getSmType());
    }
    
    @Test
    public void testLogTypes() {
        LogFormat.LogType logType1 = new LogFormat.LogType();
        logType1.setTypeName("Access Log");
        logType1.setTypeDescription("Apache access log");
        
        LogFormat.LogType logType2 = new LogFormat.LogType();
        logType2.setTypeName("Error Log");
        logType2.setTypeDescription("Apache error log");
        
        List<LogFormat.LogType> logTypes = Arrays.asList(logType1, logType2);
        logFormat.setLogTypes(logTypes);
        
        assertEquals(2, logFormat.getLogTypes().size());
        assertEquals("Access Log", logFormat.getLogTypes().get(0).getTypeName());
        assertEquals("Error Log", logFormat.getLogTypes().get(1).getTypeName());
    }
    
    @Test
    public void testPatterns() {
        LogFormat.Pattern pattern1 = new LogFormat.Pattern();
        pattern1.setExpName("APACHE_HTTP_1.00_1");
        pattern1.setGrokExp("%{IP:client} %{WORD:ident} %{WORD:auth} \\[%{HTTPDATE:timestamp}\\] \"%{WORD:method} %{URIPATH:request} %{WORD:protocol}/%{NUMBER:version}\" %{NUMBER:status} %{NUMBER:bytes}");
        pattern1.setSampleLog("192.168.1.1 - - [15/Jan/2024:10:30:45 +0900] \"GET /index.html HTTP/1.1\" 200 1234");
        pattern1.setOrder("1");
        
        LogFormat.LogType logType = new LogFormat.LogType();
        logType.setTypeName("Access Log");
        logType.setPatterns(Arrays.asList(pattern1));
        
        assertEquals(1, logType.getPatterns().size());
        assertEquals("APACHE_HTTP_1.00_1", logType.getPatterns().get(0).getExpName());
        assertNotNull(logType.getPatterns().get(0).getGrokExp());
        assertNotNull(logType.getPatterns().get(0).getSampleLog());
    }
    
    @Test
    public void testEquals() {
        LogFormat other = new LogFormat();
        other.setFormatId("APACHE_HTTP_1.00");
        
        assertTrue(logFormat.equals(other));
        
        other.setFormatId("NGINX_1.00");
        assertFalse(logFormat.equals(other));
    }
    
    @Test
    public void testHashCode() {
        LogFormat other = new LogFormat();
        other.setFormatId("APACHE_HTTP_1.00");
        
        assertEquals(logFormat.hashCode(), other.hashCode());
    }
    
    @Test
    public void testToString() {
        String str = logFormat.toString();
        assertTrue(str.contains("APACHE_HTTP_1.00"));
        assertTrue(str.contains("APACHE_HTTP"));
        assertTrue(str.contains("Web Server"));
        assertTrue(str.contains("APACHE"));
        assertTrue(str.contains("HTTP"));
    }
}