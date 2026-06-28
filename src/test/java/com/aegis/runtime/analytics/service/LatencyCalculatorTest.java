package com.aegis.runtime.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aegis.runtime.analytics.dto.LatencyMetrics;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class LatencyCalculatorTest {

    private final LatencyCalculator latencyCalculator = new LatencyCalculator();

    @Test
    void calculatesAverageMedianAndPercentiles() {
        LatencyMetrics metrics = latencyCalculator.calculate(List.of(10L, 20L, 30L, 40L, 50L));

        assertEquals(30.0, metrics.averageLatency(), 0.0001);
        assertEquals(10.0, metrics.minLatency(), 0.0001);
        assertEquals(50.0, metrics.maxLatency(), 0.0001);
        assertEquals(30.0, metrics.p50Latency(), 0.0001);
        assertEquals(48.0, metrics.p95Latency(), 0.0001);
        assertEquals(49.6, metrics.p99Latency(), 0.0001);
        assertEquals(5L, metrics.requestCount());
    }

    @Test
    void handlesSingleValue() {
        LatencyMetrics metrics = latencyCalculator.calculate(List.of(42L));

        assertEquals(42.0, metrics.averageLatency(), 0.0001);
        assertEquals(42.0, metrics.p50Latency(), 0.0001);
        assertEquals(42.0, metrics.p95Latency(), 0.0001);
        assertEquals(42.0, metrics.p99Latency(), 0.0001);
    }

    @Test
    void handlesEmptyList() {
        LatencyMetrics metrics = latencyCalculator.calculate(List.of());

        assertEquals(0.0, metrics.averageLatency(), 0.0001);
        assertEquals(0.0, metrics.minLatency(), 0.0001);
        assertEquals(0.0, metrics.maxLatency(), 0.0001);
        assertEquals(0L, metrics.requestCount());
    }

    @Test
    void handlesDuplicateValues() {
        LatencyMetrics metrics = latencyCalculator.calculate(List.of(7L, 7L, 7L, 7L));

        assertEquals(7.0, metrics.averageLatency(), 0.0001);
        assertEquals(7.0, metrics.p50Latency(), 0.0001);
        assertEquals(7.0, metrics.p95Latency(), 0.0001);
        assertEquals(7.0, metrics.p99Latency(), 0.0001);
    }

    @Test
    void handlesLargeDataset() {
        List<Long> latencies = LongStream.rangeClosed(1, 10_000).boxed().toList();
        LatencyMetrics metrics = latencyCalculator.calculate(latencies);

        assertEquals(5000.5, metrics.averageLatency(), 0.0001);
        assertEquals(5000.5, metrics.p50Latency(), 0.0001);
    }
}
