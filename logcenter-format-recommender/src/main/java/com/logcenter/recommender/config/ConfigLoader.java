package com.logcenter.recommender.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 설정 파일 로더 클래스
 * 다양한 위치에서 설정 파일을 읽어 Properties 객체로 반환
 */
public class ConfigLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    
    private static final String DEFAULT_CONFIG_FILE = "application.properties";
    private static final String CONFIG_DIR_PROPERTY = "config.dir";
    private static final String CONFIG_FILE_PROPERTY = "config.file";
    
    /**
     * 기본 설정 파일 로드
     */
    public static Properties loadDefaultConfig() {
        return loadConfig(DEFAULT_CONFIG_FILE);
    }
    
    /**
     * 지정된 설정 파일 로드
     */
    public static Properties loadConfig(String configFileName) {
        Properties properties = new Properties();
        
        // 1. 시스템 프로퍼티로 지정된 설정 파일 확인
        String configFile = System.getProperty(CONFIG_FILE_PROPERTY);
        if (configFile != null) {
            Properties props = loadFromFile(configFile);
            if (props != null) {
                properties.putAll(props);
                logger.info("설정 파일 로드 완료: {}", configFile);
            }
        }
        
        // 2. 시스템 프로퍼티로 지정된 디렉토리의 설정 파일 확인
        String configDir = System.getProperty(CONFIG_DIR_PROPERTY);
        if (configDir != null) {
            Properties props = loadFromFile(configDir + File.separator + configFileName);
            if (props != null) {
                properties.putAll(props);
                logger.info("설정 파일 로드 완료: {}/{}", configDir, configFileName);
            }
        }
        
        // 3. 현재 디렉토리의 설정 파일 확인
        Properties currentDirProps = loadFromFile(configFileName);
        if (currentDirProps != null) {
            properties.putAll(currentDirProps);
            logger.info("현재 디렉토리에서 설정 파일 로드 완료: {}", configFileName);
        }
        
        // 4. 사용자 홈 디렉토리의 .logcenter 폴더 확인
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            String userConfigPath = userHome + File.separator + ".logcenter" + 
                                   File.separator + configFileName;
            Properties userProps = loadFromFile(userConfigPath);
            if (userProps != null) {
                properties.putAll(userProps);
                logger.info("사용자 홈 디렉토리에서 설정 파일 로드 완료: {}", userConfigPath);
            }
        }
        
        // 5. 클래스패스의 설정 파일 확인
        Properties classpathProps = loadFromClasspath(configFileName);
        if (classpathProps != null) {
            // 클래스패스의 설정은 가장 낮은 우선순위 (기본값)
            classpathProps.putAll(properties);
            properties = classpathProps;
            logger.info("클래스패스에서 설정 파일 로드 완료: {}", configFileName);
        }
        
        // 6. 시스템 프로퍼티 오버라이드
        overrideWithSystemProperties(properties);
        
        return properties;
    }
    
    /**
     * 파일 시스템에서 설정 파일 로드
     */
    private static Properties loadFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(file)) {
            properties.load(input);
            return properties;
        } catch (IOException e) {
            logger.error("설정 파일 로드 실패: {}", filePath, e);
            return null;
        }
    }
    
    /**
     * 클래스패스에서 설정 파일 로드
     */
    private static Properties loadFromClasspath(String resourcePath) {
        Properties properties = new Properties();
        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (input == null) {
                return null;
            }
            properties.load(input);
            return properties;
        } catch (IOException e) {
            logger.error("클래스패스에서 설정 파일 로드 실패: {}", resourcePath, e);
            return null;
        }
    }
    
    /**
     * 시스템 프로퍼티로 설정값 오버라이드
     * 시스템 프로퍼티가 "app." 프리픽스로 시작하면 해당 값으로 오버라이드
     */
    private static void overrideWithSystemProperties(Properties properties) {
        Properties systemProps = System.getProperties();
        systemProps.forEach((key, value) -> {
            String keyStr = key.toString();
            if (keyStr.startsWith("app.")) {
                String configKey = keyStr.substring(4); // "app." 제거
                properties.setProperty(configKey, value.toString());
                logger.debug("시스템 프로퍼티로 오버라이드: {} = {}", configKey, value);
            }
        });
    }
    
    /**
     * Properties를 AppConfig에 적용
     */
    public static void applyToAppConfig(Properties properties) {
        AppConfig.getInstance().setProperties(properties);
        logger.info("AppConfig에 설정 적용 완료");
        
        if (logger.isDebugEnabled()) {
            logger.debug("적용된 설정값:");
            properties.forEach((key, value) -> 
                logger.debug("  {} = {}", key, value));
        }
    }
    
    /**
     * 기본 설정을 로드하고 AppConfig에 적용
     */
    public static void initializeConfig() {
        Properties properties = loadDefaultConfig();
        applyToAppConfig(properties);
    }
    
    /**
     * 설정 재로드
     */
    public static void reloadConfig() {
        logger.info("설정 재로드 시작...");
        initializeConfig();
        logger.info("설정 재로드 완료");
    }
}