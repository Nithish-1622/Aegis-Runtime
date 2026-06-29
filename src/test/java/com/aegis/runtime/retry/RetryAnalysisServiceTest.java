package com.aegis.runtime.retry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aegis.runtime.contracts.EventStatus;
import com.aegis.runtime.contracts.EventType;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RetryAnalysisServiceTest {

    @Test
    void calculatesRetryStatistics() {
        RetryAnalysisService service = new RetryAnalysisService(repositoryStub(sampleEvents()));

        RetryAnalysis analysis = service.analyze();

        assertEquals(2L, analysis.totalRetries());
        assertEquals(1L, analysis.retryChains());
        assertEquals(2.5, analysis.retryAmplificationFactor(), 0.0001);
        assertEquals(2L, analysis.longestRetryChain());
        assertEquals(2L, analysis.maxRetriesPerRequest());
        assertTrue(analysis.servicesWithRetries().contains("payment-service"));
    }

    @Test
    void returnsZeroMetricsForEmptyDatabase() {
        RetryAnalysisService service = new RetryAnalysisService(repositoryStub(List.of()));

        RetryAnalysis analysis = service.analyze();

        assertEquals(0L, analysis.totalRetries());
        assertEquals(0L, analysis.retryChains());
        assertEquals(0.0, analysis.retryAmplificationFactor(), 0.0001);
    }

    private List<RuntimeEventEntity> sampleEvents() {
        return List.of(
                event("evt-1", "trace-1", "span-1", null, EventType.REQUEST_STARTED, "payment-service"),
                event("evt-2", "trace-1", "span-2", "span-1", EventType.RETRY_TRIGGERED, "payment-service"),
                event("evt-3", "trace-1", "span-3", "span-2", EventType.RETRY_TRIGGERED, "payment-service"),
                event("evt-4", "trace-1", "span-4", "span-1", EventType.ERROR_OCCURRED, "payment-service"),
                event("evt-5", "trace-1", "span-5", "span-1", EventType.REQUEST_STARTED, "inventory-service"));
    }

    private RuntimeEventEntity event(
            String eventId,
            String traceId,
            String spanId,
            String parentSpanId,
            EventType eventType,
            String serviceName) {
        RuntimeEventEntity entity = new RuntimeEventEntity();
        entity.setEventId(eventId);
        entity.setTraceId(traceId);
        entity.setSpanId(spanId);
        entity.setParentSpanId(parentSpanId);
        entity.setEventType(eventType);
        entity.setServiceName(serviceName);
        entity.setTimestamp(Instant.parse("2026-06-16T10:00:00Z"));
        entity.setLatency(Duration.ofMillis(10));
        entity.setStatus(EventStatus.SUCCESS);
        return entity;
    }

    @SuppressWarnings("unchecked")
    private RuntimeEventRepository repositoryStub(List<RuntimeEventEntity> events) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return switch (method.getName()) {
                    case "findAll" -> events;
                    case "findByTraceIdOrderByTimestampAsc" -> events;
                    case "findByServiceName" -> {
                        String serviceName = (String) args[0];
                        yield events.stream().filter(event -> serviceName.equals(event.getServiceName())).toList();
                    }
                    case "count" -> (long) events.size();
                    case "toString" -> "RuntimeEventRepositoryStub";
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            }
        };
        return (RuntimeEventRepository) Proxy.newProxyInstance(
                RuntimeEventRepository.class.getClassLoader(),
                new Class<?>[] {RuntimeEventRepository.class},
                handler);
    }
}
