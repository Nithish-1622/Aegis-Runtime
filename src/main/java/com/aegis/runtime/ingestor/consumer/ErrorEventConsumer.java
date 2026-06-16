package com.aegis.runtime.ingestor.consumer;

import com.aegis.runtime.contracts.RuntimeEvent;
import com.aegis.runtime.storage.service.RuntimeEventPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ErrorEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ErrorEventConsumer.class);
    private final RuntimeEventPersistenceService runtimeEventPersistenceService;

    public ErrorEventConsumer(RuntimeEventPersistenceService runtimeEventPersistenceService) {
        this.runtimeEventPersistenceService = runtimeEventPersistenceService;
    }

    @KafkaListener(topics = "runtime.errors", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeError(RuntimeEvent event) {
        log.error("EVENT RECEIVED -> {}", event.eventId());
        runtimeEventPersistenceService.save(event);
    }
}
