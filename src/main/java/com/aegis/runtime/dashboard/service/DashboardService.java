package com.aegis.runtime.dashboard.service;

import com.aegis.runtime.analytics.dto.RuntimeMetrics;
import com.aegis.runtime.analytics.service.RuntimeMetricsService;
import com.aegis.runtime.anomaly.entity.RuntimeAnomalyEntity;
import com.aegis.runtime.anomaly.model.Severity;
import com.aegis.runtime.anomaly.repository.RuntimeAnomalyRepository;
import com.aegis.runtime.dashboard.dto.DashboardSummary;
import com.aegis.runtime.retry.RetryAnalysis;
import com.aegis.runtime.retry.RetryAnalysisService;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import com.aegis.runtime.topology.DependencyDiscoveryService;
import com.aegis.runtime.topology.ServiceTopology;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final RuntimeMetricsService runtimeMetricsService;
    private final RetryAnalysisService retryAnalysisService;
    private final RuntimeAnomalyRepository runtimeAnomalyRepository;
    private final RuntimeEventRepository runtimeEventRepository;
    private final DependencyDiscoveryService dependencyDiscoveryService;

    public DashboardService(
            RuntimeMetricsService runtimeMetricsService,
            RetryAnalysisService retryAnalysisService,
            RuntimeAnomalyRepository runtimeAnomalyRepository,
            RuntimeEventRepository runtimeEventRepository,
            DependencyDiscoveryService dependencyDiscoveryService) {
        this.runtimeMetricsService = runtimeMetricsService;
        this.retryAnalysisService = retryAnalysisService;
        this.runtimeAnomalyRepository = runtimeAnomalyRepository;
        this.runtimeEventRepository = runtimeEventRepository;
        this.dependencyDiscoveryService = dependencyDiscoveryService;
    }

    public DashboardSummary getSummary() {
        RuntimeMetrics metrics = runtimeMetricsService.getRuntimeMetrics();
        RetryAnalysis retryAnalysis = retryAnalysisService.analyze();
        List<RuntimeAnomalyEntity> anomalies = runtimeAnomalyRepository.findAll();
        List<RuntimeEventEntity> events = runtimeEventRepository.findAll();
        ServiceTopology topology = dependencyDiscoveryService.discover();

        Set<String> traces = new HashSet<>();
        Set<String> services = new HashSet<>();
        for (RuntimeEventEntity event : events) {
            if (event.getTraceId() != null && !event.getTraceId().isBlank()) {
                traces.add(event.getTraceId());
            }
            if (event.getServiceName() != null && !event.getServiceName().isBlank()) {
                services.add(event.getServiceName());
            }
        }

        long criticalAnomalies = anomalies.stream().filter(anomaly -> anomaly.getSeverity() == Severity.CRITICAL).count();
        long highAnomalies = anomalies.stream().filter(anomaly -> anomaly.getSeverity() == Severity.HIGH).count();
        Set<String> unhealthyServices = new HashSet<>();
        for (RuntimeAnomalyEntity anomaly : anomalies) {
            if (anomaly.getServiceName() != null && !anomaly.getServiceName().isBlank()
                    && (anomaly.getSeverity() == Severity.CRITICAL || anomaly.getSeverity() == Severity.HIGH)) {
                unhealthyServices.add(anomaly.getServiceName());
            }
        }

        long totalServices = topology.services().size();
        long healthyServices = Math.max(0, totalServices - unhealthyServices.size());

        return new DashboardSummary(
                metrics.totalRequests(),
                metrics.averageLatency(),
                metrics.p95Latency(),
                metrics.p99Latency(),
                totalServices,
                traces.size(),
                retryAnalysis.totalRetries(),
                retryAnalysis.retryAmplificationFactor(),
                anomalies.size(),
                criticalAnomalies,
                highAnomalies,
                healthyServices,
                unhealthyServices.size());
    }
}
