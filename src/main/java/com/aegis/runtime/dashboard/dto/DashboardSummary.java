package com.aegis.runtime.dashboard.dto;

public record DashboardSummary(
        long totalRequests,
        double averageLatency,
        double p95Latency,
        double p99Latency,
        long totalServices,
        long totalTraces,
        long totalRetries,
        double retryAmplificationFactor,
        long totalAnomalies,
        long criticalAnomalies,
        long highAnomalies,
        long healthyServices,
        long unhealthyServices) {
}
