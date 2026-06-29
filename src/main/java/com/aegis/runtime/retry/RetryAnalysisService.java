package com.aegis.runtime.retry;

import com.aegis.runtime.contracts.EventType;
import com.aegis.runtime.contracts.RuntimeEvent;
import com.aegis.runtime.graph.model.ExecutionGraph;
import com.aegis.runtime.graph.model.GraphEdge;
import com.aegis.runtime.graph.model.GraphEdgeType;
import com.aegis.runtime.graph.model.GraphNode;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import org.springframework.stereotype.Service;

@Service
public class RetryAnalysisService {

    private final RuntimeEventRepository runtimeEventRepository;

    public RetryAnalysisService(RuntimeEventRepository runtimeEventRepository) {
        this.runtimeEventRepository = runtimeEventRepository;
    }

    public RetryAnalysis analyze() {
        List<RuntimeEvent> events = runtimeEventRepository.findAll().stream()
                .map(this::toRuntimeEvent)
                .sorted(Comparator.comparing(RuntimeEvent::traceId, Comparator.nullsLast(String::compareTo))
                        .thenComparing(RuntimeEvent::timestamp, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return analyzeEvents(events, null);
    }

    public RetryAnalysis analyzeTrace(String traceId, ExecutionGraph executionGraph) {
        List<RuntimeEvent> traceEvents = runtimeEventRepository.findByTraceIdOrderByTimestampAsc(traceId).stream()
                .map(this::toRuntimeEvent)
                .toList();

        return analyzeEvents(traceEvents, executionGraph);
    }

    private RetryAnalysis analyzeEvents(List<RuntimeEvent> events, ExecutionGraph executionGraph) {
        if (events.isEmpty()) {
            return new RetryAnalysis(0, 0, 0, List.of(), 0, 0);
        }

        Map<String, Integer> retryDepthBySpan = new HashMap<>();
        Map<String, Integer> retriesPerRequest = new HashMap<>();
        Map<String, String> parentBySpan = new HashMap<>();
        Set<String> servicesWithRetries = new LinkedHashSet<>();
        long totalRetries = 0;
        long retryChains = 0;
        long longestRetryChain = 0;
        long maxRetriesPerRequest = 0;

        for (RuntimeEvent event : events) {
            String spanId = fallbackId(event.spanId(), event.eventId());
            if (spanId == null) {
                continue;
            }

            parentBySpan.put(spanId, event.parentSpanId());
            if (event.eventType() == EventType.RETRY_TRIGGERED) {
                totalRetries++;
                servicesWithRetries.add(event.serviceName());

                String rootRequestSpan = findRootRequestSpan(spanId, parentBySpan, events);
                int retryCountForRequest = retriesPerRequest.getOrDefault(rootRequestSpan, 0) + 1;
                retriesPerRequest.put(rootRequestSpan, retryCountForRequest);
                retryDepthBySpan.put(spanId, retryCountForRequest);
                longestRetryChain = Math.max(longestRetryChain, retryCountForRequest);
            }
        }

        for (int retryCount : retriesPerRequest.values()) {
            if (retryCount > 0) {
                retryChains++;
                maxRetriesPerRequest = Math.max(maxRetriesPerRequest, retryCount);
            }
        }

        long totalExecutions = events.size();
        long originalRequests = events.stream()
                .filter(event -> event.eventType() == EventType.REQUEST_STARTED)
                .count();
        double amplificationFactor = originalRequests == 0 ? 0 : (double) totalExecutions / originalRequests;

        return new RetryAnalysis(
                totalRetries,
                retryChains,
                amplificationFactor,
                List.copyOf(servicesWithRetries),
                longestRetryChain,
                maxRetriesPerRequest);
    }

    private String findRootRequestSpan(String spanId, Map<String, String> parentBySpan, List<RuntimeEvent> events) {
        String currentSpan = spanId;
        int guard = 0;
        while (currentSpan != null && guard++ < 100) {
            String parentSpan = parentBySpan.get(currentSpan);
            if (parentSpan == null || parentSpan.isBlank()) {
                return currentSpan;
            }
            currentSpan = parentSpan;
        }
        return spanId;
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
