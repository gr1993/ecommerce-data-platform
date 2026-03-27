package com.example.eventbot.service;

import com.example.eventbot.domain.event.OrderCancelledEvent;
import com.example.eventbot.domain.event.OrderCreatedEvent;
import com.example.eventbot.domain.event.PaymentCancelledEvent;
import com.example.eventbot.domain.event.PaymentConfirmedEvent;
import com.example.eventbot.domain.event.UserRegisteredEvent;
import com.example.eventbot.domain.event.InventoryChangedEvent;
import com.example.eventbot.domain.event.PageViewedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

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

    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("Publishing UserRegisteredEvent: {}", event.getUserId());
        kafkaTemplate.send("user_registered", String.valueOf(event.getUserId()), event);
    }

    public void publishInventoryChanged(InventoryChangedEvent event) {
        log.info("Publishing InventoryChangedEvent: {}", event.getProductId());
        kafkaTemplate.send("inventory_changed", String.valueOf(event.getProductId()), event);
    }

    public void publishPageViewed(PageViewedEvent event) {
        log.info("Publishing PageViewedEvent: {}", event.getUserId());
        kafkaTemplate.send("page_viewed", UUID.randomUUID().toString(), event);
    }
}
