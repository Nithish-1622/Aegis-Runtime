package com.aegis.runtime.contracts;

import java.time.Duration;
import java.time.Instant;

public record RuntimeEvent(
        String eventId,
        String traceId,
        String spanId,
        String parentSpanId,
        String serviceName,
        String instanceId,
        EventType eventType,
        Instant timestamp,
        String threadName,
        long threadId,
        Duration latency,
        EventStatus status,
        Payload payload) {
}