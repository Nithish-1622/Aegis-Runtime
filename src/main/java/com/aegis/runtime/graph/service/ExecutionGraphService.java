package com.aegis.runtime.graph.service;

import com.aegis.runtime.contracts.RuntimeEvent;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import com.aegis.runtime.graph.model.ExecutionGraph;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExecutionGraphService {

    private final RuntimeEventRepository runtimeEventRepository;
    private final ExecutionGraphBuilder executionGraphBuilder;

    public ExecutionGraphService(
            RuntimeEventRepository runtimeEventRepository,
            ExecutionGraphBuilder executionGraphBuilder) {
        this.runtimeEventRepository = runtimeEventRepository;
        this.executionGraphBuilder = executionGraphBuilder;
    }

    public ExecutionGraph buildGraph(String traceId) {
        List<RuntimeEvent> runtimeEvents = runtimeEventRepository.findByTraceIdOrderByTimestampAsc(traceId)
                .stream()
                .map(entity -> new RuntimeEvent(
                        entity.getEventId(),
                        entity.getTraceId(),
                        entity.getSpanId(),
                        entity.getParentSpanId(),
                        entity.getServiceName(),
                        entity.getInstanceId(),
                        entity.getEventType(),
                        entity.getTimestamp(),
                        entity.getThreadName(),
                        entity.getThreadId() != null ? entity.getThreadId() : 0L,
                        entity.getLatency(),
                        entity.getStatus(),
                        null))
                .toList();

        return executionGraphBuilder.build(traceId, runtimeEvents);
    }
}
