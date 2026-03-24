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
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
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
 * - 특히 집계(Step 5) 단계에서 모든 날짜의 원장을 동시에 합산하므로 전체 처리 시간이 대폭 단축됩니다.
 *
 * 전체 프로세스 (5단계):
 * 1. dailySalesMasterStep: 주문 기준 결제 대조 (Order -> Payment)
 * 2. dailyPaymentMasterStep: 결제 기준 주문 대조 (Payment -> Order) - 주문 누락 결제 확인
 * 3. dailyCancelMasterStep: 주문취소 기준 결제취소 대조 (OrderCancel -> PaymentCancel)
 * 4. dailyPaymentCancelMasterStep: 결제취소 기준 주문취소 대조 (PaymentCancel -> OrderCancel) - 취소 누락 확인
 * 5. dailyAggregationMasterStep: 일별 총계정원장 집계 (Aggregation)
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
                .next(dailyPaymentMasterStep())
                .next(dailyCancelMasterStep())
                .next(dailyPaymentCancelMasterStep())
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

    // --- Step 1: Daily Sales Reconciliation (Order -> Payment) ---

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
    public JpaCursorItemReader<RawOrder> dailySalesReader(
            @Value("#{stepExecutionContext['partitionDate']}") String partitionDate) {
        
        LocalDate date = LocalDate.parse(partitionDate);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        return new JpaCursorItemReaderBuilder<RawOrder>()
                .name("dailySalesReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT o FROM RawOrder o WHERE o.isReconciled = false AND o.orderedAt BETWEEN :start AND :end")
                .parameterValues(Map.of("start", start, "end", end))
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
                rawOrderRepository.save(order);
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
                rawOrderRepository.save(order);
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

    // --- Step 2: Daily Payment Reconciliation (Payment -> Order) ---
    // 주문 기반 대조(Step 1) 후에 실행하여, 여전히 대조되지 않은 결제 건들을 처리합니다.

    @Bean
    public Step dailyPaymentMasterStep() {
        return new StepBuilder("dailyPaymentMasterStep", jobRepository)
                .partitioner("dailyPaymentWorkerStep", settlementPartitioner(null))
                .step(dailyPaymentWorkerStep())
                .taskExecutor(settlementTaskExecutor())
                .gridSize(GRID_SIZE)
                .build();
    }

    @Bean
    public Step dailyPaymentWorkerStep() {
        return new StepBuilder("dailyPaymentWorkerStep", jobRepository)
                .<RawPayment, RawPayment>chunk(CHUNK_SIZE, transactionManager)
                .reader(dailyPaymentReader(null))
                .processor(dailyPaymentProcessor())
                .writer(dailyPaymentWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaCursorItemReader<RawPayment> dailyPaymentReader(
            @Value("#{stepExecutionContext['partitionDate']}") String partitionDate) {
        
        LocalDate date = LocalDate.parse(partitionDate);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        return new JpaCursorItemReaderBuilder<RawPayment>()
                .name("dailyPaymentReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT p FROM RawPayment p WHERE p.isReconciled = false AND p.paidAt BETWEEN :start AND :end")
                .parameterValues(Map.of("start", start, "end", end))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<RawPayment, RawPayment> dailyPaymentProcessor() {
        return payment -> {
            // 결제 번호(orderNumber)로 주문 데이터를 직접 조회 (ID가 아닌 비즈니스 키 기준)
            // RawOrderRepository에 findByOrderNumber가 없는 경우 JpaRepository의 ID 조회를 쓰거나 쿼리 필요
            // 여기서는 rawOrderRepository.findById(new RawOrderId(payment.getOrderNumber(), ...)) 방식 대신 간접 조회로 가정
            boolean orderExists = rawOrderRepository.existsById(new RawOrderId(payment.getOrderNumber(), payment.getPaidAt()));
            
            if (!orderExists) {
                payment.setReconciled(true);
                payment.setReconciliationStatus(ReconciliationStatus.ORDER_NOT_FOUND);
                return payment;
            }

            // 만약 여기서 발견된다면 (Step 1에서 누락된 경우), 상태만 업데이트
            payment.setReconciled(true);
            payment.setReconciliationStatus(ReconciliationStatus.SUCCESS);
            return payment;
        };
    }

    @Bean
    public ItemWriter<RawPayment> dailyPaymentWriter() {
        return items -> {
            for (RawPayment payment : items) {
                rawPaymentRepository.save(payment);
            }
        };
    }

    // --- Step 3: Daily Cancel Reconciliation (OrderCancel -> PaymentCancel) ---

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
    public JpaCursorItemReader<RawOrderCancel> dailyCancelReader(
            @Value("#{stepExecutionContext['partitionDate']}") String partitionDate) {
        
        LocalDate date = LocalDate.parse(partitionDate);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        return new JpaCursorItemReaderBuilder<RawOrderCancel>()
                .name("dailyCancelReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT oc FROM RawOrderCancel oc WHERE oc.isReconciled = false AND oc.cancelledAt BETWEEN :start AND :end")
                .parameterValues(Map.of("start", start, "end", end))
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
                rawOrderCancelRepository.save(orderCancel);
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

    // --- Step 4: Daily Payment Cancel Reconciliation (PaymentCancel -> OrderCancel) ---

    @Bean
    public Step dailyPaymentCancelMasterStep() {
        return new StepBuilder("dailyPaymentCancelMasterStep", jobRepository)
                .partitioner("dailyPaymentCancelWorkerStep", settlementPartitioner(null))
                .step(dailyPaymentCancelWorkerStep())
                .taskExecutor(settlementTaskExecutor())
                .gridSize(GRID_SIZE)
                .build();
    }

    @Bean
    public Step dailyPaymentCancelWorkerStep() {
        return new StepBuilder("dailyPaymentCancelWorkerStep", jobRepository)
                .<RawPaymentCancel, RawPaymentCancel>chunk(CHUNK_SIZE, transactionManager)
                .reader(dailyPaymentCancelReader(null))
                .processor(dailyPaymentCancelProcessor())
                .writer(dailyPaymentCancelWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaCursorItemReader<RawPaymentCancel> dailyPaymentCancelReader(
            @Value("#{stepExecutionContext['partitionDate']}") String partitionDate) {
        
        LocalDate date = LocalDate.parse(partitionDate);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        return new JpaCursorItemReaderBuilder<RawPaymentCancel>()
                .name("dailyPaymentCancelReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT pc FROM RawPaymentCancel pc WHERE pc.isReconciled = false AND pc.cancelledAt BETWEEN :start AND :end")
                .parameterValues(Map.of("start", start, "end", end))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<RawPaymentCancel, RawPaymentCancel> dailyPaymentCancelProcessor() {
        return paymentCancel -> {
            boolean orderCancelExists = rawOrderCancelRepository.existsById(new RawOrderCancelId(paymentCancel.getOrderNumber(), paymentCancel.getCancelledAt()));
            
            if (!orderCancelExists) {
                paymentCancel.setReconciled(true);
                paymentCancel.setReconciliationStatus(ReconciliationStatus.ORDER_CANCEL_NOT_FOUND);
                return paymentCancel;
            }

            paymentCancel.setReconciled(true);
            paymentCancel.setReconciliationStatus(ReconciliationStatus.SUCCESS);
            return paymentCancel;
        };
    }

    @Bean
    public ItemWriter<RawPaymentCancel> dailyPaymentCancelWriter() {
        return items -> {
            for (RawPaymentCancel paymentCancel : items) {
                rawPaymentCancelRepository.save(paymentCancel);
            }
        };
    }

    // --- Step 5: Daily Aggregation (Partitioned) ---

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
