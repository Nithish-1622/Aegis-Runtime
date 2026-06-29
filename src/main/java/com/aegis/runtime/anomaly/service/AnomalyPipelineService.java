package com.aegis.runtime.anomaly.service;

import com.aegis.runtime.anomaly.dto.RuntimeAnomaly;
import com.aegis.runtime.anomaly.dto.RuntimeAnomalyResponse;
import com.aegis.runtime.anomaly.entity.RuntimeAnomalyEntity;
import com.aegis.runtime.anomaly.model.Severity;
import com.aegis.runtime.anomaly.repository.RuntimeAnomalyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AnomalyPipelineService {

    private final AnomalyDetectionService anomalyDetectionService;
    private final RuntimeAnomalyRepository runtimeAnomalyRepository;
    private final ObjectMapper objectMapper;

    public AnomalyPipelineService(
            AnomalyDetectionService anomalyDetectionService,
            RuntimeAnomalyRepository runtimeAnomalyRepository,
            ObjectMapper objectMapper) {
        this.anomalyDetectionService = anomalyDetectionService;
        this.runtimeAnomalyRepository = runtimeAnomalyRepository;
        this.objectMapper = objectMapper;
    }

    public RuntimeAnomalyResponse detectAndPersist() {
        List<RuntimeAnomaly> anomalies = anomalyDetectionService.detect();
        for (RuntimeAnomaly anomaly : anomalies) {
            persistIfChanged(anomaly);
        }

        List<RuntimeAnomaly> persisted = runtimeAnomalyRepository.findAll().stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(RuntimeAnomaly::detectedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        long critical = persisted.stream().filter(anomaly -> anomaly.severity() == Severity.CRITICAL).count();
        long high = persisted.stream().filter(anomaly -> anomaly.severity() == Severity.HIGH).count();
        long medium = persisted.stream().filter(anomaly -> anomaly.severity() == Severity.MEDIUM).count();
        long low = persisted.stream().filter(anomaly -> anomaly.severity() == Severity.LOW).count();

        return new RuntimeAnomalyResponse(persisted.size(), critical, high, medium, low, persisted);
    }

    private void persistIfChanged(RuntimeAnomaly anomaly) {
        List<RuntimeAnomalyEntity> existing = runtimeAnomalyRepository.findByTraceId(anomaly.traceId());
        boolean duplicate = existing.stream().anyMatch(entity ->
                entity.getServiceName() != null
                        && entity.getServiceName().equals(anomaly.serviceName())
                        && entity.getAnomalyType() == anomaly.type()
                        && entity.getSeverity() == anomaly.severity()
                        && java.util.Objects.equals(entity.getDescription(), anomaly.description()));
        if (duplicate) {
            return;
        }

        RuntimeAnomalyEntity entity = new RuntimeAnomalyEntity();
        entity.setAnomalyId(anomaly.anomalyId());
        entity.setTraceId(anomaly.traceId());
        entity.setServiceName(anomaly.serviceName());
        entity.setAnomalyType(anomaly.type());
        entity.setSeverity(anomaly.severity());
        entity.setDescription(anomaly.description());
        entity.setDetectedAt(anomaly.detectedAt());
        entity.setMetadataJson(writeJson(anomaly.metadata()));
        runtimeAnomalyRepository.save(entity);
    }

    private RuntimeAnomaly toDto(RuntimeAnomalyEntity entity) {
        return new RuntimeAnomaly(
                entity.getAnomalyId(),
                entity.getTraceId(),
                entity.getServiceName(),
                entity.getAnomalyType(),
                entity.getSeverity(),
                entity.getDescription(),
                entity.getDetectedAt(),
                readMetadata(entity.getMetadataJson()));
    }

    private String writeJson(Object metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Failed to serialize anomaly metadata", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> readMetadata(String metadataJson) {
        try {
            if (metadataJson == null || metadataJson.isBlank()) {
                return java.util.Map.of();
            }
            return objectMapper.readValue(metadataJson, java.util.Map.class);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Failed to deserialize anomaly metadata", exception);
        }
    }
}
