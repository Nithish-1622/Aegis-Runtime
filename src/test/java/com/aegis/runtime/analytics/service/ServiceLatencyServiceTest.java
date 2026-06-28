package com.aegis.runtime.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.aegis.runtime.analytics.dto.ServiceLatency;
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

class ServiceLatencyServiceTest {

    @Test
    void computesServiceLatencyMetrics() {
        RuntimeEventRepository repository = repositoryStub(List.of(
                event("payment-service", EventStatus.SUCCESS, Duration.ofMillis(10)),
                event("payment-service", EventStatus.FAILED, Duration.ofMillis(30)),
                event("payment-service", EventStatus.SUCCESS, Duration.ofMillis(20)),
                event("order-service", EventStatus.SUCCESS, Duration.ofMillis(50))));

        ServiceLatencyService service = new ServiceLatencyService(repository, new LatencyCalculator());
        ServiceLatency metrics = service.getMetrics("payment-service");

        assertNotNull(metrics);
        assertEquals("payment-service", metrics.serviceName());
        assertEquals(3L, metrics.requestCount());
        assertEquals(66.6667, metrics.successRate(), 0.01);
        assertEquals(33.3333, metrics.failureRate(), 0.01);
    }

    @Test
    void returnsNullForUnknownService() {
        RuntimeEventRepository repository = repositoryStub(List.of(event("payment-service", EventStatus.SUCCESS, Duration.ofMillis(10))));

        ServiceLatencyService service = new ServiceLatencyService(repository, new LatencyCalculator());

        assertEquals(null, service.getMetrics("missing-service"));
    }

    private RuntimeEventEntity event(String serviceName, EventStatus status, Duration latency) {
        RuntimeEventEntity entity = new RuntimeEventEntity();
        entity.setEventId(serviceName + "-" + status);
        entity.setServiceName(serviceName);
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
