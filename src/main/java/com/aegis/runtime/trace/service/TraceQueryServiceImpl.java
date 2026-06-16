package com.aegis.runtime.trace.service;

import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import com.aegis.runtime.trace.dto.TimelineResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TraceQueryServiceImpl implements TraceQueryService {

    private final RuntimeEventRepository runtimeEventRepository;
    private final TimelineReconstructionService timelineReconstructionService;

    public TraceQueryServiceImpl(
            RuntimeEventRepository runtimeEventRepository,
            TimelineReconstructionService timelineReconstructionService) {
        this.runtimeEventRepository = runtimeEventRepository;
        this.timelineReconstructionService = timelineReconstructionService;
    }

    @Override
    public TimelineResponse getTimeline(String traceId) {
        List<RuntimeEventEntity> events = runtimeEventRepository.findByTraceIdOrderByTimestampAsc(traceId);
        return timelineReconstructionService.reconstruct(traceId, events);
    }
}
