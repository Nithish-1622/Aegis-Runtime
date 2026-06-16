package com.aegis.runtime.trace.service;

import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.trace.dto.TimelineResponse;
import com.aegis.runtime.trace.mapper.TimelineMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TimelineReconstructionServiceImpl implements TimelineReconstructionService {

    private final TimelineMapper timelineMapper;

    public TimelineReconstructionServiceImpl(TimelineMapper timelineMapper) {
        this.timelineMapper = timelineMapper;
    }

    @Override
    public TimelineResponse reconstruct(String traceId, List<RuntimeEventEntity> events) {
        return new TimelineResponse(traceId, timelineMapper.toDtoList(events));
    }
}
