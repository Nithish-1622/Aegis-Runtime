package com.aegis.runtime.anomaly.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aegis.runtime.anomaly.config.AnomalyRuleConfig;
import com.aegis.runtime.anomaly.dto.RuntimeAnomaly;
import com.aegis.runtime.anomaly.dto.RuntimeAnomalyResponse;
import com.aegis.runtime.anomaly.entity.RuntimeAnomalyEntity;
import com.aegis.runtime.anomaly.model.AnomalyType;
import com.aegis.runtime.anomaly.model.Severity;
import com.aegis.runtime.anomaly.repository.RuntimeAnomalyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnomalyPipelineServiceTest {

    @Test
    void preventsDuplicateAnomaliesAndBuildsResponse() {
        List<RuntimeAnomaly> anomalies = List.of(
                anomaly("trace-1", "payment-service", AnomalyType.HIGH_LATENCY, Severity.MEDIUM),
                anomaly("trace-1", "payment-service", AnomalyType.HIGH_LATENCY, Severity.MEDIUM));

        RuntimeAnomalyRepository repository = repositoryStub(new ArrayList<>());
        AnomalyPipelineService pipelineService = new AnomalyPipelineService(
                new AnomalyDetectionServiceStub(anomalies),
                repository,
                new ObjectMapper());

        RuntimeAnomalyResponse response = pipelineService.detectAndPersist();

        assertEquals(1L, response.totalAnomalies());
        assertEquals(1L, response.medium());
    }

    @Test
    void returnsEmptyResponseForNoAnomalies() {
        RuntimeAnomalyRepository repository = repositoryStub(new ArrayList<>());
        AnomalyPipelineService pipelineService = new AnomalyPipelineService(
                new AnomalyDetectionServiceStub(List.of()),
                repository,
                new ObjectMapper());

        RuntimeAnomalyResponse response = pipelineService.detectAndPersist();

        assertEquals(0L, response.totalAnomalies());
        assertEquals(0L, response.critical());
        assertEquals(0L, response.high());
        assertEquals(0L, response.medium());
        assertEquals(0L, response.low());
        assertEquals(0, response.anomalies().size());
    }

    private RuntimeAnomaly anomaly(String traceId, String serviceName, AnomalyType type, Severity severity) {
        return new RuntimeAnomaly(
                UUID.randomUUID(),
                traceId,
                serviceName,
                type,
                severity,
                "Observed latency exceeded configured threshold.",
                Instant.parse("2026-06-20T10:45:30Z"),
                java.util.Map.of("observedLatency", 812, "threshold", 500));
    }

    private RuntimeAnomalyRepository repositoryStub(List<RuntimeAnomalyEntity> store) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return switch (method.getName()) {
                    case "findAll" -> store;
                    case "findByTraceId" -> store.stream()
                            .filter(entity -> java.util.Objects.equals(entity.getTraceId(), args[0]))
                            .toList();
                    case "existsByTraceIdAndServiceNameAndAnomalyType" -> false;
                    case "save" -> {
                        RuntimeAnomalyEntity entity = (RuntimeAnomalyEntity) args[0];
                        store.add(entity);
                        yield entity;
                    }
                    case "count" -> (long) store.size();
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

    private static class AnomalyDetectionServiceStub extends AnomalyDetectionService {

        private final List<RuntimeAnomaly> anomalies;

        private AnomalyDetectionServiceStub(List<RuntimeAnomaly> anomalies) {
            super(null, null, null, null, null, new AnomalyRuleConfig());
            this.anomalies = anomalies;
        }

        @Override
        public List<RuntimeAnomaly> detect() {
            return anomalies;
        }
    }
}
