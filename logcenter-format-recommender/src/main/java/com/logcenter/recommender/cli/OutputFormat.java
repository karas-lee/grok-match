package com.logcenter.recommender.cli;

/**
 * 출력 형식 열거형
 */
public enum OutputFormat {
    TEXT("text"),
    JSON("json"),
    CSV("csv");
    
    private final String value;
    
    OutputFormat(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}