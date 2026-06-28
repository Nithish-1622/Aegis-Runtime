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

    private final ExecutionGraphBuilder executionGraphBuilder =
            new ExecutionGraphBuilder(new GraphNodeClassifier());

    @Test
    void buildsGraphWithExpectedNodeAndEdgeCounts() {
        ExecutionGraph graph = executionGraphBuilder.build("trace-1", sampleEvents());

        assertEquals(4, graph.getNodes().size());
        assertEquals(3, graph.getEdges().size());
    }

    @Test
    void preservesChronologicalOrdering() {
        ExecutionGraph graph = executionGraphBuilder.build("trace-1", sampleEvents());

        List<String> nodeOrder = graph.getNodes().stream().map(node -> node.getId()).toList();
        assertIterableEquals(List.of("span-req", "span-svc", "evt-retry", "evt-error"), nodeOrder);
    }

    @Test
    void createsRetryEdge() {
        ExecutionGraph graph = executionGraphBuilder.build("trace-1", sampleEvents());

        assertTrue(graph.getEdges().stream()
                .anyMatch(edge -> edge.getSource().equals("span-svc")
                        && edge.getTarget().equals("evt-retry")
                        && edge.getType() == GraphEdgeType.RETRIES));
    }

    @Test
    void createsErrorEdge() {
        ExecutionGraph graph = executionGraphBuilder.build("trace-1", sampleEvents());

        assertTrue(graph.getEdges().stream()
                .anyMatch(edge -> edge.getSource().equals("span-svc")
                        && edge.getTarget().equals("evt-error")
                        && edge.getType() == GraphEdgeType.CAUSES));
    }

    @Test
    void mapsNodeTypesCorrectly() {
        ExecutionGraph graph = executionGraphBuilder.build("trace-1", sampleEvents());

        assertEquals(GraphNodeType.REQUEST, graph.getNodes().get(0).getType());
        assertEquals(GraphNodeType.SERVICE, graph.getNodes().get(1).getType());
        assertEquals(GraphNodeType.RETRY, graph.getNodes().get(2).getType());
        assertEquals(GraphNodeType.ERROR, graph.getNodes().get(3).getType());
    }

    @Test
    void computesStatistics() {
        ExecutionGraph graph = executionGraphBuilder.build("trace-1", sampleEvents());

        assertEquals(4, graph.getStatistics().getNodeCount());
        assertEquals(3, graph.getStatistics().getEdgeCount());
        assertEquals(1, graph.getStatistics().getRequestCount());
        assertEquals(1, graph.getStatistics().getServiceCount());
        assertEquals(1, graph.getStatistics().getRetryCount());
        assertEquals(1, graph.getStatistics().getErrorCount());
        assertEquals(3, graph.getStatistics().getMaxDepth());
    }

    @Test
    void ignoresDuplicateSpanNodes() {
        ExecutionGraph graph = executionGraphBuilder.build("trace-1", duplicateSpanEvents());

        assertEquals(2, graph.getNodes().size());
        assertEquals(1, graph.getEdges().size());
    }

    @Test
    void ignoresInvalidParentReferences() {
        ExecutionGraph graph = executionGraphBuilder.build("trace-1", invalidParentEvents());

        assertEquals(2, graph.getNodes().size());
        assertEquals(0, graph.getEdges().size());
        assertEquals("span-root", graph.getNodes().get(0).getId());
    }

    private List<RuntimeEvent> sampleEvents() {
        return List.of(
                new RuntimeEvent(
                        "evt-req",
                        "trace-1",
                        "span-req",
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
                        "evt-svc-1",
                        "trace-1",
                        "span-svc",
                        "span-req",
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
                        "evt-svc-2",
                        "trace-1",
                        "span-svc",
                        "span-req",
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
                        "evt-retry",
                        "trace-1",
                        "evt-error",
                        "span-svc",
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
                        "evt-error",
                        "trace-1",
                        "evt-error",
                        "span-svc",
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

    private List<RuntimeEvent> duplicateSpanEvents() {
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
                        "span-1",
                        null,
                        "payment-service",
                        "inst-1",
                        EventType.REQUEST_COMPLETED,
                        Instant.parse("2026-06-16T10:00:01Z"),
                        "main",
                        1L,
                        Duration.ofMillis(6),
                        EventStatus.SUCCESS,
                        null),
                new RuntimeEvent(
                        "evt-3",
                        "trace-1",
                        "span-2",
                        "span-1",
                        "order-service",
                        "inst-1",
                        EventType.SERVICE_CALL_STARTED,
                        Instant.parse("2026-06-16T10:00:02Z"),
                        "main",
                        1L,
                        Duration.ofMillis(7),
                        EventStatus.IN_PROGRESS,
                        null));
    }

    private List<RuntimeEvent> invalidParentEvents() {
        return List.of(
                new RuntimeEvent(
                        "evt-root",
                        "trace-1",
                        "span-root",
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
                        "evt-child",
                        "trace-1",
                        "span-child",
                        "missing-parent",
                        "order-service",
                        "inst-1",
                        EventType.SERVICE_CALL_STARTED,
                        Instant.parse("2026-06-16T10:00:01Z"),
                        "main",
                        1L,
                        Duration.ofMillis(6),
                        EventStatus.IN_PROGRESS,
                        null));
    }
}
