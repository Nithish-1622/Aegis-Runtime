package com.aegis.runtime.ingestor.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.aegis.runtime.contracts.RuntimeEvent;

@Component
public class RetryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RetryEventConsumer.class);

    @KafkaListener(topics = "runtime.retries", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRetry(RuntimeEvent event) {
        log.warn(
                "Retry Event Received: RuntimeEvent(eventId={}, traceId={}, serviceName={}, eventType={})",
                event.eventId(),
                event.traceId(),
                event.serviceName(),
                event.eventType());
    }
}