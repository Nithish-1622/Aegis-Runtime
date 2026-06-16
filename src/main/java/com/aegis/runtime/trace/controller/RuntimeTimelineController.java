package com.aegis.runtime.trace.controller;

import com.aegis.runtime.trace.dto.TimelineResponse;
import com.aegis.runtime.trace.service.TraceQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/runtime")
public class RuntimeTimelineController {

    private final TraceQueryService traceQueryService;

    public RuntimeTimelineController(TraceQueryService traceQueryService) {
        this.traceQueryService = traceQueryService;
    }

    @GetMapping("/timeline/{traceId}")
    public ResponseEntity<TimelineResponse> getTimeline(@PathVariable String traceId) {
        return ResponseEntity.ok(traceQueryService.getTimeline(traceId));
    }
}
