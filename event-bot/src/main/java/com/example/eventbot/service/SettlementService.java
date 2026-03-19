package com.example.eventbot.service;

import com.example.eventbot.domain.entity.SettlementSettings;
import com.example.eventbot.domain.event.OrderCreatedEvent;
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
import java.time.format.DateTimeFormatter;
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
                .startDate(settings.getStartDate().toString())
                .endDate(settings.getEndDate().toString())
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
        log.info("[정산] 시뮬레이션 시작: {} ~ {} 기간, 총 {} 세트 이벤트 발행 예정 (오류 확률: {}%)", 
            settings.getStartDate(), settings.getEndDate(),
            settings.getEventCount(), settings.getErrorProbability() * 100);
        // ... (rest of startGeneration is same)

        new Thread(() -> {
            try {
                int totalTarget = settings.getEventCount();
                int batchSize = settings.getEventsPerBatch();
                
                for (int i = 0; i < totalTarget; i += batchSize) {
                    if (!settings.isRunning()) break;

                    for (int j = 0; j < batchSize && (i + j) < totalTarget; j++) {
                        // 한 세트의 이벤트 생성 (주문 + 결제)
                        boolean isError = random.nextDouble() < settings.getErrorProbability();
                        generateSettlementEventSet(isError);
                        
                        settings.setProcessedCount(settings.getProcessedCount() + 1);
                        if (isError) {
                            settings.setErrorCount(settings.getErrorCount() + 1);
                        }
                    }

                    if (i + batchSize < totalTarget) {
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

    /**
     * 정산 대조를 위한 이벤트 세트 발행
     * 정상: OrderCreatedEvent(order-created) -> PaymentConfirmedEvent(payment-confirmed) 순차 발행
     * 오류: 확률에 따라 둘 중 하나만 발행하여 대조 실패 유도
     */
    private void generateSettlementEventSet(boolean isError) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long amount = (random.nextInt(10) + 1) * 10000L; // 1만 ~ 10만 랜덤 금액

        // 설정된 시작일과 종료일 사이의 랜덤한 시각 생성
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
                .orderedAt(eventDateTime) // 생성된 랜덤 시각 적용
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
            // 오류 발생 시: 둘 중 하나만 보냄 (대조 불일치 유도)
            if (random.nextBoolean()) {
                log.warn("[정산] 오류 시뮬레이션: [주문 생성] 이벤트만 발행 (결제 누락) - 주문번호: {}, 시각: {}", orderNumber, eventDateTime);
                kafkaProducerService.publishOrderCreated(orderEvent);
            } else {
                log.warn("[정산] 오류 시뮬레이션: [결제 확정] 이벤트만 발행 (주문 누락) - 주문번호: {}, 시각: {}", orderNumber, eventDateTime);
                kafkaProducerService.publishPaymentConfirmed(paymentEvent);
            }
        } else {
            // 정상 발생 시: 순차 발행
            kafkaProducerService.publishOrderCreated(orderEvent);
            kafkaProducerService.publishPaymentConfirmed(paymentEvent);
            log.info("[정산] 정상 이벤트 세트 발행 완료 - 주문번호: {}, 시각: {}", orderNumber, eventDateTime);
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
        
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            settings.setStartDate(LocalDate.parse(request.getStartDate()));
        }
        if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            settings.setEndDate(LocalDate.parse(request.getEndDate()));
        }
        
        log.info("[정산] 설정 업데이트: {} ~ {} 기간, {}개, {}초 간격, 배치당 {}개, 오류확률 {}%", 
            settings.getStartDate(), settings.getEndDate(),
            request.getCount(), request.getInterval(), request.getPerBatch(), request.getErrorProb() * 100);
    }
}
