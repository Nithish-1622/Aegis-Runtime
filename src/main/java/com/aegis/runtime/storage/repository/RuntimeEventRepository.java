package com.aegis.runtime.storage.repository;

import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuntimeEventRepository extends JpaRepository<RuntimeEventEntity, String> {

    List<RuntimeEventEntity> findByTraceIdOrderByTimestampAsc(String traceId);

    List<RuntimeEventEntity> findByServiceName(String serviceName);
}
