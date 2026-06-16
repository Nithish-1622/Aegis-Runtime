package com.aegis.runtime.storage.service;

import com.aegis.runtime.contracts.RuntimeEvent;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.mapper.RuntimeEventMapper;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RuntimeEventPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(RuntimeEventPersistenceService.class);

    private final RuntimeEventMapper runtimeEventMapper;
    private final RuntimeEventRepository runtimeEventRepository;

    public RuntimeEventPersistenceService(
            RuntimeEventMapper runtimeEventMapper,
            RuntimeEventRepository runtimeEventRepository) {
        this.runtimeEventMapper = runtimeEventMapper;
        this.runtimeEventRepository = runtimeEventRepository;
    }

    public void save(RuntimeEvent runtimeEvent) {
        try {
            log.info("Persisting Event -> {}", runtimeEvent.eventId());
            RuntimeEventEntity entity = runtimeEventMapper.toEntity(runtimeEvent);
            runtimeEventRepository.save(entity);
            log.info("Persisted Event -> {}", runtimeEvent.eventId());
        } catch (Exception exception) {
            log.error("Failed to persist runtime event {}", runtimeEvent.eventId(), exception);
        }
    }
}
