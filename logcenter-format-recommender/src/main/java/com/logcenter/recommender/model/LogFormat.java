package com.logcenter.recommender.model;

import java.util.List;
import java.util.Objects;

/**
 * 로그 포맷 정보를 나타내는 모델 클래스
 * GROK-PATTERN-CONVERTER.sql 파일의 데이터 구조를 매핑
 */
public class LogFormat {
    
    private String formatId;           // 포맷 고유 ID (예: APACHE_HTTP_1.00)
    private String formatName;         // 포맷 이름 (예: APACHE_HTTP)
    private String formatVersion;      // 포맷 버전 (예: 1.00)
    private String groupName;          // 그룹 이름 (예: Web Server, Firewall)
    private String groupId;            // 그룹 ID
    private String vendor;             // 벤더명 (예: APACHE, FORTINET)
    private String model;              // 모델명 (예: HTTP, FG600C)
    private String smType;             // SM 타입 (예: logformat6)
    private List<LogType> logTypes;    // 로그 타입 목록
    
    public LogFormat() {
    }
    
    // Getters and Setters
    public String getFormatId() {
        return formatId;
    }
    
    public void setFormatId(String formatId) {
        this.formatId = formatId;
    }
    
    public String getFormatName() {
        return formatName;
    }
    
    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }
    
    public String getFormatVersion() {
        return formatVersion;
    }
    
    public void setFormatVersion(String formatVersion) {
        this.formatVersion = formatVersion;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public String getVendor() {
        return vendor;
    }
    
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getSmType() {
        return smType;
    }
    
    public void setSmType(String smType) {
        this.smType = smType;
    }
    
    public List<LogType> getLogTypes() {
        return logTypes;
    }
    
    public void setLogTypes(List<LogType> logTypes) {
        this.logTypes = logTypes;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogFormat logFormat = (LogFormat) o;
        return Objects.equals(formatId, logFormat.formatId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(formatId);
    }
    
    @Override
    public String toString() {
        return "LogFormat{" +
                "formatId='" + formatId + '\'' +
                ", formatName='" + formatName + '\'' +
                ", groupName='" + groupName + '\'' +
                ", vendor='" + vendor + '\'' +
                ", model='" + model + '\'' +
                '}';
    }
    
    /**
     * 로그 타입 정보를 나타내는 내부 클래스
     */
    public static class LogType {
        private String typeName;           // 타입 이름 (예: Event Log)
        private String typeDescription;    // 타입 설명
        private List<Pattern> patterns;    // 패턴 목록
        
        public LogType() {
        }
        
        // Getters and Setters
        public String getTypeName() {
            return typeName;
        }
        
        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }
        
        public String getTypeDescription() {
            return typeDescription;
        }
        
        public void setTypeDescription(String typeDescription) {
            this.typeDescription = typeDescription;
        }
        
        public List<Pattern> getPatterns() {
            return patterns;
        }
        
        public void setPatterns(List<Pattern> patterns) {
            this.patterns = patterns;
        }
        
        @Override
        public String toString() {
            return "LogType{" +
                    "typeName='" + typeName + '\'' +
                    ", patterns=" + (patterns != null ? patterns.size() : 0) +
                    '}';
        }
    }
    
    /**
     * 패턴 정보를 나타내는 내부 클래스
     */
    public static class Pattern {
        private String expName;       // 패턴 이름 (예: APACHE_HTTP_1.00_1)
        private String grokExp;       // Grok 표현식
        private String sampleLog;     // 샘플 로그
        private String order;         // 순서
        
        public Pattern() {
        }
        
        // Getters and Setters
        public String getExpName() {
            return expName;
        }
        
        public void setExpName(String expName) {
            this.expName = expName;
        }
        
        public String getGrokExp() {
            return grokExp;
        }
        
        public void setGrokExp(String grokExp) {
            this.grokExp = grokExp;
        }
        
        public String getSampleLog() {
            return sampleLog;
        }
        
        public void setSampleLog(String sampleLog) {
            this.sampleLog = sampleLog;
        }
        
        public String getOrder() {
            return order;
        }
        
        public void setOrder(String order) {
            this.order = order;
        }
        
        @Override
        public String toString() {
            return "Pattern{" +
                    "expName='" + expName + '\'' +
                    ", order='" + order + '\'' +
                    '}';
        }
    }
}