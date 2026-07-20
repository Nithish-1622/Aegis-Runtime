package com.aegis.runtime.dashboard.dto;

import java.util.List;

public record RuntimeHealth(
        String status,
        int score,
        long activeAnomalies,
        List<String> criticalServices,
        String summary) {
}
