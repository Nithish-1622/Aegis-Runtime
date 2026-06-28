package com.aegis.runtime.graph.service;

import com.aegis.runtime.contracts.EventType;
import com.aegis.runtime.contracts.RuntimeEvent;
import com.aegis.runtime.graph.model.ExecutionGraph;
import com.aegis.runtime.graph.model.GraphEdge;
import com.aegis.runtime.graph.model.GraphEdgeType;
import com.aegis.runtime.graph.model.GraphNode;
import com.aegis.runtime.graph.model.GraphNodeType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ExecutionGraphBuilder {

    public ExecutionGraph build(String traceId, List<RuntimeEvent> runtimeEvents) {
        List<RuntimeEvent> sortedEvents = runtimeEvents.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RuntimeEvent::timestamp))
                .toList();

        List<GraphNode> nodes = new ArrayList<>(sortedEvents.size());
        List<GraphEdge> edges = new ArrayList<>();
        Map<String, String> spanToEventId = new HashMap<>();
        Map<String, GraphNode> uniqueNodes = new LinkedHashMap<>();

        for (RuntimeEvent runtimeEvent : sortedEvents) {
            if (uniqueNodes.containsKey(runtimeEvent.eventId())) {
                continue;
            }

            GraphNode graphNode = toNode(runtimeEvent);
            uniqueNodes.put(runtimeEvent.eventId(), graphNode);
            nodes.add(graphNode);

            String parentSpanId = runtimeEvent.parentSpanId();
            if (parentSpanId != null && spanToEventId.containsKey(parentSpanId)) {
                String sourceEventId = spanToEventId.get(parentSpanId);
                edges.add(new GraphEdge(sourceEventId, runtimeEvent.eventId(), toEdgeType(runtimeEvent.eventType())));
            }

            if (runtimeEvent.spanId() != null) {
                spanToEventId.put(runtimeEvent.spanId(), runtimeEvent.eventId());
            }
        }

        return new ExecutionGraph(traceId, nodes, edges);
    }

    private GraphNode toNode(RuntimeEvent runtimeEvent) {
        GraphNodeType nodeType = toNodeType(runtimeEvent.eventType());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("serviceName", runtimeEvent.serviceName());
        metadata.put("instanceId", runtimeEvent.instanceId());
        metadata.put("spanId", runtimeEvent.spanId());
        metadata.put("parentSpanId", runtimeEvent.parentSpanId());
        metadata.put("eventType", runtimeEvent.eventType().name());
        metadata.put("status", runtimeEvent.status() != null ? runtimeEvent.status().name() : null);
        metadata.put("threadName", runtimeEvent.threadName());
        metadata.put("threadId", runtimeEvent.threadId());
        metadata.put("latency", runtimeEvent.latency());
        metadata.put("payload", runtimeEvent.payload());

        String label = runtimeEvent.serviceName() != null && !runtimeEvent.serviceName().isBlank()
                ? runtimeEvent.serviceName()
                : runtimeEvent.eventType().name();

        return new GraphNode(runtimeEvent.eventId(), label, nodeType, runtimeEvent.timestamp(), metadata);
    }

    private GraphNodeType toNodeType(EventType eventType) {
        if (eventType == null) {
            return GraphNodeType.SERVICE;
        }

        return switch (eventType) {
            case REQUEST_STARTED, REQUEST_COMPLETED -> GraphNodeType.REQUEST;
            case SERVICE_CALL_STARTED, SERVICE_CALL_COMPLETED -> GraphNodeType.SERVICE;
            case RETRY_TRIGGERED -> GraphNodeType.RETRY;
            case ERROR_OCCURRED -> GraphNodeType.ERROR;
            case HEARTBEAT -> GraphNodeType.SERVICE;
        };
    }

    private GraphEdgeType toEdgeType(EventType eventType) {
        if (eventType == null) {
            return GraphEdgeType.CALLS;
        }

        return switch (eventType) {
            case RETRY_TRIGGERED -> GraphEdgeType.RETRIES;
            case ERROR_OCCURRED -> GraphEdgeType.CAUSES;
            default -> GraphEdgeType.CALLS;
        };
    }
}
