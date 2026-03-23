package com.example.settlementservice.config;

import com.example.settlementservice.domain.entity.*;
import com.example.settlementservice.repository.*;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 일일 정산 배치 작업 설정 (Daily Settlement Job Configuration)
 *
 * [성능 최적화] 일자별 파티셔닝(Date Partitioning) 적용
 * - 7일 윈도우 전략에 따라 (targetDate - 7일 ~ targetDate) 범위의 데이터를 처리합니다.
 * - 각 날짜별로 별도의 파티션을 생성하여 8개의 스레드가 병렬로 대조 및 집계를 수행합니다.
 * - 특히 집계(Step 3) 단계에서 모든 날짜의 원장을 동시에 합산하므로 전체 처리 시간이 대폭 단축됩니다.
 *
 * 전체 프로세스 (3단계):
 * 1. dailySalesReconciliationStep (Master -> Worker): 일일 매출 대조 병렬 처리
 * 2. dailyCancelReconciliationStep (Master -> Worker): 일일 취소 대조 병렬 처리
 * 3. dailyAggregationStep (Master -> Worker): 일별 총계정원장 집계 병렬 처리
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
    private static final int GRID_SIZE = 8; // 오늘 + 과거 7일

    @Bean
    public Job dailySettlementJob() {
        return new JobBuilder("dailySettlementJob", jobRepository)
                .start(dailySalesMasterStep())
                .next(dailyCancelMasterStep())
                .next(dailyAggregationMasterStep())
                .build();
    }

    // --- 공통 설정 (Partitioner & TaskExecutor) ---

    @Bean
    @JobScope
    public Partitioner settlementPartitioner(@Value("#{jobParameters['targetDate']}") String targetDate) {
        return gridSize -> {
            LocalDate end = LocalDate.parse(targetDate);
            LocalDate start = end.minusDays(7);
            Map<String, ExecutionContext> partitions = new HashMap<>();
            
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                ExecutionContext context = new ExecutionContext();
                context.putString("partitionDate", date.toString());
                partitions.put("partition-" + date, context);
            }
            return partitions;
        };
    }

    @Bean
    public TaskExecutor settlementTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(GRID_SIZE);
        executor.setMaxPoolSize(GRID_SIZE * 2);
        executor.setThreadNamePrefix("settle-worker-");
        executor.initialize();
        return executor;
    }

    // --- Step 1: Daily Sales Reconciliation (Partitioned) ---

    @Bean
    public Step dailySalesMasterStep() {
        return new StepBuilder("dailySalesMasterStep", jobRepository)
                .partitioner("dailySalesWorkerStep", settlementPartitioner(null))
                .step(dailySalesWorkerStep())
                .taskExecutor(settlementTaskExecutor())
                .gridSize(GRID_SIZE)
                .build();
    }

    @Bean
    public Step dailySalesWorkerStep() {
        return new StepBuilder("dailySalesWorkerStep", jobRepository)
                .<RawOrder, Ledger>chunk(CHUNK_SIZE, transactionManager)
                .reader(dailySalesReader(null))
                .processor(dailySalesProcessor())
                .writer(dailySalesWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<RawOrder> dailySalesReader(
            @Value("#{stepExecutionContext['partitionDate']}") String partitionDate) {
        
        LocalDate date = LocalDate.parse(partitionDate);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        log.info("[DailySalesReader] Partition Date: {}", partitionDate);

        return new JpaPagingItemReaderBuilder<RawOrder>()
                .name("dailySalesReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT o FROM RawOrder o WHERE o.isReconciled = false AND o.orderedAt BETWEEN :start AND :end")
                .parameterValues(Map.of("start", start, "end", end))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    @Bean
    @StepScope
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
            rawOrderRepository.save(order);

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

    // --- Step 2: Daily Cancel Reconciliation (Partitioned) ---

    @Bean
    public Step dailyCancelMasterStep() {
        return new StepBuilder("dailyCancelMasterStep", jobRepository)
                .partitioner("dailyCancelWorkerStep", settlementPartitioner(null))
                .step(dailyCancelWorkerStep())
                .taskExecutor(settlementTaskExecutor())
                .gridSize(GRID_SIZE)
                .build();
    }

    @Bean
    public Step dailyCancelWorkerStep() {
        return new StepBuilder("dailyCancelWorkerStep", jobRepository)
                .<RawOrderCancel, Ledger>chunk(CHUNK_SIZE, transactionManager)
                .reader(dailyCancelReader(null))
                .processor(dailyCancelProcessor())
                .writer(dailyCancelWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<RawOrderCancel> dailyCancelReader(
            @Value("#{stepExecutionContext['partitionDate']}") String partitionDate) {
        
        LocalDate date = LocalDate.parse(partitionDate);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        log.info("[DailyCancelReader] Partition Date: {}", partitionDate);

        return new JpaPagingItemReaderBuilder<RawOrderCancel>()
                .name("dailyCancelReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT oc FROM RawOrderCancel oc WHERE oc.isReconciled = false AND oc.cancelledAt BETWEEN :start AND :end")
                .parameterValues(Map.of("start", start, "end", end))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    @Bean
    @StepScope
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
            rawOrderCancelRepository.save(orderCancel);

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

    // --- Step 3: Daily Aggregation (Partitioned) ---

    @Bean
    public Step dailyAggregationMasterStep() {
        return new StepBuilder("dailyAggregationMasterStep", jobRepository)
                .partitioner("dailyAggregationWorkerStep", settlementPartitioner(null))
                .step(dailyAggregationWorkerStep())
                .taskExecutor(settlementTaskExecutor())
                .gridSize(GRID_SIZE)
                .build();
    }

    @Bean
    public Step dailyAggregationWorkerStep() {
        return new StepBuilder("dailyAggregationWorkerStep", jobRepository)
                .tasklet(dailyAggregationTasklet(null), transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet dailyAggregationTasklet(@Value("#{stepExecutionContext['partitionDate']}") String partitionDate) {
        return (contribution, chunkContext) -> {
            LocalDate date = LocalDate.parse(partitionDate);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            log.info("[DailyAggregation] 파티션 날짜 집계 시작: {}", partitionDate);

            // 각 날짜별 SALES 집계 및 Upsert
            aggregateAndSave(date, "SALES", start, end);
            
            // 각 날짜별 CANCEL 집계 및 Upsert
            aggregateAndSave(date, "CANCEL", start, end);

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
                        .description("일일 정산 배치 집계 (병렬)")
                        .build());

        dailyLedger.setTotalAmount(totalAmount);
        dailyLedger.setTotalCount(totalCount);

        dailyGeneralLedgerRepository.save(dailyLedger);
    }
}
