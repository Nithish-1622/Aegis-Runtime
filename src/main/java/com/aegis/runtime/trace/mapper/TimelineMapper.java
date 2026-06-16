package com.aegis.runtime.trace.mapper;

import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.trace.dto.TimelineEventDTO;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TimelineMapper {

    public TimelineEventDTO toDto(RuntimeEventEntity entity) {
        return new TimelineEventDTO(
                entity.getEventId(),
                entity.getTraceId(),
                entity.getSpanId(),
                entity.getParentSpanId(),
                entity.getServiceName(),
                entity.getInstanceId(),
                entity.getEventType(),
                entity.getTimestamp(),
                entity.getThreadName(),
                entity.getThreadId(),
                entity.getLatency(),
                entity.getStatus(),
                entity.getPayloadJson());
    }

    public List<TimelineEventDTO> toDtoList(List<RuntimeEventEntity> entities) {
        return entities.stream().map(this::toDto).toList();
    }
}
