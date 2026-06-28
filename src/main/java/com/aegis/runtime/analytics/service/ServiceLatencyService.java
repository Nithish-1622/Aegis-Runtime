package com.aegis.runtime.analytics.service;

import com.aegis.runtime.analytics.dto.LatencyMetrics;
import com.aegis.runtime.analytics.dto.ServiceLatency;
import com.aegis.runtime.contracts.EventStatus;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ServiceLatencyService {

    private final RuntimeEventRepository runtimeEventRepository;
    private final LatencyCalculator latencyCalculator;

    public ServiceLatencyService(
            RuntimeEventRepository runtimeEventRepository,
            LatencyCalculator latencyCalculator) {
        this.runtimeEventRepository = runtimeEventRepository;
        this.latencyCalculator = latencyCalculator;
    }

    public ServiceLatency getMetrics(String serviceName) {
        List<RuntimeEventEntity> events = runtimeEventRepository.findByServiceName(serviceName);
        if (events.isEmpty()) {
            return null;
        }

        List<Long> latencies = events.stream()
                .map(RuntimeEventEntity::getLatency)
                .filter(java.util.Objects::nonNull)
                .map(Duration::toMillis)
                .toList();

        LatencyMetrics latencyMetrics = latencyCalculator.calculate(latencies);
        long requestCount = events.size();
        long successfulRequests = events.stream()
                .filter(event -> event.getStatus() == EventStatus.SUCCESS)
                .count();
        long failedRequests = events.stream()
                .filter(event -> event.getStatus() == EventStatus.FAILED)
                .count();

        double successRate = requestCount == 0 ? 0 : (successfulRequests * 100.0) / requestCount;
        double failureRate = requestCount == 0 ? 0 : (failedRequests * 100.0) / requestCount;

        return new ServiceLatency(
                serviceName,
                requestCount,
                latencyMetrics.averageLatency(),
                latencyMetrics.minLatency(),
                latencyMetrics.maxLatency(),
                latencyMetrics.p50Latency(),
                latencyMetrics.p95Latency(),
                latencyMetrics.p99Latency(),
                successRate,
                failureRate);
    }
}
