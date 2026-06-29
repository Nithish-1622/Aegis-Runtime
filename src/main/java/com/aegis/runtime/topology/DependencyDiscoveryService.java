package com.aegis.runtime.topology;

import com.aegis.runtime.contracts.EventType;
import com.aegis.runtime.contracts.RuntimeEvent;
import com.aegis.runtime.graph.model.ExecutionGraph;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DependencyDiscoveryService {

    private final RuntimeEventRepository runtimeEventRepository;

    public DependencyDiscoveryService(RuntimeEventRepository runtimeEventRepository) {
        this.runtimeEventRepository = runtimeEventRepository;
    }

    public ServiceTopology discover() {
        List<RuntimeEvent> events = runtimeEventRepository.findAll().stream()
                .map(this::toRuntimeEvent)
                .sorted(Comparator.comparing(RuntimeEvent::timestamp, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return discover(events, null);
    }

    public ServiceTopology discover(String traceId, ExecutionGraph executionGraph) {
        List<RuntimeEvent> events = runtimeEventRepository.findByTraceIdOrderByTimestampAsc(traceId).stream()
                .map(this::toRuntimeEvent)
                .toList();
        return discover(events, executionGraph);
    }

    private ServiceTopology discover(List<RuntimeEvent> events, ExecutionGraph executionGraph) {
        if (events.isEmpty()) {
            return new ServiceTopology(List.of(), List.of());
        }

        Map<String, Long> requestCounts = new LinkedHashMap<>();
        Map<String, Long> dependencyCounts = new LinkedHashMap<>();
        Map<String, RuntimeEvent> spanIndex = new HashMap<>();
        Set<String> dependencyKeys = new LinkedHashSet<>();

        for (RuntimeEvent event : events) {
            if (event.serviceName() != null) {
                requestCounts.put(event.serviceName(), requestCounts.getOrDefault(event.serviceName(), 0L) + 1);
            }

            String spanId = fallbackId(event.spanId(), event.eventId());
            if (spanId != null) {
                spanIndex.put(spanId, event);
            }
        }

        for (RuntimeEvent event : events) {
            if (event.parentSpanId() == null || event.parentSpanId().isBlank()) {
                continue;
            }

            RuntimeEvent parent = spanIndex.get(event.parentSpanId());
            if (parent == null || parent.serviceName() == null || event.serviceName() == null) {
                continue;
            }
            if (Objects.equals(parent.serviceName(), event.serviceName())) {
                continue;
            }

            String key = parent.serviceName() + "->" + event.serviceName();
            if (dependencyKeys.add(key)) {
                dependencyCounts.put(key, dependencyCounts.getOrDefault(key, 0L) + 1);
            } else {
                dependencyCounts.put(key, dependencyCounts.get(key) + 1);
            }
        }

        List<ServiceNode> services = requestCounts.entrySet().stream()
                .map(entry -> new ServiceNode(entry.getKey(), entry.getValue()))
                .toList();

        List<ServiceDependency> dependencies = dependencyCounts.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("->", 2);
                    return new ServiceDependency(parts[0], parts[1], entry.getValue());
                })
                .toList();

        return new ServiceTopology(services, dependencies);
    }

    private RuntimeEvent toRuntimeEvent(RuntimeEventEntity entity) {
        return new RuntimeEvent(
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
                null);
    }

    private String fallbackId(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }
}
