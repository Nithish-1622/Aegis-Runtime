package com.aegis.runtime.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aegis.runtime.anomaly.entity.RuntimeAnomalyEntity;
import com.aegis.runtime.anomaly.model.AnomalyType;
import com.aegis.runtime.anomaly.model.Severity;
import com.aegis.runtime.anomaly.repository.RuntimeAnomalyRepository;
import com.aegis.runtime.dashboard.dto.RuntimeHealth;
import com.aegis.runtime.retry.RetryAnalysis;
import com.aegis.runtime.retry.RetryAnalysisService;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuntimeHealthServiceTest {

    @Test
    void returnsHealthyStatusForEmptyDatabase() {
        RuntimeHealthService service = new RuntimeHealthService(anomalyRepo(List.of()), eventRepo(List.of()), retryService(new RetryAnalysis(0, 0, 0, List.of(), 0, 0)));

        RuntimeHealth health = service.getHealth();

        assertEquals("HEALTHY", health.status());
        assertEquals(100, health.score());
        assertEquals(0L, health.activeAnomalies());
    }

    @Test
    void deductsScoreForHighLatencyRetryStormAndCriticalAnomaly() {
        List<RuntimeAnomalyEntity> anomalies = List.of(
                anomaly("payment-service", Severity.CRITICAL, AnomalyType.SERVICE_SATURATION),
                anomaly("payment-service", Severity.HIGH, AnomalyType.HIGH_LATENCY));
        List<RuntimeEventEntity> events = List.of(event(Duration.ofMillis(300)));
        RuntimeHealthService service = new RuntimeHealthService(
                anomalyRepo(anomalies),
                eventRepo(events),
                retryService(new RetryAnalysis(25, 0, 1.8, List.of("payment-service"), 0, 0)));

        RuntimeHealth health = service.getHealth();

        assertEquals("UNSTABLE", health.status());
        assertEquals(40, health.score());
        assertTrue(health.criticalServices().contains("payment-service"));
    }

    private RuntimeEventEntity event(Duration latency) {
        RuntimeEventEntity entity = new RuntimeEventEntity();
        entity.setEventId("evt-1");
        entity.setServiceName("payment-service");
        entity.setTimestamp(Instant.now());
        entity.setLatency(latency);
        return entity;
    }

    private RuntimeAnomalyEntity anomaly(String serviceName, Severity severity, AnomalyType type) {
        RuntimeAnomalyEntity entity = new RuntimeAnomalyEntity();
        entity.setAnomalyId(java.util.UUID.randomUUID());
        entity.setServiceName(serviceName);
        entity.setSeverity(severity);
        entity.setAnomalyType(type);
        entity.setDetectedAt(Instant.now());
        return entity;
    }

    private RetryAnalysisService retryService(RetryAnalysis analysis) {
        return new RetryAnalysisService(null) {
            @Override
            public RetryAnalysis analyze() {
                return analysis;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private RuntimeAnomalyRepository anomalyRepo(List<RuntimeAnomalyEntity> anomalies) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return switch (method.getName()) {
                    case "findAll" -> anomalies;
                    case "toString" -> "RuntimeAnomalyRepositoryStub";
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            }
        };
        return (RuntimeAnomalyRepository) Proxy.newProxyInstance(
                RuntimeAnomalyRepository.class.getClassLoader(),
                new Class<?>[] {RuntimeAnomalyRepository.class},
                handler);
    }

    @SuppressWarnings("unchecked")
    private RuntimeEventRepository eventRepo(List<RuntimeEventEntity> events) {
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
