package com.aegis.runtime.dashboard.dto;

import java.time.Instant;

public record DashboardTrend(
        Instant timestamp,
        long requestCount,
        double averageLatency,
        long errorCount,
        long retryCount) {
}
