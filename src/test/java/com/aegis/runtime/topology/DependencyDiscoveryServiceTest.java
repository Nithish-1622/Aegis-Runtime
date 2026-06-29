package com.aegis.runtime.topology;

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

class DependencyDiscoveryServiceTest {

    @Test
    void generatesUniqueTopologyDependencies() {
        DependencyDiscoveryService service = new DependencyDiscoveryService(repositoryStub(sampleEvents()));

        ServiceTopology topology = service.discover();

        assertEquals(3, topology.services().size());
        assertEquals(2, topology.dependencies().size());
        assertTrue(topology.dependencies().stream().anyMatch(dep ->
                dep.source().equals("order-service") && dep.target().equals("payment-service") && dep.callCount() == 2));
    }

    @Test
    void removesSelfCallsAndHandlesEmptyDatabase() {
        DependencyDiscoveryService service = new DependencyDiscoveryService(repositoryStub(List.of()));

        ServiceTopology topology = service.discover();

        assertEquals(0, topology.services().size());
        assertEquals(0, topology.dependencies().size());
    }

    private List<RuntimeEventEntity> sampleEvents() {
        return List.of(
                event("evt-1", "order-service", "span-1", null),
                event("evt-2", "payment-service", "span-2", "span-1"),
                event("evt-3", "payment-service", "span-3", "span-1"),
                event("evt-4", "inventory-service", "span-4", "span-2"),
                event("evt-5", "payment-service", "span-5", "span-5"));
    }

    private RuntimeEventEntity event(String eventId, String serviceName, String spanId, String parentSpanId) {
        RuntimeEventEntity entity = new RuntimeEventEntity();
        entity.setEventId(eventId);
        entity.setServiceName(serviceName);
        entity.setSpanId(spanId);
        entity.setParentSpanId(parentSpanId);
        entity.setEventType(parentSpanId == null ? EventType.REQUEST_STARTED : EventType.SERVICE_CALL_STARTED);
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
