package com.aegis.runtime.anomaly.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aegis.runtime.analytics.dto.ServiceLatency;
import com.aegis.runtime.analytics.service.RuntimeMetricsService;
import com.aegis.runtime.analytics.service.ServiceLatencyService;
import com.aegis.runtime.anomaly.config.AnomalyRuleConfig;
import com.aegis.runtime.anomaly.model.AnomalyType;
import com.aegis.runtime.anomaly.model.Severity;
import com.aegis.runtime.contracts.EventStatus;
import com.aegis.runtime.contracts.EventType;
import com.aegis.runtime.retry.RetryAnalysis;
import com.aegis.runtime.retry.RetryAnalysisService;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import com.aegis.runtime.topology.DependencyDiscoveryService;
import com.aegis.runtime.topology.ServiceDependency;
import com.aegis.runtime.topology.ServiceNode;
import com.aegis.runtime.topology.ServiceTopology;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnomalyDetectionServiceTest {

    @Test
    void detectsHighLatencyRetryStormThreadStarvationAndSaturation() {
        RuntimeEventRepository repository = repositoryStub(sampleEvents());
        AnomalyDetectionService service = new AnomalyDetectionService(
                repository,
                new RuntimeMetricsService(repository, new com.aegis.runtime.analytics.service.LatencyCalculator()),
                serviceLatencyServiceStub(),
                retryAnalysisServiceStub(repository),
                topologyServiceStub(repository),
                new AnomalyRuleConfig());

        var anomalies = service.detect();

        assertTrue(anomalies.stream().anyMatch(anomaly -> anomaly.type() == AnomalyType.HIGH_LATENCY));
        assertTrue(anomalies.stream().anyMatch(anomaly -> anomaly.type() == AnomalyType.RETRY_STORM));
        assertTrue(anomalies.stream().anyMatch(anomaly -> anomaly.type() == AnomalyType.THREAD_STARVATION));
        assertTrue(anomalies.stream().anyMatch(anomaly -> anomaly.type() == AnomalyType.SERVICE_SATURATION));
        assertTrue(anomalies.stream().anyMatch(anomaly -> anomaly.severity() == Severity.CRITICAL));
    }

    @Test
    void returnsEmptyListForEmptyDatabase() {
        RuntimeEventRepository repository = repositoryStub(List.of());
        AnomalyDetectionService service = new AnomalyDetectionService(
                repository,
                new RuntimeMetricsService(repository, new com.aegis.runtime.analytics.service.LatencyCalculator()),
                serviceLatencyServiceStub(),
                retryAnalysisServiceStub(repository),
                topologyServiceStub(repository),
                new AnomalyRuleConfig());

        assertEquals(0, service.detect().size());
    }

    private List<RuntimeEventEntity> sampleEvents() {
        return List.of(
                event("evt-1", "trace-1", "span-1", null, "payment-service", EventType.REQUEST_STARTED, EventStatus.IN_PROGRESS, Duration.ofMillis(900), Instant.now().minusSeconds(120)),
                event("evt-2", "trace-1", "span-2", "span-1", "payment-service", EventType.RETRY_TRIGGERED, EventStatus.IN_PROGRESS, Duration.ofMillis(50), Instant.now().minusSeconds(10)),
                event("evt-3", "trace-1", "span-3", "span-2", "payment-service", EventType.RETRY_TRIGGERED, EventStatus.IN_PROGRESS, Duration.ofMillis(50), Instant.now().minusSeconds(10)),
                event("evt-4", "trace-1", "span-4", "span-1", "payment-service", EventType.ERROR_OCCURRED, EventStatus.FAILED, Duration.ofMillis(10), Instant.now().minusSeconds(10)),
                event("evt-5", "trace-2", "span-5", null, "inventory-service", EventType.REQUEST_STARTED, EventStatus.SUCCESS, Duration.ofMillis(700), Instant.now().minusSeconds(5)));
    }

    private RuntimeEventEntity event(
            String eventId,
            String traceId,
            String spanId,
            String parentSpanId,
            String serviceName,
            EventType eventType,
            EventStatus status,
            Duration latency,
            Instant timestamp) {
        RuntimeEventEntity entity = new RuntimeEventEntity();
        entity.setEventId(eventId);
        entity.setTraceId(traceId);
        entity.setSpanId(spanId);
        entity.setParentSpanId(parentSpanId);
        entity.setServiceName(serviceName);
        entity.setEventType(eventType);
        entity.setStatus(status);
        entity.setLatency(latency);
        entity.setTimestamp(timestamp);
        entity.setThreadName("worker-1");
        entity.setThreadId(11L);
        return entity;
    }

    private ServiceLatencyService serviceLatencyServiceStub() {
        return new ServiceLatencyService(null, null) {
            @Override
            public ServiceLatency getMetrics(String serviceName) {
                if ("payment-service".equals(serviceName)) {
                    return new ServiceLatency(serviceName, 1500, 900, 10, 1200, 100, 200, 300, 99.0, 1.0);
                }
                if ("inventory-service".equals(serviceName)) {
                    return new ServiceLatency(serviceName, 1200, 650, 10, 900, 80, 160, 240, 98.0, 2.0);
                }
                return null;
            }
        };
    }

    private RetryAnalysisService retryAnalysisServiceStub(RuntimeEventRepository repository) {
        return new RetryAnalysisService(repository) {
            @Override
            public RetryAnalysis analyze() {
                return new RetryAnalysis(3, 2, 1.8, List.of("payment-service", "inventory-service"), 3, 2);
            }
        };
    }

    private DependencyDiscoveryService topologyServiceStub(RuntimeEventRepository repository) {
        return new DependencyDiscoveryService(repository) {
            @Override
            public ServiceTopology discover() {
                return new ServiceTopology(
                        List.of(new ServiceNode("payment-service", 1500), new ServiceNode("inventory-service", 1200)),
                        List.of(new ServiceDependency("payment-service", "inventory-service", 1200)));
            }
        };
    }

    @SuppressWarnings("unchecked")
    private RuntimeEventRepository repositoryStub(List<RuntimeEventEntity> events) {
        class Holder {
            final java.util.List<RuntimeEventEntity> store = new java.util.ArrayList<>(events);
        }
        Holder holder = new Holder();
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return switch (method.getName()) {
                    case "findAll" -> holder.store;
                    case "findByTraceIdOrderByTimestampAsc" -> holder.store.stream()
                            .filter(event -> java.util.Objects.equals(event.getTraceId(), args[0]))
                            .toList();
                    case "findByServiceName" -> holder.store.stream()
                            .filter(event -> java.util.Objects.equals(event.getServiceName(), args[0]))
                            .toList();
                    case "save" -> {
                        RuntimeEventEntity entity = (RuntimeEventEntity) args[0];
                        holder.store.add(entity);
                        yield entity;
                    }
                    case "existsByTraceIdAndServiceNameAndAnomalyType" -> false;
                    case "count" -> (long) holder.store.size();
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
