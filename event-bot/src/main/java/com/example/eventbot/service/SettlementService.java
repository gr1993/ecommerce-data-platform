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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
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
                .cancelCountTarget(settings.getCancelCountTarget())
                .startDate(settings.getStartDate().toString())
                .endDate(settings.getEndDate().toString())
                .running(settings.isRunning())
                .processedCount(settings.getProcessedCount())
                .errorCount(settings.getErrorCount())
                .processedCancelCount(settings.getProcessedCancelCount())
                .totalTarget(settings.getTotalTargetCount())
                .build();
    }

    public void startGeneration() {
        if (settings.isRunning()) return;

        settings.resetCounts();
        settings.setRunning(true);
        
        log.info("[정산] 시뮬레이션 시작: 전체={}건 (취소={}건 포함, 나머지 중 오류확률={}%)", 
            settings.getEventCount(), settings.getCancelCountTarget(), settings.getErrorProbability() * 100);

        new Thread(() -> {
            try {
                int totalTarget = settings.getEventCount();
                int cancelTarget = Math.min(settings.getCancelCountTarget(), totalTarget);
                
                for (int i = 0; i < totalTarget; i++) {
                    if (!settings.isRunning()) break;

                    boolean isError = false;
                    boolean shouldCancel = false;

                    // 1. 앞부분 루프는 '취소될 주문' 세트 생성 (정상 주문/결제 후 취소 이벤트 연달아 발행)
                    if (i < cancelTarget) {
                        shouldCancel = true;
                    } 
                    // 2. 뒷부분 루프는 '일반 주문' 또는 '오류 주문' 생성
                    else {
                        isError = random.nextDouble() < settings.getErrorProbability();
                    }

                    generateSettlementEventSet(isError, shouldCancel);
                    
                    settings.setProcessedCount(settings.getProcessedCount() + 1);
                    if (isError) {
                        settings.setErrorCount(settings.getErrorCount() + 1);
                    }
                    if (shouldCancel) {
                        settings.setProcessedCancelCount(settings.getProcessedCancelCount() + 1);
                    }

                    if (i < totalTarget - 1 && (i + 1) % settings.getEventsPerBatch() == 0) {
                        Thread.sleep(settings.getIntervalSeconds() * 1000L);
                    }
                }
            } catch (InterruptedException e) {
                log.warn("[정산] 시뮬레이션 쓰레드 중단됨.");
                Thread.currentThread().interrupt();
            } finally {
                settings.setRunning(false);
                log.info("[정산] 시뮬레이션 종료. 최종 발행 세트: {}/{}", settings.getProcessedCount(), settings.getEventCount());
            }
        }).start();
    }

    private void generateSettlementEventSet(boolean isError, boolean shouldCancel) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long amount = (random.nextInt(10) + 1) * 10000L;

        long daysBetween = ChronoUnit.DAYS.between(settings.getStartDate(), settings.getEndDate());
        LocalDate randomDate = settings.getStartDate().plusDays(random.nextLong(daysBetween + 1));
        LocalTime randomTime = LocalTime.of(random.nextInt(24), random.nextInt(60), random.nextInt(60));
        LocalDateTime eventDateTime = LocalDateTime.of(randomDate, randomTime).truncatedTo(ChronoUnit.SECONDS);

        OrderCreatedEvent orderEvent = OrderCreatedEvent.builder()
                .orderId(random.nextLong(1000000))
                .orderNumber(orderNumber)
                .userId(random.nextLong(10000))
                .orderStatus("CONFIRMED")
                .totalPaymentAmount(new BigDecimal(amount))
                .orderedAt(eventDateTime)
                .build();

        PaymentConfirmedEvent paymentEvent = PaymentConfirmedEvent.builder()
                .orderNumber(orderNumber)
                .paymentKey("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .paymentMethod("CARD")
                .paymentAmount(amount)
                .paymentStatus("DONE")
                .paidAt(eventDateTime)
                .customerId(String.valueOf(random.nextLong(10000)))
                .build();

        if (isError) {
            if (random.nextBoolean()) {
                log.warn("[정산] 오류 시뮬레이션: [주문 생성] 이벤트만 발행 (결제 누락) - 주문번호: {}", orderNumber);
                kafkaProducerService.publishOrderCreated(orderEvent);
            } else {
                log.warn("[정산] 오류 시뮬레이션: [결제 확정] 이벤트만 발행 (주문 누락) - 주문번호: {}", orderNumber);
                kafkaProducerService.publishPaymentConfirmed(paymentEvent);
            }
        } else {
            kafkaProducerService.publishOrderCreated(orderEvent);
            kafkaProducerService.publishPaymentConfirmed(paymentEvent);

            if (shouldCancel) {
                LocalDateTime cancelDateTime = eventDateTime.plusMinutes(random.nextInt(120) + 1);
                
                OrderCancelledEvent cancelOrder = OrderCancelledEvent.builder()
                        .orderId(orderEvent.getOrderId())
                        .orderNumber(orderNumber)
                        .cancellationReason("USER_REQUEST")
                        .userId(orderEvent.getUserId())
                        .cancelledAt(cancelDateTime)
                        .build();

                PaymentCancelledEvent cancelPayment = PaymentCancelledEvent.builder()
                        .orderNumber(orderNumber)
                        .amount(amount)
                        .customerId(paymentEvent.getCustomerId())
                        .cancelReason("고객 변심")
                        .cancelledAt(cancelDateTime)
                        .build();

                kafkaProducerService.publishOrderCancelled(cancelOrder);
                kafkaProducerService.publishPaymentCancelled(cancelPayment);
                log.info("[정산] 취소 세트 발행 완료 (주문+결제+취소) - 주문번호: {}, 시각: {}", orderNumber, cancelDateTime);
            } else {
                log.info("[정산] 정상 주문 발행 완료 - 주문번호: {}, 시각: {}", orderNumber, eventDateTime);
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
        settings.setCancelCountTarget(request.getCancelCount());
        
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            settings.setStartDate(LocalDate.parse(request.getStartDate()));
        }
        if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            settings.setEndDate(LocalDate.parse(request.getEndDate()));
        }
        
        log.info("[정산] 설정 업데이트: {} ~ {} 기간, {}개, {}초 간격, 배치당 {}개, 오류확률 {}%, 취소목표 {}건", 
            settings.getStartDate(), settings.getEndDate(),
            request.getCount(), request.getInterval(), request.getPerBatch(), 
            request.getErrorProb() * 100, request.getCancelCount());
    }
}
