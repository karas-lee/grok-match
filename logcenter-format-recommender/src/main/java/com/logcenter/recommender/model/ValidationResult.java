package com.logcenter.recommender.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 로그 포맷 검증 결과를 나타내는 모델 클래스
 */
public class ValidationResult {
    
    public enum Status {
        PASS("통과"),
        FAIL("실패"),
        WARNING("경고"),
        SKIPPED("건너뜀");
        
        private final String description;
        
        Status(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private String formatId;
    private String formatName;
    private String expName;
    private Status status;
    private List<String> errorMessages;
    private List<String> warningMessages;
    private Map<String, Object> extractedFields;
    private String grokExpression;
    private String sampleLog;
    private long validationTime;
    private String groupName;
    private String vendor;
    
    public ValidationResult() {
        this.errorMessages = new ArrayList<>();
        this.warningMessages = new ArrayList<>();
        this.extractedFields = new HashMap<>();
        this.status = Status.SKIPPED;
    }
    
    public ValidationResult(String formatId, String formatName) {
        this();
        this.formatId = formatId;
        this.formatName = formatName;
    }
    
    public void addError(String message) {
        errorMessages.add(message);
        if (status != Status.FAIL) {
            status = Status.FAIL;
        }
    }
    
    public void addWarning(String message) {
        warningMessages.add(message);
        if (status == Status.PASS || status == Status.SKIPPED) {
            status = Status.WARNING;
        }
    }
    
    public boolean hasErrors() {
        return !errorMessages.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warningMessages.isEmpty();
    }
    
    public boolean isSuccess() {
        return status == Status.PASS || status == Status.WARNING;
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
    
    public String getExpName() {
        return expName;
    }
    
    public void setExpName(String expName) {
        this.expName = expName;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public List<String> getErrorMessages() {
        return errorMessages;
    }
    
    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }
    
    public List<String> getWarningMessages() {
        return warningMessages;
    }
    
    public void setWarningMessages(List<String> warningMessages) {
        this.warningMessages = warningMessages;
    }
    
    public Map<String, Object> getExtractedFields() {
        return extractedFields;
    }
    
    public void setExtractedFields(Map<String, Object> extractedFields) {
        this.extractedFields = extractedFields;
    }
    
    public String getGrokExpression() {
        return grokExpression;
    }
    
    public void setGrokExpression(String grokExpression) {
        this.grokExpression = grokExpression;
    }
    
    public String getSampleLog() {
        return sampleLog;
    }
    
    public void setSampleLog(String sampleLog) {
        this.sampleLog = sampleLog;
    }
    
    public long getValidationTime() {
        return validationTime;
    }
    
    public void setValidationTime(long validationTime) {
        this.validationTime = validationTime;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getVendor() {
        return vendor;
    }
    
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
    
    @Override
    public String toString() {
        return String.format("ValidationResult{formatId='%s', status=%s, errors=%d, warnings=%d}",
                formatId, status, errorMessages.size(), warningMessages.size());
    }
}