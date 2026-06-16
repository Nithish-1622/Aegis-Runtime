package com.aegis.runtime.trace.service;

import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.trace.dto.TimelineResponse;
import java.util.List;

public interface TimelineReconstructionService {

    TimelineResponse reconstruct(String traceId, List<RuntimeEventEntity> events);
}
