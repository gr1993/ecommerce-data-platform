package com.example.eventbot.service;

import com.example.eventbot.domain.entity.SettlementSettings;
import com.example.eventbot.domain.event.OrderCancelledEvent;
import com.example.eventbot.domain.event.OrderCreatedEvent;
import com.example.eventbot.domain.event.PaymentCancelledEvent;
import com.example.eventbot.domain.event.PaymentConfirmedEvent;
import com.example.eventbot.dto.request.SettlementSettingsRequest;
import com.example.eventbot.dto.response.SettlementSettingsResponse;
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

    public SettlementSettingsResponse getSettingsResponse() {
        return SettlementSettingsResponse.builder()
                .count(settings.getEventCount())
                .interval(settings.getIntervalSeconds())
                .perBatch(settings.getEventsPerBatch())
                .errorProb(settings.getErrorProbability())
                .running(settings.isRunning())
                .processedCount(settings.getProcessedCount())
                .errorCount(settings.getErrorCount())
                .totalTarget(settings.getTotalTargetCount())
                .build();
    }

    public void startGeneration() {
        if (settings.isRunning()) return;

        settings.resetCounts();
        settings.setRunning(true);
        log.info("[정산] 시뮬레이션 시작: 총 {}개 이벤트 발행 예정", settings.getTotalTargetCount());

        new Thread(() -> {
            try {
                for (int i = 0; i < settings.getEventCount(); i++) {
                    if (!settings.isRunning()) break;

                    for (int j = 0; j < settings.getEventsPerBatch(); j++) {
                        boolean isError = random.nextDouble() < settings.getErrorProbability();
                        publishRandomEvent(isError);
                        
                        settings.setProcessedCount(settings.getProcessedCount() + 1);
                        if (isError) {
                            settings.setErrorCount(settings.getErrorCount() + 1);
                        }
                    }

                    if (i < settings.getEventCount() - 1) {
                        Thread.sleep(settings.getIntervalSeconds() * 1000L);
                    }
                }
            } catch (InterruptedException e) {
                log.warn("[정산] 시뮬레이션 쓰레드 중단됨.");
                Thread.currentThread().interrupt();
            } finally {
                settings.setRunning(false);
                log.info("[정산] 시뮬레이션 종료. 최종 발행: {}/{}", settings.getProcessedCount(), settings.getTotalTargetCount());
            }
        }).start();
    }

    private void publishRandomEvent(boolean isError) {
        // 오류 데이터 시나리오: 1. 주문번호 누락(Null) 2. 잘못된 금액(음수) 3. 필수 상태값 누락
        String orderNumber = (isError && random.nextInt(3) == 0) ? null : "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int type = random.nextInt(4);

        switch (type) {
            case 0 -> {
                BigDecimal amount = (isError && random.nextInt(3) == 1) ? new BigDecimal("-50000") : new BigDecimal("50000");
                OrderCreatedEvent event = OrderCreatedEvent.builder()
                        .orderId(random.nextLong(10000))
                        .orderNumber(orderNumber)
                        .userId(random.nextLong(1000))
                        .orderStatus("CREATED")
                        .totalPaymentAmount(amount)
                        .orderedAt(LocalDateTime.now())
                        .build();
                kafkaProducerService.publishOrderCreated(event);
            }
            case 1 -> {
                OrderCancelledEvent event = OrderCancelledEvent.builder()
                        .orderId(isError ? null : random.nextLong(10000)) // 필수 ID 누락 오류
                        .orderNumber(orderNumber)
                        .cancellationReason("USER_REQUEST")
                        .cancelledAt(LocalDateTime.now())
                        .build();
                kafkaProducerService.publishOrderCancelled(event);
            }
            case 2 -> {
                PaymentConfirmedEvent event = PaymentConfirmedEvent.builder()
                        .orderNumber(orderNumber)
                        .paymentKey(UUID.randomUUID().toString())
                        .paymentAmount(isError ? 0L : 45000L) // 결제 금액 0원 오류
                        .paymentStatus("DONE")
                        .paidAt(LocalDateTime.now().toString())
                        .build();
                kafkaProducerService.publishPaymentConfirmed(event);
            }
            case 3 -> {
                PaymentCancelledEvent event = PaymentCancelledEvent.builder()
                        .orderNumber(orderNumber)
                        .amount(45000L)
                        .cancelReason(isError ? "" : "ORDER_CANCELLED") // 사유 누락
                        .cancelledAt(LocalDateTime.now())
                        .build();
                kafkaProducerService.publishPaymentCancelled(event);
            }
        }
    }

    public void stopGeneration() {
        settings.setRunning(false);
        log.info("[정산] 시뮬레이션 중단 요청됨.");
    }

    public void updateSettings(SettlementSettingsRequest request) {
        settings.setEventCount(request.getCount());
        settings.setIntervalSeconds(request.getInterval());
        settings.setEventsPerBatch(request.getPerBatch());
        settings.setErrorProbability(request.getErrorProb());
    }
}
