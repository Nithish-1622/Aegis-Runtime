package com.aegis.runtime.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aegis.runtime.contracts.EventStatus;
import com.aegis.runtime.contracts.EventType;
import com.aegis.runtime.dashboard.dto.DashboardTrend;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardTrendServiceTest {

    @Test
    void returnsEmptyArrayForEmptyDatabase() {
        DashboardTrendService service = new DashboardTrendService(repositoryStub(List.of()));

        DashboardTrendService.TrendResponse response = service.getTrends("24h");

        assertEquals("24h", response.window());
        assertTrue(response.points().isEmpty());
    }

    @Test
    void aggregatesTrendPointsByWindow() {
        Instant now = Instant.now();
        List<RuntimeEventEntity> events = List.of(
                event(now.minus(Duration.ofHours(1)).plus(Duration.ofMinutes(5)), EventType.REQUEST_STARTED, EventStatus.SUCCESS, Duration.ofMillis(40)),
                event(now.minus(Duration.ofHours(1)).plus(Duration.ofMinutes(5)), EventType.RETRY_TRIGGERED, EventStatus.SUCCESS, Duration.ofMillis(40)),
                event(now.minus(Duration.ofMinutes(30)), EventType.ERROR_OCCURRED, EventStatus.FAILED, Duration.ofMillis(90)));

        DashboardTrendService service = new DashboardTrendService(repositoryStub(events));

        DashboardTrendService.TrendResponse response = service.getTrends("24h");

        assertFalse(response.points().isEmpty());
        assertEquals(3L, response.points().stream().mapToLong(DashboardTrend::requestCount).sum());
    }

    private RuntimeEventEntity event(Instant timestamp, EventType type, EventStatus status, Duration latency) {
        RuntimeEventEntity entity = new RuntimeEventEntity();
        entity.setEventId("evt-" + timestamp.toEpochMilli() + "-" + type);
        entity.setTraceId("trace-1");
        entity.setServiceName("payment-service");
        entity.setTimestamp(timestamp);
        entity.setEventType(type);
        entity.setStatus(status);
        entity.setLatency(latency);
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
