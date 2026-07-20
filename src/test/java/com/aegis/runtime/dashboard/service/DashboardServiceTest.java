package com.aegis.runtime.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aegis.runtime.analytics.dto.RuntimeMetrics;
import com.aegis.runtime.analytics.service.RuntimeMetricsService;
import com.aegis.runtime.anomaly.entity.RuntimeAnomalyEntity;
import com.aegis.runtime.anomaly.model.AnomalyType;
import com.aegis.runtime.anomaly.model.Severity;
import com.aegis.runtime.anomaly.repository.RuntimeAnomalyRepository;
import com.aegis.runtime.dashboard.dto.DashboardSummary;
import com.aegis.runtime.retry.RetryAnalysis;
import com.aegis.runtime.retry.RetryAnalysisService;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import com.aegis.runtime.topology.DependencyDiscoveryService;
import com.aegis.runtime.topology.ServiceTopology;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    @Test
    void returnsZeroSummaryForEmptyDatabase() {
        DashboardService service = new DashboardService(
                metricsService(new RuntimeMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0)),
                retryService(new RetryAnalysis(0, 0, 0, List.of(), 0, 0)),
                anomalyRepository(List.of()),
                eventRepository(List.of()),
                new DependencyDiscoveryService(eventRepository(List.of())));

        DashboardSummary summary = service.getSummary();

        assertEquals(0L, summary.totalRequests());
        assertEquals(0L, summary.totalServices());
        assertEquals(0L, summary.totalAnomalies());
        assertEquals(0L, summary.healthyServices());
    }

    @Test
    void aggregatesDashboardData() {
        List<RuntimeEventEntity> events = List.of(
                event("trace-1", "payment-service"),
                event("trace-1", "payment-service"),
                event("trace-2", "inventory-service"));
        List<RuntimeAnomalyEntity> anomalies = List.of(anomaly("payment-service", Severity.CRITICAL), anomaly("inventory-service", Severity.HIGH));
        DashboardService service = new DashboardService(
                metricsService(new RuntimeMetrics(1260, 0, 0, 42, 0, 0, 0, 123, 311)),
                retryService(new RetryAnalysis(42, 0, 1.18, List.of("payment-service"), 0, 0)),
                anomalyRepository(anomalies),
                eventRepository(events),
                new DependencyDiscoveryService(eventRepository(events)));

        DashboardSummary summary = service.getSummary();

        assertEquals(1260L, summary.totalRequests());
        assertEquals(2L, summary.totalServices());
        assertEquals(2L, summary.totalTraces());
        assertEquals(42L, summary.totalRetries());
        assertEquals(2L, summary.totalAnomalies());
        assertEquals(1L, summary.criticalAnomalies());
        assertEquals(1L, summary.highAnomalies());
        assertEquals(0L, summary.healthyServices());
        assertEquals(2L, summary.unhealthyServices());
    }

    private RuntimeMetricsService metricsService(RuntimeMetrics metrics) {
        return new RuntimeMetricsService(null, null) {
            @Override
            public RuntimeMetrics getRuntimeMetrics() {
                return metrics;
            }
        };
    }

    private RetryAnalysisService retryService(RetryAnalysis analysis) {
        return new RetryAnalysisService(null) {
            @Override
            public RetryAnalysis analyze() {
                return analysis;
            }
        };
    }

    private RuntimeEventRepository eventRepository(List<RuntimeEventEntity> events) {
        return repositoryStub(events);
    }

    private RuntimeAnomalyRepository anomalyRepository(List<RuntimeAnomalyEntity> anomalies) {
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

    private RuntimeEventEntity event(String traceId, String serviceName) {
        RuntimeEventEntity entity = new RuntimeEventEntity();
        entity.setEventId(traceId + "-" + serviceName + "-" + Instant.now().toEpochMilli());
        entity.setTraceId(traceId);
        entity.setServiceName(serviceName);
        entity.setTimestamp(Instant.now());
        return entity;
    }

    private RuntimeAnomalyEntity anomaly(String serviceName, Severity severity) {
        RuntimeAnomalyEntity entity = new RuntimeAnomalyEntity();
        entity.setAnomalyId(java.util.UUID.randomUUID());
        entity.setServiceName(serviceName);
        entity.setSeverity(severity);
        entity.setAnomalyType(severity == Severity.CRITICAL ? AnomalyType.SERVICE_SATURATION : AnomalyType.HIGH_LATENCY);
        entity.setDetectedAt(Instant.now());
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
