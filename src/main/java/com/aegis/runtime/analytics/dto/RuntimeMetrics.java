package com.aegis.runtime.analytics.dto;

public record RuntimeMetrics(
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        double averageLatency,
        double minLatency,
        double maxLatency,
        double p50Latency,
        double p95Latency,
        double p99Latency) {
}
