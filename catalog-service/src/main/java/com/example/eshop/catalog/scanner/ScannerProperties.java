package com.example.eshop.catalog.scanner;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scanner")
public class ScannerProperties {
    private String aiInferenceUrl = "http://localhost:8000";
    private int aiTimeoutSeconds = 8;
    private long duplicateWindowMs = 1500;
    private int rateLimit = 30;
    private int rateWindowSeconds = 10;

    public String getAiInferenceUrl() { return aiInferenceUrl; }
    public void setAiInferenceUrl(String aiInferenceUrl) { this.aiInferenceUrl = aiInferenceUrl; }
    public int getAiTimeoutSeconds() { return aiTimeoutSeconds; }
    public void setAiTimeoutSeconds(int aiTimeoutSeconds) { this.aiTimeoutSeconds = aiTimeoutSeconds; }
    public long getDuplicateWindowMs() { return duplicateWindowMs; }
    public void setDuplicateWindowMs(long duplicateWindowMs) { this.duplicateWindowMs = duplicateWindowMs; }
    public int getRateLimit() { return rateLimit; }
    public void setRateLimit(int rateLimit) { this.rateLimit = rateLimit; }
    public int getRateWindowSeconds() { return rateWindowSeconds; }
    public void setRateWindowSeconds(int rateWindowSeconds) { this.rateWindowSeconds = rateWindowSeconds; }
}
