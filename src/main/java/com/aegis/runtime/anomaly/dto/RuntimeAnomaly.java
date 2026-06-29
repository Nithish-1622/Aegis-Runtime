package com.aegis.runtime.anomaly.dto;

import com.aegis.runtime.anomaly.model.AnomalyType;
import com.aegis.runtime.anomaly.model.Severity;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RuntimeAnomaly(
        UUID anomalyId,
        String traceId,
        String serviceName,
        AnomalyType type,
        Severity severity,
        String description,
        Instant detectedAt,
        Map<String, Object> metadata) {
}
