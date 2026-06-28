package com.aegis.runtime.analytics.service;

import com.aegis.runtime.analytics.dto.LatencyMetrics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LatencyCalculator {

    public LatencyMetrics calculate(List<Long> latencies) {
        if (latencies == null || latencies.isEmpty()) {
            return new LatencyMetrics(0, 0, 0, 0, 0, 0, 0);
        }

        List<Long> sortedLatencies = new ArrayList<>(latencies);
        Collections.sort(sortedLatencies);

        long requestCount = sortedLatencies.size();
        double minLatency = sortedLatencies.getFirst();
        double maxLatency = sortedLatencies.getLast();
        double averageLatency = sortedLatencies.stream().mapToLong(Long::longValue).average().orElse(0);
        double p50Latency = percentile(sortedLatencies, 50);
        double p95Latency = percentile(sortedLatencies, 95);
        double p99Latency = percentile(sortedLatencies, 99);

        return new LatencyMetrics(
                averageLatency,
                minLatency,
                maxLatency,
                p50Latency,
                p95Latency,
                p99Latency,
                requestCount);
    }

    private double percentile(List<Long> sortedLatencies, double percentile) {
        if (sortedLatencies.isEmpty()) {
            return 0;
        }
        if (sortedLatencies.size() == 1) {
            return sortedLatencies.getFirst();
        }

        double index = percentile / 100.0 * (sortedLatencies.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sortedLatencies.get(lower);
        }

        double fraction = index - lower;
        return sortedLatencies.get(lower) + (sortedLatencies.get(upper) - sortedLatencies.get(lower)) * fraction;
    }
}
