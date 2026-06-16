package com.aegis.runtime.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
public class KafkaTopicConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaTopicConfig.class);

    @Bean
    public NewTopic runtimeEventsTopic() {
        log.info("Created Kafka topic: runtime.events");
        return TopicBuilder.name("runtime.events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic runtimeErrorsTopic() {
        log.info("Created Kafka topic: runtime.errors");
        return TopicBuilder.name("runtime.errors")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic runtimeRetriesTopic() {
        log.info("Created Kafka topic: runtime.retries");
        return TopicBuilder.name("runtime.retries")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic runtimeHeartbeatsTopic() {
        log.info("Created Kafka topic: runtime.heartbeats");
        return TopicBuilder.name("runtime.heartbeats")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
