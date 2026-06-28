package com.aegis.runtime.analytics.dto;

public record ServiceLatency(
        String serviceName,
        long requestCount,
        double averageLatency,
        double minLatency,
        double maxLatency,
        double p50Latency,
        double p95Latency,
        double p99Latency,
        double successRate,
        double failureRate) {
}
