package com.aegis.runtime.graph.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class GraphNode {

    private final String id;
    private final String label;
    private final GraphNodeType type;
    private final Instant timestamp;
    private final Map<String, Object> metadata;

    public GraphNode(
            String id,
            String label,
            GraphNodeType type,
            Instant timestamp,
            Map<String, Object> metadata) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.timestamp = timestamp;
        this.metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public GraphNodeType getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
