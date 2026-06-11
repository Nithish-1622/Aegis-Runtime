package com.aegis.runtime.ingestor.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.aegis.runtime.contracts.RuntimeEvent;

@Component
public class HeartbeatConsumer {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatConsumer.class);

    @KafkaListener(topics = "runtime.heartbeats", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeHeartbeat(RuntimeEvent event) {
        log.info(
                "Heartbeat Event Received: RuntimeEvent(eventId={}, serviceName={}, instanceId={}, eventType={})",
                event.eventId(),
                event.serviceName(),
                event.instanceId(),
                event.eventType());
    }
}