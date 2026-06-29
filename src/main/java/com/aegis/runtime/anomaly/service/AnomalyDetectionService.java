package com.aegis.runtime.anomaly.service;

import com.aegis.runtime.analytics.dto.ServiceLatency;
import com.aegis.runtime.analytics.service.RuntimeMetricsService;
import com.aegis.runtime.analytics.service.ServiceLatencyService;
import com.aegis.runtime.anomaly.config.AnomalyRuleConfig;
import com.aegis.runtime.anomaly.dto.RuntimeAnomaly;
import com.aegis.runtime.anomaly.model.AnomalyType;
import com.aegis.runtime.anomaly.model.Severity;
import com.aegis.runtime.contracts.EventStatus;
import com.aegis.runtime.contracts.RuntimeEvent;
import com.aegis.runtime.retry.RetryAnalysis;
import com.aegis.runtime.retry.RetryAnalysisService;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import com.aegis.runtime.topology.DependencyDiscoveryService;
import com.aegis.runtime.topology.ServiceTopology;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AnomalyDetectionService {

    private final RuntimeEventRepository runtimeEventRepository;
    private final RuntimeMetricsService runtimeMetricsService;
    private final ServiceLatencyService serviceLatencyService;
    private final RetryAnalysisService retryAnalysisService;
    private final DependencyDiscoveryService dependencyDiscoveryService;
    private final AnomalyRuleConfig anomalyRuleConfig;

    public AnomalyDetectionService(
            RuntimeEventRepository runtimeEventRepository,
            RuntimeMetricsService runtimeMetricsService,
            ServiceLatencyService serviceLatencyService,
            RetryAnalysisService retryAnalysisService,
            DependencyDiscoveryService dependencyDiscoveryService,
            AnomalyRuleConfig anomalyRuleConfig) {
        this.runtimeEventRepository = runtimeEventRepository;
        this.runtimeMetricsService = runtimeMetricsService;
        this.serviceLatencyService = serviceLatencyService;
        this.retryAnalysisService = retryAnalysisService;
        this.dependencyDiscoveryService = dependencyDiscoveryService;
        this.anomalyRuleConfig = anomalyRuleConfig;
    }

    public List<RuntimeAnomaly> detect() {
        List<RuntimeEventEntity> events = runtimeEventRepository.findAll();
        if (events.isEmpty()) {
            return List.of();
        }

        ServiceTopology topology = dependencyDiscoveryService.discover();
        RetryAnalysis retryAnalysis = retryAnalysisService.analyze();
        var runtimeMetrics = runtimeMetricsService.getRuntimeMetrics();
        Map<String, ServiceLatency> serviceLatencyMap = buildServiceLatencyMap(topology);

        List<RuntimeAnomaly> anomalies = new ArrayList<>();
        anomalies.addAll(detectHighLatency(events, serviceLatencyMap));
        anomalies.addAll(detectRetryStorm(retryAnalysis));
        anomalies.addAll(detectThreadStarvation(events));
        anomalies.addAll(detectServiceSaturation(serviceLatencyMap));
        return anomalies;
    }

    private Map<String, ServiceLatency> buildServiceLatencyMap(ServiceTopology topology) {
        Map<String, ServiceLatency> serviceLatencyMap = new LinkedHashMap<>();
        if (topology == null || topology.services() == null) {
            return serviceLatencyMap;
        }

        for (var serviceNode : topology.services()) {
            ServiceLatency latency = serviceLatencyService.getMetrics(serviceNode.serviceName());
            if (latency != null) {
                serviceLatencyMap.put(serviceNode.serviceName(), latency);
            }
        }
        return serviceLatencyMap;
    }

    private List<RuntimeAnomaly> detectHighLatency(
            List<RuntimeEventEntity> events,
            Map<String, ServiceLatency> serviceLatencyMap) {
        List<RuntimeAnomaly> anomalies = new ArrayList<>();
        long threshold = anomalyRuleConfig.getHighLatencyThresholdMs();
        for (Map.Entry<String, ServiceLatency> entry : serviceLatencyMap.entrySet()) {
            if (entry.getValue().averageLatency() > threshold) {
                anomalies.add(buildAnomaly(
                        null,
                        entry.getKey(),
                        AnomalyType.HIGH_LATENCY,
                        Severity.MEDIUM,
                        "Observed latency exceeded configured threshold.",
                        Map.of(
                                "observedLatency", entry.getValue().averageLatency(),
                                "threshold", threshold)));
            }
        }
        return anomalies;
    }

    private List<RuntimeAnomaly> detectRetryStorm(RetryAnalysis retryAnalysis) {
        if (retryAnalysis == null || retryAnalysis.retryAmplificationFactor() <= anomalyRuleConfig.getRetryAmplificationThreshold()) {
            return List.of();
        }

        return List.of(buildAnomaly(
                null,
                retryAnalysis.servicesWithRetries().isEmpty() ? null : retryAnalysis.servicesWithRetries().getFirst(),
                AnomalyType.RETRY_STORM,
                Severity.HIGH,
                "Retry amplification exceeded configured threshold.",
                Map.of(
                        "retryCount", retryAnalysis.totalRetries(),
                        "amplificationFactor", retryAnalysis.retryAmplificationFactor(),
                        "threshold", anomalyRuleConfig.getRetryAmplificationThreshold())));
    }

    private List<RuntimeAnomaly> detectThreadStarvation(List<RuntimeEventEntity> events) {
        List<RuntimeAnomaly> anomalies = new ArrayList<>();
        Instant cutoff = Instant.now().minus(Duration.ofSeconds(anomalyRuleConfig.getThreadTimeoutSeconds()));
        for (RuntimeEventEntity event : events) {
            if (event.getStatus() == EventStatus.IN_PROGRESS
                    && event.getTimestamp() != null
                    && event.getTimestamp().isBefore(cutoff)) {
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("threadName", event.getThreadName());
                metadata.put("threadId", event.getThreadId());
                metadata.put("waitingDuration", Duration.between(event.getTimestamp(), Instant.now()).toMillis());
                metadata.put("thresholdSeconds", anomalyRuleConfig.getThreadTimeoutSeconds());
                anomalies.add(buildAnomaly(
                        event.getTraceId(),
                        event.getServiceName(),
                        AnomalyType.THREAD_STARVATION,
                        Severity.HIGH,
                        "Long-running in-progress event detected.",
                        metadata));
            }
        }
        return anomalies;
    }

    private List<RuntimeAnomaly> detectServiceSaturation(Map<String, ServiceLatency> serviceLatencyMap) {
        List<RuntimeAnomaly> anomalies = new ArrayList<>();
        long threshold = anomalyRuleConfig.getServiceSaturationRequestThreshold();
        for (Map.Entry<String, ServiceLatency> entry : serviceLatencyMap.entrySet()) {
            if (entry.getValue().requestCount() >= threshold && entry.getValue().averageLatency() > anomalyRuleConfig.getHighLatencyThresholdMs()) {
                anomalies.add(buildAnomaly(
                        null,
                        entry.getKey(),
                        AnomalyType.SERVICE_SATURATION,
                        Severity.CRITICAL,
                        "Average latency and request volume exceeded configured thresholds.",
                        Map.of(
                                "averageLatency", entry.getValue().averageLatency(),
                                "requestCount", entry.getValue().requestCount(),
                                "threshold", threshold)));
            }
        }
        return anomalies;
    }

    private RuntimeAnomaly buildAnomaly(
            String traceId,
            String serviceName,
            AnomalyType type,
            Severity severity,
            String description,
            Map<String, Object> metadata) {
        Map<String, Object> safeMetadata = new LinkedHashMap<>();
        if (metadata != null) {
            safeMetadata.putAll(metadata);
        }
        safeMetadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new RuntimeAnomaly(
                UUID.randomUUID(),
                traceId,
                serviceName,
                type,
                severity,
                description,
                Instant.now(),
                Map.copyOf(safeMetadata));
    }
}
