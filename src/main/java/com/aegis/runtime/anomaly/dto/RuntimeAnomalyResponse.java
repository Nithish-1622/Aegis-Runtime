package com.aegis.runtime.anomaly.dto;

import java.util.List;

public record RuntimeAnomalyResponse(
        long totalAnomalies,
        long critical,
        long high,
        long medium,
        long low,
        List<RuntimeAnomaly> anomalies) {
}
