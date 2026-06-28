package com.aegis.runtime.graph.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aegis.runtime.contracts.EventStatus;
import com.aegis.runtime.contracts.EventType;
import com.aegis.runtime.contracts.RuntimeEvent;
import com.aegis.runtime.graph.model.ExecutionGraph;
import com.aegis.runtime.graph.model.GraphEdgeType;
import com.aegis.runtime.graph.model.GraphNodeType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecutionGraphBuilderTest {

    private final ExecutionGraphBuilder executionGraphBuilder = new ExecutionGraphBuilder();

    @Test
    void buildsGraphWithExpectedNodeAndEdgeCounts() {
        ExecutionGraph graph = executionGraphBuilder.build("trace-1", sampleEvents());

        assertEquals(5, graph.getNodes().size());
        assertEquals(4, graph.getEdges().size());
    }

    @Test
    void preservesChronologicalOrdering() {
        ExecutionGraph graph = executionGraphBuilder.build("trace-1", sampleEvents());

        List<String> nodeOrder = graph.getNodes().stream().map(node -> node.getId()).toList();
        assertIterableEquals(List.of("evt-1", "evt-2", "evt-3", "evt-4", "evt-5"), nodeOrder);
    }

    @Test
    void createsRetryEdge() {
        ExecutionGraph graph = executionGraphBuilder.build("trace-1", sampleEvents());

        assertTrue(graph.getEdges().stream()
                .anyMatch(edge -> edge.getSource().equals("evt-3")
                        && edge.getTarget().equals("evt-4")
                        && edge.getType() == GraphEdgeType.RETRIES));
    }

    @Test
    void createsErrorEdge() {
        ExecutionGraph graph = executionGraphBuilder.build("trace-1", sampleEvents());

        assertTrue(graph.getEdges().stream()
                .anyMatch(edge -> edge.getSource().equals("evt-3")
                        && edge.getTarget().equals("evt-5")
                        && edge.getType() == GraphEdgeType.CAUSES));
    }

    @Test
    void mapsNodeTypesCorrectly() {
        ExecutionGraph graph = executionGraphBuilder.build("trace-1", sampleEvents());

        assertEquals(GraphNodeType.REQUEST, graph.getNodes().get(0).getType());
        assertEquals(GraphNodeType.SERVICE, graph.getNodes().get(1).getType());
        assertEquals(GraphNodeType.SERVICE, graph.getNodes().get(2).getType());
        assertEquals(GraphNodeType.RETRY, graph.getNodes().get(3).getType());
        assertEquals(GraphNodeType.ERROR, graph.getNodes().get(4).getType());
    }

    private List<RuntimeEvent> sampleEvents() {
        return List.of(
                new RuntimeEvent(
                        "evt-1",
                        "trace-1",
                        "span-1",
                        null,
                        "payment-service",
                        "inst-1",
                        EventType.REQUEST_STARTED,
                        Instant.parse("2026-06-16T10:00:00Z"),
                        "main",
                        1L,
                        Duration.ofMillis(5),
                        EventStatus.IN_PROGRESS,
                        null),
                new RuntimeEvent(
                        "evt-2",
                        "trace-1",
                        "span-2",
                        "span-1",
                        "order-service",
                        "inst-1",
                        EventType.SERVICE_CALL_STARTED,
                        Instant.parse("2026-06-16T10:00:01Z"),
                        "main",
                        1L,
                        Duration.ofMillis(7),
                        EventStatus.IN_PROGRESS,
                        null),
                new RuntimeEvent(
                        "evt-3",
                        "trace-1",
                        "span-2",
                        "span-1",
                        "order-service",
                        "inst-1",
                        EventType.SERVICE_CALL_COMPLETED,
                        Instant.parse("2026-06-16T10:00:02Z"),
                        "main",
                        1L,
                        Duration.ofMillis(8),
                        EventStatus.SUCCESS,
                        null),
                new RuntimeEvent(
                        "evt-4",
                        "trace-1",
                        "span-3",
                        "span-2",
                        "order-service",
                        "inst-1",
                        EventType.RETRY_TRIGGERED,
                        Instant.parse("2026-06-16T10:00:03Z"),
                        "worker-1",
                        2L,
                        Duration.ofMillis(9),
                        EventStatus.IN_PROGRESS,
                        null),
                new RuntimeEvent(
                        "evt-5",
                        "trace-1",
                        "span-4",
                        "span-2",
                        "order-service",
                        "inst-1",
                        EventType.ERROR_OCCURRED,
                        Instant.parse("2026-06-16T10:00:04Z"),
                        "worker-1",
                        2L,
                        Duration.ofMillis(10),
                        EventStatus.FAILED,
                        null));
    }
}
