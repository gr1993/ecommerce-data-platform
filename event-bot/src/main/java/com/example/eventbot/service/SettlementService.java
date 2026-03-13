package com.example.eventbot.service;

import com.example.eventbot.domain.entity.SettlementSettings;
import com.example.eventbot.domain.event.OrderCancelledEvent;
import com.example.eventbot.domain.event.OrderCreatedEvent;
import com.example.eventbot.domain.event.PaymentCancelledEvent;
import com.example.eventbot.domain.event.PaymentConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {
    private final KafkaProducerService kafkaProducerService;
    private final SettlementSettings settings = new SettlementSettings();
    private final Random random = new Random();

    public SettlementSettings getSettings() {
        return settings;
    }

    public void startGeneration() {
        if (settings.isRunning()) return;

        settings.setRunning(true);
        log.info("[정산] 이벤트 생성 시작: 토픽={}, 횟수={}, 오류확률={}",
                settings.getTopic(), settings.getEventCount(), settings.getErrorProbability());

        new Thread(() -> {
            try {
                for (int i = 0; i < settings.getEventCount(); i++) {
                    if (!settings.isRunning()) break;

                    publishRandomEvent();

                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                settings.setRunning(false);
                log.info("[정산] 이벤트 생성 완료.");
            }
        }).start();
    }

    private void publishRandomEvent() {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int type = random.nextInt(4);

        switch (type) {
            case 0 -> {
                OrderCreatedEvent event = OrderCreatedEvent.builder()
                        .orderId(random.nextLong(10000))
                        .orderNumber(orderNumber)
                        .userId(random.nextLong(1000))
                        .orderStatus("CREATED")
                        .totalProductAmount(new BigDecimal("50000"))
                        .totalDiscountAmount(new BigDecimal("5000"))
                        .totalPaymentAmount(new BigDecimal("45000"))
                        .orderedAt(LocalDateTime.now())
                        .orderItems(List.of(
                                OrderCreatedEvent.OrderItemSnapshot.builder()
                                        .orderItemId(random.nextLong(100000))
                                        .productId(101L)
                                        .skuId(201L)
                                        .productName("테스트 상품")
                                        .productCode("PROD-001")
                                        .quantity(1)
                                        .unitPrice(new BigDecimal("50000"))
                                        .totalPrice(new BigDecimal("50000"))
                                        .build()
                        ))
                        .delivery(OrderCreatedEvent.DeliverySnapshot.builder()
                                .receiverName("홍길동")
                                .receiverPhone("010-1234-5678")
                                .zipcode("12345")
                                .address("서울시 강남구")
                                .addressDetail("테헤란로 123")
                                .deliveryMemo("문 앞에 놓아주세요")
                                .build())
                        .build();
                kafkaProducerService.publishOrderCreated(event);
            }
            case 1 -> {
                OrderCancelledEvent event = OrderCancelledEvent.builder()
                        .orderId(random.nextLong(10000))
                        .orderNumber(orderNumber)
                        .cancellationReason("USER_REQUEST")
                        .userId(random.nextLong(1000))
                        .cancelledAt(LocalDateTime.now())
                        .cancelledItems(List.of(
                                OrderCancelledEvent.CancelledOrderItem.builder()
                                        .orderItemId(random.nextLong(100000))
                                        .productId(101L)
                                        .skuId(201L)
                                        .productName("테스트 상품")
                                        .productCode("PROD-001")
                                        .quantity(1)
                                        .unitPrice(new BigDecimal("50000"))
                                        .totalPrice(new BigDecimal("50000"))
                                        .build()
                        ))
                        .build();
                kafkaProducerService.publishOrderCancelled(event);
            }
            case 2 -> {
                PaymentConfirmedEvent event = PaymentConfirmedEvent.builder()
                        .orderNumber(orderNumber)
                        .paymentKey(UUID.randomUUID().toString())
                        .paymentMethod("CARD")
                        .paymentAmount(45000L)
                        .paymentStatus("DONE")
                        .paidAt(LocalDateTime.now().toString())
                        .customerId("CUST-" + random.nextInt(1000))
                        .build();
                kafkaProducerService.publishPaymentConfirmed(event);
            }
            case 3 -> {
                PaymentCancelledEvent event = PaymentCancelledEvent.builder()
                        .orderNumber(orderNumber)
                        .amount(45000L)
                        .customerId("CUST-" + random.nextInt(1000))
                        .cancelReason("ORDER_CANCELLED")
                        .cancelledAt(LocalDateTime.now())
                        .build();
                kafkaProducerService.publishPaymentCancelled(event);
            }
        }
    }

    public void stopGeneration() {
        settings.setRunning(false);
        log.info("[정산] 이벤트 생성 사용자 중단.");
    }

    public void updateSettings(String topic, int count, double errorProb) {
        settings.setTopic(topic);
        settings.setEventCount(count);
        settings.setErrorProbability(errorProb);
        log.info("[정산] 설정 변경: 토픽={}, 횟수={}, 오류확률={}", topic, count, errorProb);
    }
}
