package com.logcenter.recommender.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.logcenter.recommender.service.LogFormatRecommender;

import java.util.List;

/**
 * 로그 포맷 추천 API 요청 모델
 */
public class LogFormatRequest {
    
    @JsonProperty("log_samples")
    private List<String> logSamples;
    
    @JsonProperty("group_filter")
    private String groupFilter;
    
    @JsonProperty("vendor_filter")
    private String vendorFilter;
    
    @JsonProperty("top_n")
    private int topN = 10;
    
    @JsonProperty("include_metadata")
    private boolean includeMetadata = false;
    
    @JsonProperty("parallel_processing")
    private boolean parallelProcessing = true;
    
    public LogFormatRequest() {
    }
    
    public LogFormatRequest(List<String> logSamples) {
        this.logSamples = logSamples;
    }
    
    /**
     * RecommendOptions로 변환
     */
    public LogFormatRecommender.RecommendOptions toRecommendOptions() {
        LogFormatRecommender.RecommendOptions options = new LogFormatRecommender.RecommendOptions();
        options.setMaxResults(topN);
        options.setGroupFilter(groupFilter);
        options.setVendorFilter(vendorFilter);
        options.setParallelProcessing(parallelProcessing);
        return options;
    }
    
    // Getters and Setters
    public List<String> getLogSamples() {
        return logSamples;
    }
    
    public void setLogSamples(List<String> logSamples) {
        this.logSamples = logSamples;
    }
    
    public String getGroupFilter() {
        return groupFilter;
    }
    
    public void setGroupFilter(String groupFilter) {
        this.groupFilter = groupFilter;
    }
    
    public String getVendorFilter() {
        return vendorFilter;
    }
    
    public void setVendorFilter(String vendorFilter) {
        this.vendorFilter = vendorFilter;
    }
    
    public int getTopN() {
        return topN;
    }
    
    public void setTopN(int topN) {
        this.topN = topN;
    }
    
    public boolean isIncludeMetadata() {
        return includeMetadata;
    }
    
    public void setIncludeMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }
    
    public boolean isParallelProcessing() {
        return parallelProcessing;
    }
    
    public void setParallelProcessing(boolean parallelProcessing) {
        this.parallelProcessing = parallelProcessing;
    }
}