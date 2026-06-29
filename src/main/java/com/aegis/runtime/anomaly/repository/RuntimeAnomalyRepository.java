package com.aegis.runtime.anomaly.repository;

import com.aegis.runtime.anomaly.entity.RuntimeAnomalyEntity;
import com.aegis.runtime.anomaly.model.AnomalyType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuntimeAnomalyRepository extends JpaRepository<RuntimeAnomalyEntity, UUID> {

    List<RuntimeAnomalyEntity> findByTraceId(String traceId);

    boolean existsByTraceIdAndServiceNameAndAnomalyType(String traceId, String serviceName, AnomalyType anomalyType);
}
