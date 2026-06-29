package com.aegis.runtime.anomaly.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "runtime.anomaly")
public class AnomalyRuleConfig {

    private long highLatencyThresholdMs = 500;
    private double retryAmplificationThreshold = 1.5;
    private long threadTimeoutSeconds = 60;
    private long serviceSaturationRequestThreshold = 1000;

    public long getHighLatencyThresholdMs() {
        return highLatencyThresholdMs;
    }

    public void setHighLatencyThresholdMs(long highLatencyThresholdMs) {
        this.highLatencyThresholdMs = highLatencyThresholdMs;
    }

    public double getRetryAmplificationThreshold() {
        return retryAmplificationThreshold;
    }

    public void setRetryAmplificationThreshold(double retryAmplificationThreshold) {
        this.retryAmplificationThreshold = retryAmplificationThreshold;
    }

    public long getThreadTimeoutSeconds() {
        return threadTimeoutSeconds;
    }

    public void setThreadTimeoutSeconds(long threadTimeoutSeconds) {
        this.threadTimeoutSeconds = threadTimeoutSeconds;
    }

    public long getServiceSaturationRequestThreshold() {
        return serviceSaturationRequestThreshold;
    }

    public void setServiceSaturationRequestThreshold(long serviceSaturationRequestThreshold) {
        this.serviceSaturationRequestThreshold = serviceSaturationRequestThreshold;
    }
}
