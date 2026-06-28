package com.aegis.runtime.analytics.service;

import com.aegis.runtime.analytics.dto.LatencyMetrics;
import com.aegis.runtime.analytics.dto.RuntimeMetrics;
import com.aegis.runtime.contracts.EventStatus;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RuntimeMetricsService {

    private final RuntimeEventRepository runtimeEventRepository;
    private final LatencyCalculator latencyCalculator;

    public RuntimeMetricsService(
            RuntimeEventRepository runtimeEventRepository,
            LatencyCalculator latencyCalculator) {
        this.runtimeEventRepository = runtimeEventRepository;
        this.latencyCalculator = latencyCalculator;
    }

    public RuntimeMetrics getRuntimeMetrics() {
        List<RuntimeEventEntity> events = runtimeEventRepository.findAll();
        List<Long> latencies = events.stream()
                .map(RuntimeEventEntity::getLatency)
                .filter(java.util.Objects::nonNull)
                .map(Duration::toMillis)
                .toList();

        LatencyMetrics latencyMetrics = latencyCalculator.calculate(latencies);
        long totalRequests = events.size();
        long successfulRequests = events.stream()
                .filter(event -> event.getStatus() == EventStatus.SUCCESS)
                .count();
        long failedRequests = events.stream()
                .filter(event -> event.getStatus() == EventStatus.FAILED)
                .count();

        return new RuntimeMetrics(
                totalRequests,
                successfulRequests,
                failedRequests,
                latencyMetrics.averageLatency(),
                latencyMetrics.minLatency(),
                latencyMetrics.maxLatency(),
                latencyMetrics.p50Latency(),
                latencyMetrics.p95Latency(),
                latencyMetrics.p99Latency());
    }
}
