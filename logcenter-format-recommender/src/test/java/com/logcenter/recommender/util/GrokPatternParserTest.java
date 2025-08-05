package com.logcenter.recommender.util;

import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.*;

/**
 * GrokPatternParser 테스트
 */
public class GrokPatternParserTest {
    
    @Test
    public void testExtractNamedFields() {
        // Given
        String pattern = "%{IP:client_ip} %{DATA:ident} %{DATA:auth} \\[%{DATA:timestamp}\\] \"%{WORD} %{DATA:uri} %{DATA:protocol}\" %{NUMBER:status} %{NUMBER:bytes}";
        
        // When
        Set<String> namedFields = GrokPatternParser.extractNamedFields(pattern);
        
        // Then
        assertEquals(8, namedFields.size());
        assertTrue(namedFields.contains("client_ip"));
        assertTrue(namedFields.contains("ident"));
        assertTrue(namedFields.contains("auth"));
        assertTrue(namedFields.contains("timestamp"));
        assertTrue(namedFields.contains("uri"));
        assertTrue(namedFields.contains("protocol"));
        assertTrue(namedFields.contains("status"));
        assertTrue(namedFields.contains("bytes"));
        
        // WORD는 필드명이 없으므로 포함되지 않아야 함
        assertFalse(namedFields.contains("WORD"));
    }
    
    @Test
    public void testExtractNamedFieldsWithUnnamedPatterns() {
        // Given
        String pattern = "%{DATE_FORMAT18:event_time} %{SKIP}%{DATE_FORMAT3} %{DATA:hostname} %{WORD:program}\\[%{INT:pid}\\]: Failed password for %{DATA:user} from %{IP:src_ip} port %{INT:src_port} %{SKIP}";
        
        // When
        Set<String> namedFields = GrokPatternParser.extractNamedFields(pattern);
        
        // Then
        assertEquals(7, namedFields.size());
        assertTrue(namedFields.contains("event_time"));
        assertTrue(namedFields.contains("hostname"));
        assertTrue(namedFields.contains("program"));
        assertTrue(namedFields.contains("pid"));
        assertTrue(namedFields.contains("user"));
        assertTrue(namedFields.contains("src_ip"));
        assertTrue(namedFields.contains("src_port"));
        
        // SKIP과 DATE_FORMAT3는 필드명이 없으므로 포함되지 않아야 함
        assertFalse(namedFields.contains("SKIP"));
        assertFalse(namedFields.contains("DATE_FORMAT3"));
    }
    
    @Test
    public void testEmptyPattern() {
        // Given
        String pattern = "";
        
        // When
        Set<String> namedFields = GrokPatternParser.extractNamedFields(pattern);
        
        // Then
        assertTrue(namedFields.isEmpty());
    }
    
    @Test
    public void testNullPattern() {
        // Given
        String pattern = null;
        
        // When
        Set<String> namedFields = GrokPatternParser.extractNamedFields(pattern);
        
        // Then
        assertTrue(namedFields.isEmpty());
    }
    
    @Test
    public void testPatternWithSpaces() {
        // Given
        String pattern = "%{IP: client_ip } %{DATA: message }";
        
        // When
        Set<String> namedFields = GrokPatternParser.extractNamedFields(pattern);
        
        // Then
        assertEquals(2, namedFields.size());
        assertTrue(namedFields.contains("client_ip"));
        assertTrue(namedFields.contains("message"));
    }
    
    @Test
    public void testIsNamedField() {
        // Given
        String pattern = "%{IP:client_ip} %{WORD} %{DATA:message}";
        
        // When & Then
        assertTrue(GrokPatternParser.isNamedField(pattern, "client_ip"));
        assertTrue(GrokPatternParser.isNamedField(pattern, "message"));
        assertFalse(GrokPatternParser.isNamedField(pattern, "WORD"));
        assertFalse(GrokPatternParser.isNamedField(pattern, "IP"));
        assertFalse(GrokPatternParser.isNamedField(pattern, "DATA"));
    }
}