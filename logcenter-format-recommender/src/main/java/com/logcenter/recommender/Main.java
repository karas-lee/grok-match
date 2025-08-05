package com.logcenter.recommender;

import com.logcenter.recommender.cli.CliCommand;
import com.logcenter.recommender.config.ConfigLoader;
import picocli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LogCenter Format Recommender 메인 클래스
 * CLI 애플리케이션의 진입점
 */
public class Main {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    /**
     * 애플리케이션 진입점
     * @param args 명령행 인자
     */
    public static void main(String[] args) {
        // 시스템 속성 설정 (로깅 레벨 등)
        configureSystem();
        
        // 설정 파일 로드
        ConfigLoader.initializeConfig();
        
        // CLI 명령 실행
        int exitCode = new CommandLine(new CliCommand())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);
        
        // 종료 코드에 따른 종료
        System.exit(exitCode);
    }
    
    /**
     * 시스템 속성 구성
     */
    private static void configureSystem() {
        // 기본 로깅 레벨 설정 (환경 변수로 오버라이드 가능)
        if (System.getProperty("logback.configurationFile") == null) {
            String logLevel = System.getenv("LOG_LEVEL");
            if (logLevel != null) {
                System.setProperty("logging.level.root", logLevel);
            }
        }
        
        // 파일 인코딩 설정
        System.setProperty("file.encoding", "UTF-8");
        
        // Java 1.8 호환성을 위한 설정
        System.setProperty("java.awt.headless", "true");
        
        logger.debug("시스템 구성 완료");
    }
    
    /**
     * 예외 처리를 위한 종료 메서드
     * @param message 오류 메시지
     * @param exitCode 종료 코드
     */
    public static void exit(String message, int exitCode) {
        if (exitCode == 0) {
            logger.info(message);
        } else {
            logger.error(message);
        }
        System.exit(exitCode);
    }
}