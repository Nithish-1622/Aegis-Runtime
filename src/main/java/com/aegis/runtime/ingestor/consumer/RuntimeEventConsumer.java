package com.aegis.runtime.ingestor.consumer;

import com.aegis.runtime.contracts.RuntimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RuntimeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RuntimeEventConsumer.class);

    @KafkaListener(topics = "runtime.events", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(RuntimeEvent event) {
        log.info(
                "Received Runtime Event:\nRuntimeEvent(eventId={}, traceId={}, serviceName={}, eventType={})",
                event.eventId(),
                event.traceId(),
                event.serviceName(),
                event.eventType());
    }
}