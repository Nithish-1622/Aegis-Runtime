package com.aegis.runtime.graph.model;

import java.util.Collections;
import java.util.List;

public class ExecutionGraph {

    private final String traceId;
    private final GraphStatistics statistics;
    private final List<GraphNode> nodes;
    private final List<GraphEdge> edges;

    public ExecutionGraph(String traceId, GraphStatistics statistics, List<GraphNode> nodes, List<GraphEdge> edges) {
        this.traceId = traceId;
        this.statistics = statistics;
        this.nodes = nodes == null ? List.of() : Collections.unmodifiableList(List.copyOf(nodes));
        this.edges = edges == null ? List.of() : Collections.unmodifiableList(List.copyOf(edges));
    }

    public String getTraceId() {
        return traceId;
    }

    public GraphStatistics getStatistics() {
        return statistics;
    }

    public List<GraphNode> getNodes() {
        return nodes;
    }

    public List<GraphEdge> getEdges() {
        return edges;
    }
}
