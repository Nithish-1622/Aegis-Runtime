package com.aegis.runtime.trace.dto;

import com.aegis.runtime.contracts.EventStatus;
import com.aegis.runtime.contracts.EventType;
import java.time.Duration;
import java.time.Instant;

public class TimelineEventDTO {

    private String eventId;
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String serviceName;
    private String instanceId;
    private EventType eventType;
    private Instant timestamp;
    private String threadName;
    private Long threadId;
    private Duration latency;
    private EventStatus status;
    private String payloadJson;

    public TimelineEventDTO() {
    }

    public TimelineEventDTO(
            String eventId,
            String traceId,
            String spanId,
            String parentSpanId,
            String serviceName,
            String instanceId,
            EventType eventType,
            Instant timestamp,
            String threadName,
            Long threadId,
            Duration latency,
            EventStatus status,
            String payloadJson) {
        this.eventId = eventId;
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.serviceName = serviceName;
        this.instanceId = instanceId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.threadName = threadName;
        this.threadId = threadId;
        this.latency = latency;
        this.status = status;
        this.payloadJson = payloadJson;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }
    public String getParentSpanId() { return parentSpanId; }
    public void setParentSpanId(String parentSpanId) { this.parentSpanId = parentSpanId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getThreadName() { return threadName; }
    public void setThreadName(String threadName) { this.threadName = threadName; }
    public Long getThreadId() { return threadId; }
    public void setThreadId(Long threadId) { this.threadId = threadId; }
    public Duration getLatency() { return latency; }
    public void setLatency(Duration latency) { this.latency = latency; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
}
