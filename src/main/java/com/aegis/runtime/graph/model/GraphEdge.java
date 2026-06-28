package com.aegis.runtime.graph.model;

public class GraphEdge {

    private final String source;
    private final String target;
    private final GraphEdgeType type;

    public GraphEdge(String source, String target, GraphEdgeType type) {
        this.source = source;
        this.target = target;
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public GraphEdgeType getType() {
        return type;
    }
}
