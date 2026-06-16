package com.aegis.runtime.trace.service;

import com.aegis.runtime.trace.dto.TimelineResponse;

public interface TraceQueryService {

    TimelineResponse getTimeline(String traceId);
}
