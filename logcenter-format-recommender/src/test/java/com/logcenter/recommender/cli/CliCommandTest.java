package com.logcenter.recommender.cli;

import com.logcenter.recommender.Main;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import picocli.CommandLine;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * CLI 명령어 테스트
 */
public class CliCommandTest {
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }
    
    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Test
    public void testHelpOption() {
        // --help 옵션 테스트
        String[] args = {"--help"};
        
        CommandLine cmd = new CommandLine(new CliCommand());
        int exitCode = cmd.execute(args);
        
        assertEquals(0, exitCode);
        String output = outContent.toString();
        assertTrue("도움말에 설명이 포함되어야 함", 
            output.contains("SIEM 로그 포맷 추천 도구"));
    }
    
    @Test
    public void testVersionOption() {
        // --version 옵션 테스트
        String[] args = {"--version"};
        
        CommandLine cmd = new CommandLine(new CliCommand());
        int exitCode = cmd.execute(args);
        
        assertEquals(0, exitCode);
        String output = outContent.toString();
        assertTrue("버전 정보가 출력되어야 함", 
            output.contains("LogCenter Format Recommender 1.0.0"));
    }
    
    @Test
    public void testListFormatsOption() {
        // --list-formats 옵션 테스트
        String[] args = {"--list-formats"};
        
        CommandLine cmd = new CommandLine(new CliCommand());
        int exitCode = cmd.execute(args);
        
        // 디버깅을 위한 출력
        String output = outContent.toString();
        String error = errContent.toString();
        if (exitCode != 0) {
            System.out.println("Exit code: " + exitCode);
            System.out.println("Output: " + output);
            System.out.println("Error: " + error);
        }
        
        assertEquals(0, exitCode);
        assertTrue("포맷 목록이 출력되어야 함", 
            output.contains("사용 가능한 로그 포맷"));
    }
    
    @Test
    public void testListGroupsOption() {
        // --list-groups 옵션 테스트
        String[] args = {"--list-groups"};
        
        CommandLine cmd = new CommandLine(new CliCommand());
        int exitCode = cmd.execute(args);
        
        assertEquals(0, exitCode);
        String output = outContent.toString();
        assertTrue("그룹 목록이 출력되어야 함", 
            output.contains("그룹별 포맷 수"));
    }
    
    @Test
    public void testListVendorsOption() {
        // --list-vendors 옵션 테스트
        String[] args = {"--list-vendors"};
        
        CommandLine cmd = new CommandLine(new CliCommand());
        int exitCode = cmd.execute(args);
        
        assertEquals(0, exitCode);
        String output = outContent.toString();
        assertTrue("벤더 목록이 출력되어야 함", 
            output.contains("벤더별 포맷 수"));
    }
    
    @Test
    public void testAnalyzeTextLog() {
        // 텍스트 로그 분석 테스트
        String apacheLog = "192.168.1.1 - - [04/Oct/2023:14:14:53 +0900] " +
            "\"GET /index.html HTTP/1.1\" 200 2326";
        String[] args = {apacheLog};
        
        CommandLine cmd = new CommandLine(new CliCommand());
        int exitCode = cmd.execute(args);
        
        assertEquals(0, exitCode);
        String output = outContent.toString();
        assertTrue("추천 결과가 출력되어야 함", 
            output.contains("로그 포맷 추천 결과"));
    }
    
    @Test
    public void testAnalyzeWithGroupFilter() {
        // 그룹 필터 테스트
        String apacheLog = "192.168.1.1 - - [04/Oct/2023:14:14:53 +0900] " +
            "\"GET /index.html HTTP/1.1\" 200 2326";
        String[] args = {apacheLog, "--group", "WEBSERVER"};
        
        CommandLine cmd = new CommandLine(new CliCommand());
        int exitCode = cmd.execute(args);
        
        assertEquals(0, exitCode);
        String output = outContent.toString();
        assertTrue("추천 결과가 출력되어야 함", 
            output.contains("로그 포맷 추천 결과"));
    }
    
    @Test
    public void testJsonOutput() {
        // JSON 출력 테스트
        String apacheLog = "192.168.1.1 - - [04/Oct/2023:14:14:53 +0900] " +
            "\"GET /index.html HTTP/1.1\" 200 2326";
        String[] args = {apacheLog, "--output", "json"};
        
        CommandLine cmd = new CommandLine(new CliCommand());
        int exitCode = cmd.execute(args);
        
        assertEquals(0, exitCode);
        String output = outContent.toString();
        // JSON 출력 확인
        assertTrue("JSON 형식이어야 함", 
            output.trim().startsWith("[") || output.trim().startsWith("{"));
    }
    
    @Test
    public void testCsvOutput() {
        // CSV 출력 테스트
        String apacheLog = "192.168.1.1 - - [04/Oct/2023:14:14:53 +0900] " +
            "\"GET /index.html HTTP/1.1\" 200 2326";
        String[] args = {apacheLog, "--output", "csv"};
        
        CommandLine cmd = new CommandLine(new CliCommand());
        int exitCode = cmd.execute(args);
        
        assertEquals(0, exitCode);
        String output = outContent.toString();
        // CSV 헤더 확인
        assertTrue("CSV 헤더가 있어야 함", 
            output.contains("순위,포맷ID,포맷명,그룹,벤더,신뢰도"));
    }
    
    @Test
    public void testDetailOption() {
        // 상세 정보 옵션 테스트
        String apacheLog = "192.168.1.1 - - [04/Oct/2023:14:14:53 +0900] " +
            "\"GET /index.html HTTP/1.1\" 200 2326";
        String[] args = {apacheLog, "--detail"};
        
        CommandLine cmd = new CommandLine(new CliCommand());
        int exitCode = cmd.execute(args);
        
        assertEquals(0, exitCode);
        String output = outContent.toString();
        // 상세 정보 확인
        assertTrue("매칭 정보가 출력되어야 함", 
            output.contains("매칭") || output.contains("필드"));
    }
    
    @Test
    public void testFileAnalysis() throws IOException {
        // 임시 로그 파일 생성
        File tempFile = File.createTempFile("test", ".log");
        tempFile.deleteOnExit();
        
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("192.168.1.1 - - [04/Oct/2023:14:14:53 +0900] " +
                "\"GET /index.html HTTP/1.1\" 200 2326\n");
            writer.write("192.168.1.2 - - [04/Oct/2023:14:14:54 +0900] " +
                "\"POST /api/data HTTP/1.1\" 201 125\n");
        }
        
        String[] args = {tempFile.getAbsolutePath(), "--file"};
        
        CommandLine cmd = new CommandLine(new CliCommand());
        int exitCode = cmd.execute(args);
        
        assertEquals(0, exitCode);
        String output = outContent.toString();
        assertTrue("파일 분석 결과가 출력되어야 함", 
            output.contains("분석된 로그 라인"));
    }
    
    @Test
    public void testNoInput() {
        // 입력 없이 실행
        String[] args = {};
        
        CommandLine cmd = new CommandLine(new CliCommand());
        int exitCode = cmd.execute(args);
        
        assertEquals(1, exitCode);
        String error = errContent.toString();
        assertTrue("오류 메시지가 출력되어야 함", 
            error.contains("로그 입력이 필요합니다"));
    }
    
    @Test
    public void testInvalidFile() {
        // 존재하지 않는 파일
        String[] args = {"/invalid/path/to/file.log", "--file"};
        
        CommandLine cmd = new CommandLine(new CliCommand());
        int exitCode = cmd.execute(args);
        
        assertEquals(1, exitCode);
        String error = errContent.toString();
        assertTrue("파일 없음 오류가 출력되어야 함", 
            error.contains("파일을 찾을 수 없습니다"));
    }
}