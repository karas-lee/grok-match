package com.logcenter.recommender.model;

import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;

import java.util.Objects;

/**
 * Grok 패턴 정보를 나타내는 모델 클래스
 * 커스텀 패턴과 표준 패턴을 포함
 */
public class GrokPattern {
    
    private String name;              // 패턴 이름 (예: SRC_IP, DATE_FORMAT1)
    private String pattern;           // 패턴 정규식
    private String type;              // 패턴 타입 (CUSTOM, STANDARD)
    private String category;          // 패턴 카테고리 (IP, PORT, DATE, TEXT 등)
    private Grok compiledGrok;        // 컴파일된 Grok 객체 (캐싱용)
    private boolean isCompiled;       // 컴파일 여부
    
    public GrokPattern() {
        this.isCompiled = false;
    }
    
    public GrokPattern(String name, String pattern) {
        this.name = name;
        this.pattern = pattern;
        this.type = "CUSTOM";
        this.isCompiled = false;
    }
    
    public GrokPattern(String name, String pattern, String type) {
        this.name = name;
        this.pattern = pattern;
        this.type = type;
        this.isCompiled = false;
    }
    
    /**
     * 패턴을 컴파일하고 캐싱
     * @param compiler Grok 컴파일러
     * @return 컴파일 성공 여부
     */
    public boolean compile(GrokCompiler compiler) {
        try {
            compiler.register(name, pattern);
            this.compiledGrok = compiler.compile("%{" + name + "}");
            this.isCompiled = true;
            return true;
        } catch (Exception e) {
            this.isCompiled = false;
            return false;
        }
    }
    
    /**
     * 카테고리 자동 분류
     */
    public void categorize() {
        if (name == null) {
            this.category = "UNKNOWN";
            return;
        }
        
        String upperName = name.toUpperCase();
        
        if (upperName.contains("IP") || upperName.equals("HOST_IP") || 
            upperName.equals("DEVICE_IP")) {
            this.category = "IP";
        } else if (upperName.contains("PORT")) {
            this.category = "PORT";
        } else if (upperName.contains("MAC")) {
            this.category = "MAC";
        } else if (upperName.contains("DATE") || upperName.contains("TIME") || 
                   upperName.equals("LOG_TIME")) {
            this.category = "DATE_TIME";
        } else if (upperName.startsWith("TEXT")) {
            this.category = "TEXT";
        } else if (upperName.equals("MAIL") || upperName.equals("SENDER") || 
                   upperName.equals("RECEIVER")) {
            this.category = "EMAIL";
        } else if (upperName.startsWith("RESERVED")) {
            this.category = "RESERVED";
        } else if (upperName.startsWith("CISCO")) {
            this.category = "VENDOR_SPECIFIC";
        } else if (upperName.equals("SKIP") || upperName.contains("SKIP")) {
            this.category = "UTILITY";
        } else if (upperName.equals("COUNT") || upperName.equals("PID") || 
                   upperName.equals("FILE_SIZE") || upperName.endsWith("_SIZE") || 
                   upperName.endsWith("_PKT") || upperName.endsWith("_COUNT")) {
            this.category = "NUMBER";
        } else {
            this.category = "FIELD";
        }
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.isCompiled = false; // 패턴이 변경되면 재컴파일 필요
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Grok getCompiledGrok() {
        return compiledGrok;
    }
    
    public boolean isCompiled() {
        return isCompiled;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GrokPattern that = (GrokPattern) o;
        return Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public String toString() {
        return "GrokPattern{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", category='" + category + '\'' +
                ", isCompiled=" + isCompiled +
                '}';
    }
}