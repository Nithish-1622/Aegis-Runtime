package com.aegis.runtime.analytics.dto;

public record LatencyMetrics(
        double averageLatency,
        double minLatency,
        double maxLatency,
        double p50Latency,
        double p95Latency,
        double p99Latency,
        long requestCount) {
}
