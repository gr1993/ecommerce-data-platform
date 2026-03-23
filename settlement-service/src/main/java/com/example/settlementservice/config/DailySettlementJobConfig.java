package com.example.settlementservice.config;

import com.example.settlementservice.domain.entity.*;
import com.example.settlementservice.repository.*;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 일일 정산 배치 작업 설정 (Daily Settlement Job Configuration)
 *
 * 전체 일일 정산 프로세스는 3단계(Step)로 구성됩니다:
 *
 * [Step 1: dailySalesReconciliationStep (일일 매출 대조)]
 * 1. 목적: 정상적으로 발생한 주문과 결제 데이터를 매칭하여 매출 원장(Ledger)을 생성합니다.
 * 2. 대상: is_reconciled = false 이면서, targetDate 기준 7일 전 ~ 당일 사이의 주문 데이터.
 * 3. 7일 윈도우: 시스템 장애나 지연으로 인해 뒤늦게 도착한 과거 결제 데이터를 보완하기 위해 7일의 조회 범위를 가집니다.
 *
 * [Step 2: dailyCancelReconciliationStep (일일 취소 대조)]
 * 1. 목적: 주문 취소와 결제 취소 데이터를 매칭하여 취소 원장(Ledger)을 생성합니다.
 * 2. 대상: is_reconciled = false 이면서, targetDate 기준 7일 전 ~ 당일 사이의 주문 취소 데이터.
 *
 * [Step 3: dailyAggregationStep (일별 총계정원장 집계)]
 * 1. 목적: 당일(targetDate) 기준 7일 전부터 오늘까지의 모든 원장(Ledger) 데이터를 타입별로 재합산하여 일별 총계정원장에 기록합니다.
 * 2. 로직: 'SALES'와 'CANCEL' 각각의 총 금액과 건수를 집계하여 daily_general_ledger에 Upsert 합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DailySettlementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    
    private final RawPaymentRepository rawPaymentRepository;
    private final RawPaymentCancelRepository rawPaymentCancelRepository;
    private final LedgerRepository ledgerRepository;
    private final RawOrderRepository rawOrderRepository;
    private final RawOrderCancelRepository rawOrderCancelRepository;
    private final DailyGeneralLedgerRepository dailyGeneralLedgerRepository;

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job dailySettlementJob() {
        return new JobBuilder("dailySettlementJob", jobRepository)
                .start(dailySalesReconciliationStep())
                .next(dailyCancelReconciliationStep())
                .next(dailyAggregationStep())
                .build();
    }

    // --- Step 1: Daily Sales Reconciliation ---

    @Bean
    public Step dailySalesReconciliationStep() {
        return new StepBuilder("dailySalesReconciliationStep", jobRepository)
                .<RawOrder, Ledger>chunk(CHUNK_SIZE, transactionManager)
                .reader(dailySalesReader(null))
                .processor(dailySalesProcessor())
                .writer(dailySalesWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<RawOrder> dailySalesReader(
            @Value("#{jobParameters['targetDate']}") String targetDate) {
        
        LocalDate date = LocalDate.parse(targetDate);
        LocalDateTime windowStart = date.minusDays(7).atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        log.info("[DailySalesReader] Target Date: {}, Window: {} ~ {}", targetDate, windowStart, end);

        return new JpaPagingItemReaderBuilder<RawOrder>()
                .name("dailySalesReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT o FROM RawOrder o WHERE o.isReconciled = false AND o.orderedAt BETWEEN :start AND :end")
                .parameterValues(Map.of("start", windowStart, "end", end))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    @Bean
    public ItemProcessor<RawOrder, Ledger> dailySalesProcessor() {
        return order -> {
            List<RawPayment> payments = rawPaymentRepository.findByOrderNumber(order.getOrderNumber());
            
            if (payments.isEmpty()) {
                order.setReconciled(true);
                order.setReconciliationStatus(ReconciliationStatus.PAYMENT_NOT_FOUND);
                return null;
            }

            RawPayment payment = payments.get(0);
            
            BigDecimal paymentAmount = BigDecimal.valueOf(payment.getPaymentAmount());
            if (order.getTotalPaymentAmount().compareTo(paymentAmount) != 0) {
                order.setReconciled(true);
                order.setReconciliationStatus(ReconciliationStatus.AMOUNT_MISMATCH);
                payment.setReconciled(true);
                payment.setReconciliationStatus(ReconciliationStatus.AMOUNT_MISMATCH);
                rawPaymentRepository.save(payment);
                return null;
            }

            order.setReconciled(true);
            order.setReconciliationStatus(ReconciliationStatus.SUCCESS);
            payment.setReconciled(true);
            payment.setReconciliationStatus(ReconciliationStatus.SUCCESS);
            rawPaymentRepository.save(payment);

            Ledger ledger = ledgerRepository
                    .findByOrderNumberAndLedgerType(order.getOrderNumber(), "SALES")
                    .orElseGet(() -> Ledger.builder()
                            .orderNumber(order.getOrderNumber())
                            .ledgerType("SALES")
                            .build());

            ledger.setAmount(order.getTotalPaymentAmount());
            ledger.setEventAt(order.getOrderedAt());

            return ledger;
        };
    }

    @Bean
    public ItemWriter<Ledger> dailySalesWriter() {
        return items -> {
            for (Ledger ledger : items) {
                ledgerRepository.save(ledger);
            }
        };
    }

    // --- Step 2: Daily Cancel Reconciliation ---

    @Bean
    public Step dailyCancelReconciliationStep() {
        return new StepBuilder("dailyCancelReconciliationStep", jobRepository)
                .<RawOrderCancel, Ledger>chunk(CHUNK_SIZE, transactionManager)
                .reader(dailyCancelReader(null))
                .processor(dailyCancelProcessor())
                .writer(dailyCancelWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<RawOrderCancel> dailyCancelReader(
            @Value("#{jobParameters['targetDate']}") String targetDate) {
        
        LocalDate date = LocalDate.parse(targetDate);
        LocalDateTime windowStart = date.minusDays(7).atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        log.info("[DailyCancelReader] Target Date: {}, Window: {} ~ {}", targetDate, windowStart, end);

        return new JpaPagingItemReaderBuilder<RawOrderCancel>()
                .name("dailyCancelReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT oc FROM RawOrderCancel oc WHERE oc.isReconciled = false AND oc.cancelledAt BETWEEN :start AND :end")
                .parameterValues(Map.of("start", windowStart, "end", end))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    @Bean
    public ItemProcessor<RawOrderCancel, Ledger> dailyCancelProcessor() {
        return orderCancel -> {
            List<RawPaymentCancel> paymentCancels = rawPaymentCancelRepository.findByOrderNumber(orderCancel.getOrderNumber());

            if (paymentCancels.isEmpty()) {
                orderCancel.setReconciled(true);
                orderCancel.setReconciliationStatus(ReconciliationStatus.PAYMENT_CANCEL_NOT_FOUND);
                return null;
            }

            RawPaymentCancel paymentCancel = paymentCancels.get(0);

            orderCancel.setReconciled(true);
            orderCancel.setReconciliationStatus(ReconciliationStatus.SUCCESS);
            paymentCancel.setReconciled(true);
            paymentCancel.setReconciliationStatus(ReconciliationStatus.SUCCESS);
            rawPaymentCancelRepository.save(paymentCancel);

            Ledger ledger = ledgerRepository
                    .findByOrderNumberAndLedgerType(orderCancel.getOrderNumber(), "CANCEL")
                    .orElseGet(() -> Ledger.builder()
                            .orderNumber(orderCancel.getOrderNumber())
                            .ledgerType("CANCEL")
                            .build());

            ledger.setAmount(BigDecimal.valueOf(paymentCancel.getAmount()).negate());
            ledger.setEventAt(orderCancel.getCancelledAt());

            return ledger;
        };
    }

    @Bean
    public ItemWriter<Ledger> dailyCancelWriter() {
        return items -> {
            for (Ledger ledger : items) {
                ledgerRepository.save(ledger);
            }
        };
    }

    // --- Step 3: Daily Aggregation ---

    @Bean
    public Step dailyAggregationStep() {
        return new StepBuilder("dailyAggregationStep", jobRepository)
                .tasklet(dailyAggregationTasklet(null), transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet dailyAggregationTasklet(@Value("#{jobParameters['targetDate']}") String targetDate) {
        return (contribution, chunkContext) -> {
            LocalDate endDate = LocalDate.parse(targetDate);
            LocalDate startDate = endDate.minusDays(7);

            log.info("[DailyAggregation] {} ~ {} 기간 집계 갱신 시작", startDate, endDate);

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = date.atTime(LocalTime.MAX);

                aggregateAndSave(date, "SALES", start, end);
                aggregateAndSave(date, "CANCEL", start, end);
            }

            return RepeatStatus.FINISHED;
        };
    }

    private void aggregateAndSave(LocalDate date, String type, LocalDateTime start, LocalDateTime end) {
        Object resultObj = ledgerRepository.aggregateByTypeAndDate(type, start, end);
        Object[] result = (Object[]) resultObj;

        BigDecimal totalAmount = (result[0] != null) ? (BigDecimal) result[0] : BigDecimal.ZERO;
        int totalCount = (result[1] != null) ? ((Long) result[1]).intValue() : 0;

        DailyGeneralLedger dailyLedger = dailyGeneralLedgerRepository
                .findBySettlementDateAndLedgerType(date, type)
                .orElseGet(() -> DailyGeneralLedger.builder()
                        .settlementDate(date)
                        .ledgerType(type)
                        .description("일일 정산 배치 집계")
                        .build());

        dailyLedger.setTotalAmount(totalAmount);
        dailyLedger.setTotalCount(totalCount);

        dailyGeneralLedgerRepository.save(dailyLedger);
    }
}
