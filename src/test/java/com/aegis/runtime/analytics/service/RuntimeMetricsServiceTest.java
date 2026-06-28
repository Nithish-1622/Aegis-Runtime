package com.aegis.runtime.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.aegis.runtime.analytics.dto.RuntimeMetrics;
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

class RuntimeMetricsServiceTest {

    @Test
    void computesRuntimeMetricsFromEvents() {
        RuntimeEventRepository repository = repositoryStub(List.of(
                event("evt-1", EventStatus.SUCCESS, Duration.ofMillis(10)),
                event("evt-2", EventStatus.FAILED, Duration.ofMillis(30)),
                event("evt-3", EventStatus.SUCCESS, Duration.ofMillis(20))));

        RuntimeMetricsService service = new RuntimeMetricsService(repository, new LatencyCalculator());
        RuntimeMetrics metrics = service.getRuntimeMetrics();

        assertEquals(3L, metrics.totalRequests());
        assertEquals(2L, metrics.successfulRequests());
        assertEquals(1L, metrics.failedRequests());
        assertEquals(20.0, metrics.averageLatency(), 0.0001);
    }

    @Test
    void returnsZeroMetricsForEmptyDatabase() {
        RuntimeMetricsService service = new RuntimeMetricsService(repositoryStub(List.of()), new LatencyCalculator());

        RuntimeMetrics metrics = service.getRuntimeMetrics();

        assertEquals(0L, metrics.totalRequests());
        assertEquals(0.0, metrics.averageLatency(), 0.0001);
    }

    private RuntimeEventEntity event(String eventId, EventStatus status, Duration latency) {
        RuntimeEventEntity entity = new RuntimeEventEntity();
        entity.setEventId(eventId);
        entity.setServiceName("payment-service");
        entity.setEventType(EventType.REQUEST_STARTED);
        entity.setTimestamp(Instant.parse("2026-06-16T10:00:00Z"));
        entity.setLatency(latency);
        entity.setStatus(status);
        return entity;
    }

    @SuppressWarnings("unchecked")
    private RuntimeEventRepository repositoryStub(List<RuntimeEventEntity> events) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return switch (method.getName()) {
                    case "findAll" -> events;
                    case "findByServiceName" -> {
                        String serviceName = (String) args[0];
                        yield events.stream()
                                .filter(event -> serviceName.equals(event.getServiceName()))
                                .toList();
                    }
                    case "findByTraceIdOrderByTimestampAsc" -> events;
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
