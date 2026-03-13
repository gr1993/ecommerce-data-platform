package com.example.eventbot.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order-created").partitions(3).replicas(1).build();
    }
    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name("order-cancelled").partitions(3).replicas(1).build();
    }
    @Bean
    public NewTopic paymentConfirmedTopic() {
        return TopicBuilder.name("payment-confirmed").partitions(3).replicas(1).build();
    }
    @Bean
    public NewTopic paymentCancelledTopic() {
        return TopicBuilder.name("payment-cancelled").partitions(3).replicas(1).build();
    }
}
