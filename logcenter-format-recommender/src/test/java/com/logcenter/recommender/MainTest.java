package com.logcenter.recommender;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.Permission;

/**
 * Main 클래스 테스트
 */
public class MainTest {
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private static class NoExitSecurityManager extends SecurityManager {
        @Override
        public void checkPermission(Permission perm) {
            // 권한 체크 허용
        }
        
        @Override
        public void checkExit(int status) {
            throw new SecurityException("System.exit(" + status + ") 호출됨");
        }
    }
    
    @Before
    public void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        System.setSecurityManager(new NoExitSecurityManager());
    }
    
    @After
    public void tearDown() {
        System.setSecurityManager(null);
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Test
    public void testMainWithHelp() {
        try {
            Main.main(new String[]{"--help"});
        } catch (SecurityException e) {
            // System.exit가 호출되면 SecurityException 발생
            assertTrue("exit(0)이 호출되어야 함", 
                e.getMessage().contains("System.exit(0)"));
        }
        
        String output = outContent.toString();
        assertTrue("도움말이 출력되어야 함", 
            output.contains("SIEM 로그 포맷 추천 도구"));
    }
    
    @Test
    public void testMainWithVersion() {
        try {
            Main.main(new String[]{"--version"});
        } catch (SecurityException e) {
            assertTrue("exit(0)이 호출되어야 함", 
                e.getMessage().contains("System.exit(0)"));
        }
        
        String output = outContent.toString();
        assertTrue("버전 정보가 출력되어야 함", 
            output.contains("1.0.0"));
    }
    
    @Test
    public void testMainWithInvalidArgs() {
        try {
            Main.main(new String[]{"--invalid-option"});
        } catch (SecurityException e) {
            // 잘못된 옵션은 exit(2) 호출
            assertTrue("exit(2)가 호출되어야 함", 
                e.getMessage().contains("System.exit(2)"));
        }
    }
    
    @Test
    public void testExitMethod() {
        // 정상 종료 테스트
        try {
            Main.exit("Success", 0);
        } catch (SecurityException e) {
            assertTrue(e.getMessage().contains("System.exit(0)"));
        }
        
        // 오류 종료 테스트
        try {
            Main.exit("Error", 1);
        } catch (SecurityException e) {
            assertTrue(e.getMessage().contains("System.exit(1)"));
        }
    }
    
    @Test
    public void testSystemConfiguration() {
        // 시스템 설정 테스트
        String originalEncoding = System.getProperty("file.encoding");
        String originalHeadless = System.getProperty("java.awt.headless");
        
        try {
            Main.main(new String[]{"--help"});
        } catch (SecurityException e) {
            // 예상된 동작
        }
        
        // 시스템 속성이 설정되었는지 확인
        assertEquals("UTF-8", System.getProperty("file.encoding"));
        assertEquals("true", System.getProperty("java.awt.headless"));
        
        // 원래 값으로 복원
        if (originalEncoding != null) {
            System.setProperty("file.encoding", originalEncoding);
        }
        if (originalHeadless != null) {
            System.setProperty("java.awt.headless", originalHeadless);
        }
    }
}