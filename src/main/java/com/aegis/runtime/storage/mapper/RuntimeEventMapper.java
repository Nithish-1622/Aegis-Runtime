package com.aegis.runtime.storage.mapper;

import com.aegis.runtime.contracts.RuntimeEvent;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class RuntimeEventMapper {

    private final ObjectMapper objectMapper;

    public RuntimeEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RuntimeEventEntity toEntity(RuntimeEvent runtimeEvent) {
        try {
            String payloadJson = runtimeEvent.payload() == null
                    ? null
                    : objectMapper.writeValueAsString(runtimeEvent.payload().attributes());

            return new RuntimeEventEntity(
                    runtimeEvent.eventId(),
                    runtimeEvent.traceId(),
                    runtimeEvent.spanId(),
                    runtimeEvent.parentSpanId(),
                    runtimeEvent.serviceName(),
                    runtimeEvent.instanceId(),
                    runtimeEvent.eventType(),
                    runtimeEvent.timestamp(),
                    runtimeEvent.threadName(),
                    runtimeEvent.threadId(),
                    runtimeEvent.latency(),
                    runtimeEvent.status(),
                    payloadJson);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Failed to serialize runtime event payload to JSON", exception);
        }
    }
}
