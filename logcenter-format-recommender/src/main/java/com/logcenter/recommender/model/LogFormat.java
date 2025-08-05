package com.logcenter.recommender.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * 로그 포맷 정보를 나타내는 모델 클래스
 * GROK-PATTERN-CONVERTER.sql 파일의 데이터 구조를 매핑
 */
public class LogFormat {
    
    @JsonProperty("format_id")
    private String formatId;           // 포맷 고유 ID (예: APACHE_HTTP_1.00)
    @JsonProperty("format_name")
    private String formatName;         // 포맷 이름 (예: APACHE_HTTP)
    @JsonProperty("format_version")
    private String formatVersion;      // 포맷 버전 (예: 1.00)
    @JsonProperty("group_name")
    private String groupName;          // 그룹 이름 (예: Web Server, Firewall)
    @JsonProperty("group_id")
    private String groupId;            // 그룹 ID
    private String vendor;             // 벤더명 (예: APACHE, FORTINET)
    private String model;              // 모델명 (예: HTTP, FG600C)
    @JsonProperty("sm_type")
    private String smType;             // SM 타입 (예: logformat6)
    @JsonProperty("log_type")
    private List<LogType> logTypes;    // 로그 타입 목록
    private String grokPattern;        // 대표 Grok 패턴
    private List<String> requiredFields; // 필수 필드 목록
    
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
    
    public String getGrokPattern() {
        return grokPattern;
    }
    
    public void setGrokPattern(String grokPattern) {
        this.grokPattern = grokPattern;
    }
    
    public List<String> getRequiredFields() {
        return requiredFields;
    }
    
    public void setRequiredFields(List<String> requiredFields) {
        this.requiredFields = requiredFields;
    }
    
    /**
     * 그룹명을 반환 (getFormatGroup 별칭)
     */
    public String getFormatGroup() {
        return groupName;
    }
    
    /**
     * 그룹명을 반환 (getGroup 별칭)
     */
    public String getGroup() {
        return groupName;
    }
    
    /**
     * 첫 번째 로그 타입의 첫 번째 패턴을 대표 Grok 패턴으로 반환
     */
    public String getPrimaryGrokPattern() {
        if (grokPattern != null && !grokPattern.isEmpty()) {
            return grokPattern;
        }
        
        if (logTypes != null && !logTypes.isEmpty()) {
            LogType firstType = logTypes.get(0);
            if (firstType.getPatterns() != null && !firstType.getPatterns().isEmpty()) {
                return firstType.getPatterns().get(0).getGrokExp();
            }
        }
        return null;
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
        @JsonProperty("type")
        private String typeName;           // 타입 이름 (예: Event Log)
        @JsonProperty("type_description")
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
        @JsonProperty("exp_name")
        private String expName;       // 패턴 이름 (예: APACHE_HTTP_1.00_1)
        @JsonProperty("grok_exp")
        private String grokExp;       // Grok 표현식
        @JsonProperty("samplelog")
        private String sampleLog;     // 샘플 로그
        private String order;         // 순서
        @JsonProperty("data_table")
        private List<DataTable> dataTable;  // 데이터 테이블 (필드 정보)
        
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
        
        public List<DataTable> getDataTable() {
            return dataTable;
        }
        
        public void setDataTable(List<DataTable> dataTable) {
            this.dataTable = dataTable;
        }
        
        @Override
        public String toString() {
            return "Pattern{" +
                    "expName='" + expName + '\'' +
                    ", order='" + order + '\'' +
                    '}';
        }
    }
    
    /**
     * 데이터 테이블 항목을 나타내는 내부 클래스
     */
    public static class DataTable {
        private String explanation;   // 필드 설명
        private Object pattern;       // 패턴 (O: 필수, X: 선택, 또는 숫자)
        private String value;         // 샘플 값
        @JsonProperty("field_name")
        private String fieldName;     // 필드 이름
        
        public DataTable() {
        }
        
        // Getters and Setters
        public String getExplanation() {
            return explanation;
        }
        
        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
        
        public String getPattern() {
            return pattern != null ? pattern.toString() : null;
        }
        
        public void setPattern(Object pattern) {
            this.pattern = pattern;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public String getFieldName() {
            return fieldName;
        }
        
        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }
        
        @Override
        public String toString() {
            return "DataTable{" +
                    "fieldName='" + fieldName + '\'' +
                    ", pattern='" + pattern + '\'' +
                    '}';
        }
    }
}