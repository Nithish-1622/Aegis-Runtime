package com.aegis.runtime.dashboard.service;

import com.aegis.runtime.anomaly.entity.RuntimeAnomalyEntity;
import com.aegis.runtime.anomaly.model.Severity;
import com.aegis.runtime.anomaly.repository.RuntimeAnomalyRepository;
import com.aegis.runtime.dashboard.dto.RuntimeHealth;
import com.aegis.runtime.retry.RetryAnalysisService;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RuntimeHealthService {

    private final RuntimeAnomalyRepository runtimeAnomalyRepository;
    private final RuntimeEventRepository runtimeEventRepository;
    private final RetryAnalysisService retryAnalysisService;

    public RuntimeHealthService(
            RuntimeAnomalyRepository runtimeAnomalyRepository,
            RuntimeEventRepository runtimeEventRepository,
            RetryAnalysisService retryAnalysisService) {
        this.runtimeAnomalyRepository = runtimeAnomalyRepository;
        this.runtimeEventRepository = runtimeEventRepository;
        this.retryAnalysisService = retryAnalysisService;
    }

    public RuntimeHealth getHealth() {
        List<RuntimeAnomalyEntity> anomalies = runtimeAnomalyRepository.findAll();
        List<RuntimeEventEntity> events = runtimeEventRepository.findAll();
        var retryAnalysis = retryAnalysisService.analyze();

        int score = 100;
        long activeAnomalies = anomalies.size();
        Set<String> criticalServices = new HashSet<>();
        boolean highLatency = false;
        boolean retryStorm = false;
        boolean serviceSaturation = false;

        for (RuntimeAnomalyEntity anomaly : anomalies) {
            if (anomaly.getServiceName() != null && anomaly.getSeverity() == Severity.CRITICAL) {
                criticalServices.add(anomaly.getServiceName());
            }
            if (anomaly.getSeverity() == Severity.CRITICAL) {
                score -= 20;
            } else if (anomaly.getSeverity() == Severity.HIGH) {
                score -= 10;
            }
            if (anomaly.getAnomalyType() != null
                    && anomaly.getAnomalyType().name().contains("LATENCY")
                    && anomaly.getSeverity().ordinal() >= Severity.HIGH.ordinal()) {
                highLatency = true;
            }
            if (anomaly.getAnomalyType() != null && anomaly.getAnomalyType().name().contains("RETRY")) {
                retryStorm = true;
            }
            if (anomaly.getAnomalyType() != null && anomaly.getAnomalyType().name().contains("SATURATION")) {
                serviceSaturation = true;
            }
        }

        if (!highLatency && events.stream().anyMatch(event -> event.getLatency() != null && event.getLatency().toMillis() > 250)) {
            highLatency = true;
        }
        if (!retryStorm && (retryAnalysis.totalRetries() >= 20 || retryAnalysis.retryAmplificationFactor() >= 1.5)) {
            retryStorm = true;
        }
        if (!serviceSaturation && activeAnomalies > 0) {
            serviceSaturation = false;
        }

        if (retryStorm) {
            score -= 10;
        }
        if (serviceSaturation) {
            score -= 15;
        }
        if (highLatency) {
            score -= 5;
        }

        score = Math.max(0, Math.min(100, score));
        String status = statusFor(score);
        String summary = buildSummary(status, activeAnomalies, highLatency, retryStorm, serviceSaturation, criticalServices.size());

        return new RuntimeHealth(status, score, activeAnomalies, List.copyOf(new ArrayList<>(criticalServices)), summary);
    }

    private String statusFor(int score) {
        if (score >= 90) {
            return "HEALTHY";
        }
        if (score >= 70) {
            return "DEGRADED";
        }
        if (score >= 40) {
            return "UNSTABLE";
        }
        return "CRITICAL";
    }

    private String buildSummary(
            String status,
            long activeAnomalies,
            boolean highLatency,
            boolean retryStorm,
            boolean serviceSaturation,
            int criticalServiceCount) {
        if (activeAnomalies == 0) {
            return "Overall runtime is healthy.";
        }

        List<String> parts = new ArrayList<>();
        if (highLatency) {
            parts.add("latency anomaly");
        }
        if (retryStorm) {
            parts.add("retry storm");
        }
        if (serviceSaturation) {
            parts.add("service saturation");
        }
        if (parts.isEmpty()) {
            parts.add("active anomaly");
        }

        String detail = String.join(" and ", parts);
        if (criticalServiceCount > 0) {
            return "Overall runtime is " + status.toLowerCase() + " with " + criticalServiceCount + " critical service(s) affected by " + detail + ".";
        }
        return "Overall runtime is " + status.toLowerCase() + " with " + detail + ".";
    }
}
