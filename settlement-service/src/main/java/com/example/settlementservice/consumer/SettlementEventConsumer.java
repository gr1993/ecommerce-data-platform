package com.example.settlementservice.consumer;

import com.example.settlementservice.domain.entity.RawOrder;
import com.example.settlementservice.domain.entity.RawOrderCancel;
import com.example.settlementservice.domain.entity.RawPayment;
import com.example.settlementservice.domain.entity.RawPaymentCancel;
import com.example.settlementservice.domain.event.OrderCancelledEvent;
import com.example.settlementservice.domain.event.OrderCreatedEvent;
import com.example.settlementservice.domain.event.PaymentCancelledEvent;
import com.example.settlementservice.domain.event.PaymentConfirmedEvent;
import com.example.settlementservice.repository.RawOrderCancelRepository;
import com.example.settlementservice.repository.RawOrderRepository;
import com.example.settlementservice.repository.RawPaymentCancelRepository;
import com.example.settlementservice.repository.RawPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventConsumer {

    private final RawOrderRepository rawOrderRepository;
    private final RawPaymentRepository rawPaymentRepository;
    private final RawOrderCancelRepository rawOrderCancelRepository;
    private final RawPaymentCancelRepository rawPaymentCancelRepository;

    /**
     * 주문 생성 이벤트 소비
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            retryTopicSuffix = "-retry",
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "order-created", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeOrderCreated(@Payload OrderCreatedEvent event) {
        log.info("[Consumer] OrderCreated 수신: {}", event.getOrderNumber());
        
        RawOrder entity = RawOrder.builder()
                .orderId(event.getOrderId())
                .orderNumber(event.getOrderNumber())
                .userId(event.getUserId())
                .orderStatus(event.getOrderStatus())
                .totalPaymentAmount(event.getTotalPaymentAmount())
                .orderedAt(event.getOrderedAt())
                .orderItems(new HashMap<>(Map.of("items", Optional.ofNullable(event.getOrderItems()).orElse(List.of())))) // null 체크
                .delivery(event.getDelivery())
                .build();
        
        rawOrderRepository.save(entity);
    }

    /**
     * 결제 확정 이벤트 소비
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            retryTopicSuffix = "-retry",
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "payment-confirmed", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumePaymentConfirmed(@Payload PaymentConfirmedEvent event) {
        log.info("[Consumer] PaymentConfirmed 수신: {}", event.getOrderNumber());

        RawPayment entity = RawPayment.builder()
                .orderNumber(event.getOrderNumber())
                .paymentKey(event.getPaymentKey())
                .paymentMethod(event.getPaymentMethod())
                .paymentAmount(event.getPaymentAmount())
                .paymentStatus(event.getPaymentStatus())
                .paidAt(event.getPaidAt())
                .customerId(event.getCustomerId())
                .build();

        rawPaymentRepository.save(entity);
    }

    /**
     * 주문 취소 이벤트 소비
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            retryTopicSuffix = "-retry",
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "order-cancelled", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeOrderCancelled(@Payload OrderCancelledEvent event) {
        log.info("[Consumer] OrderCancelled 수신: {}", event.getOrderNumber());

        RawOrderCancel entity = RawOrderCancel.builder()
                .orderId(event.getOrderId())
                .orderNumber(event.getOrderNumber())
                .userId(event.getUserId())
                .cancellationReason(event.getCancellationReason())
                .cancelledAt(event.getCancelledAt())
                .cancelledItems(new HashMap<>(Map.of("items", Optional.ofNullable(event.getCancelledItems()).orElse(List.of())))) // null 체크
                .build();

        rawOrderCancelRepository.save(entity);
    }

    /**
     * 결제 취소 이벤트 소비
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            retryTopicSuffix = "-retry",
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "payment-cancelled", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumePaymentCancelled(@Payload PaymentCancelledEvent event) {
        log.info("[Consumer] PaymentCancelled 수신: {}", event.getOrderNumber());

        RawPaymentCancel entity = RawPaymentCancel.builder()
                .orderNumber(event.getOrderNumber())
                .amount(event.getAmount())
                .customerId(event.getCustomerId())
                .cancelReason(event.getCancelReason())
                .cancelledAt(event.getCancelledAt())
                .build();

        rawPaymentCancelRepository.save(entity);
    }

    @DltHandler
    public void handleDlt(Object payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("[Consumer] DLT 메시지 도달 - Topic: {}, Payload: {}", topic, payload);
    }
}
