package com.aegis.runtime.trace.dto;

import java.util.List;

public class TimelineResponse {

    private String traceId;
    private List<TimelineEventDTO> timeline;

    public TimelineResponse() {
    }

    public TimelineResponse(String traceId, List<TimelineEventDTO> timeline) {
        this.traceId = traceId;
        this.timeline = timeline;
    }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public List<TimelineEventDTO> getTimeline() { return timeline; }
    public void setTimeline(List<TimelineEventDTO> timeline) { this.timeline = timeline; }
}
