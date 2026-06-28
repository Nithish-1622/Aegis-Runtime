package com.aegis.runtime.graph.service;

import com.aegis.runtime.contracts.EventStatus;
import com.aegis.runtime.contracts.EventType;
import com.aegis.runtime.contracts.RuntimeEvent;
import com.aegis.runtime.graph.model.ExecutionGraph;
import com.aegis.runtime.graph.model.GraphEdge;
import com.aegis.runtime.graph.model.GraphEdgeType;
import com.aegis.runtime.graph.model.GraphNode;
import com.aegis.runtime.graph.model.GraphNodeType;
import com.aegis.runtime.graph.model.GraphStatistics;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ExecutionGraphBuilder {

    private final GraphNodeClassifier graphNodeClassifier;

    public ExecutionGraphBuilder(GraphNodeClassifier graphNodeClassifier) {
        this.graphNodeClassifier = graphNodeClassifier;
    }

    public ExecutionGraph build(String traceId, List<RuntimeEvent> runtimeEvents) {
        List<RuntimeEvent> sortedEvents = runtimeEvents == null ? List.of() : runtimeEvents.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RuntimeEvent::timestamp, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Map<String, GraphNode> nodesById = new LinkedHashMap<>();
        Map<String, Integer> depthByNodeId = new HashMap<>();
        Set<String> edgeKeys = new LinkedHashSet<>();
        List<GraphEdge> edges = new ArrayList<>();

        int requestCount = 0;
        int serviceCount = 0;
        int retryCount = 0;
        int errorCount = 0;
        int maxDepth = 0;

        for (RuntimeEvent runtimeEvent : sortedEvents) {
            String nodeId = nodeId(runtimeEvent);
            GraphNodeType nodeType = graphNodeClassifier.classify(runtimeEvent);

            GraphNode existingNode = nodesById.get(nodeId);
            if (existingNode == null) {
                GraphNode node = buildNode(runtimeEvent, nodeId, nodeType);
                nodesById.put(nodeId, node);
                int depth = computeDepth(runtimeEvent, nodeId, nodeType, nodesById, depthByNodeId);
                depthByNodeId.put(nodeId, depth);
                maxDepth = Math.max(maxDepth, depth);

                switch (nodeType) {
                    case REQUEST -> requestCount++;
                    case SERVICE -> serviceCount++;
                    case RETRY -> retryCount++;
                    case ERROR -> errorCount++;
                }
            } else {
                depthByNodeId.putIfAbsent(nodeId, computeDepth(runtimeEvent, nodeId, nodeType, nodesById, depthByNodeId));
            }

            String parentNodeId = parentNodeId(runtimeEvent);
            if (parentNodeId != null && nodesById.containsKey(parentNodeId) && !parentNodeId.equals(nodeId)) {
                GraphEdgeType edgeType = edgeType(runtimeEvent.eventType(), nodeType);
                addEdge(edges, edgeKeys, parentNodeId, nodeId, edgeType);
            }
        }

        List<GraphNode> nodes = nodesById.values().stream()
                .sorted(Comparator.comparing(GraphNode::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        GraphStatistics statistics = new GraphStatistics(
                nodes.size(),
                edges.size(),
                requestCount,
                serviceCount,
                retryCount,
                errorCount,
                maxDepth);

        return new ExecutionGraph(traceId, statistics, nodes, edges);
    }

    private GraphNode buildNode(RuntimeEvent runtimeEvent, String nodeId, GraphNodeType nodeType) {
        String label = runtimeEvent.serviceName() != null && !runtimeEvent.serviceName().isBlank()
                ? runtimeEvent.serviceName()
                : runtimeEvent.eventType() != null ? runtimeEvent.eventType().name() : "UNKNOWN";

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("serviceName", runtimeEvent.serviceName());
        metadata.put("eventType", runtimeEvent.eventType() != null ? runtimeEvent.eventType().name() : null);
        metadata.put("status", runtimeEvent.status() != null ? runtimeEvent.status().name() : null);
        metadata.put("latency", runtimeEvent.latency());
        metadata.put("instanceId", runtimeEvent.instanceId());
        return new GraphNode(nodeId, label, nodeType, runtimeEvent.timestamp(), metadata);
    }

    private String nodeId(RuntimeEvent runtimeEvent) {
        if (runtimeEvent == null) {
            return null;
        }

        if (runtimeEvent.eventType() == EventType.RETRY_TRIGGERED || runtimeEvent.eventType() == EventType.ERROR_OCCURRED) {
            return runtimeEvent.eventId() != null ? runtimeEvent.eventId() : runtimeEvent.spanId();
        }

        return runtimeEvent.spanId() != null && !runtimeEvent.spanId().isBlank()
                ? runtimeEvent.spanId()
                : runtimeEvent.eventId();
    }

    private String parentNodeId(RuntimeEvent runtimeEvent) {
        if (runtimeEvent == null || runtimeEvent.parentSpanId() == null || runtimeEvent.parentSpanId().isBlank()) {
            return null;
        }
        return runtimeEvent.parentSpanId();
    }

    private GraphEdgeType edgeType(EventType eventType, GraphNodeType nodeType) {
        if (nodeType == GraphNodeType.RETRY) {
            return GraphEdgeType.RETRIES;
        }
        if (nodeType == GraphNodeType.ERROR) {
            return GraphEdgeType.CAUSES;
        }
        if (eventType == EventType.RETRY_TRIGGERED) {
            return GraphEdgeType.RETRIES;
        }
        if (eventType == EventType.ERROR_OCCURRED) {
            return GraphEdgeType.CAUSES;
        }
        return GraphEdgeType.CALLS;
    }

    private void addEdge(
            List<GraphEdge> edges,
            Set<String> edgeKeys,
            String source,
            String target,
            GraphEdgeType type) {
        String key = source + "->" + target + ":" + type;
        if (source.equals(target) || edgeKeys.contains(key)) {
            return;
        }
        edgeKeys.add(key);
        edges.add(new GraphEdge(source, target, type));
    }

    private int computeDepth(
            RuntimeEvent runtimeEvent,
            String nodeId,
            GraphNodeType nodeType,
            Map<String, GraphNode> nodesById,
            Map<String, Integer> depthByNodeId) {
        String parentNodeId = parentNodeId(runtimeEvent);
        if (parentNodeId == null || !nodesById.containsKey(parentNodeId) || parentNodeId.equals(nodeId)) {
            return 1;
        }

        int parentDepth = depthByNodeId.getOrDefault(parentNodeId, 1);
        if (nodeType == GraphNodeType.RETRY || nodeType == GraphNodeType.ERROR || nodeType == GraphNodeType.SERVICE) {
            return parentDepth + 1;
        }
        return parentDepth + 1;
    }
}
