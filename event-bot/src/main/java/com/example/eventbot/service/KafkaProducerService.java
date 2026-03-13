package com.example.eventbot.service;

import com.example.eventbot.domain.event.OrderCancelledEvent;
import com.example.eventbot.domain.event.OrderCreatedEvent;
import com.example.eventbot.domain.event.PaymentCancelledEvent;
import com.example.eventbot.domain.event.PaymentConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent: {}", event.getOrderNumber());
        kafkaTemplate.send("order-created", event.getOrderNumber(), event);
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        log.info("Publishing OrderCancelledEvent: {}", event.getOrderNumber());
        kafkaTemplate.send("order-cancelled", event.getOrderNumber(), event);
    }

    public void publishPaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("Publishing PaymentConfirmedEvent: {}", event.getOrderNumber());
        kafkaTemplate.send("payment-confirmed", event.getOrderNumber(), event);
    }

    public void publishPaymentCancelled(PaymentCancelledEvent event) {
        log.info("Publishing PaymentCancelledEvent: {}", event.getOrderNumber());
        kafkaTemplate.send("payment-cancelled", event.getOrderNumber(), event);
    }
}
