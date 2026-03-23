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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SettlementBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    
    private final RawPaymentRepository rawPaymentRepository;
    private final RawPaymentCancelRepository rawPaymentCancelRepository;
    private final LedgerRepository ledgerRepository;
    private final RawOrderRepository rawOrderRepository;
    private final RawOrderCancelRepository rawOrderCancelRepository;

    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job settlementJob() {
        return new JobBuilder("settlementJob", jobRepository)
                .start(salesReconciliationStep())
                .next(cancelReconciliationStep())
                .build();
    }

    // --- Step 1: Sales Reconciliation ---

    @Bean
    public Step salesReconciliationStep() {
        return new StepBuilder("salesReconciliationStep", jobRepository)
                .<RawOrder, Ledger>chunk(CHUNK_SIZE, transactionManager)
                .reader(salesReader())
                .processor(salesProcessor())
                .writer(salesWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<RawOrder> salesReader() {
        return new JpaPagingItemReaderBuilder<RawOrder>()
                .name("salesReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT o FROM RawOrder o WHERE o.isReconciled = false")
                .pageSize(CHUNK_SIZE)
                .build();
    }

    @Bean
    public ItemProcessor<RawOrder, Ledger> salesProcessor() {
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

            // Success
            order.setReconciled(true);
            order.setReconciliationStatus(ReconciliationStatus.SUCCESS);
            payment.setReconciled(true);
            payment.setReconciliationStatus(ReconciliationStatus.SUCCESS);
            rawPaymentRepository.save(payment);

            return Ledger.builder()
                    .orderNumber(order.getOrderNumber())
                    .amount(order.getTotalPaymentAmount())
                    .ledgerType("SALES")
                    .eventAt(order.getOrderedAt())
                    .build();
        };
    }

    @Bean
    public ItemWriter<Ledger> salesWriter() {
        return items -> {
            for (Ledger ledger : items) {
                ledgerRepository.save(ledger);
            }
            // RawOrder will be updated by JPA as it's attached to persistence context
        };
    }

    // --- Step 2: Cancel Reconciliation ---

    @Bean
    public Step cancelReconciliationStep() {
        return new StepBuilder("cancelReconciliationStep", jobRepository)
                .<RawOrderCancel, Ledger>chunk(CHUNK_SIZE, transactionManager)
                .reader(cancelReader())
                .processor(cancelProcessor())
                .writer(cancelWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<RawOrderCancel> cancelReader() {
        return new JpaPagingItemReaderBuilder<RawOrderCancel>()
                .name("cancelReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT oc FROM RawOrderCancel oc WHERE oc.isReconciled = false")
                .pageSize(CHUNK_SIZE)
                .build();
    }

    @Bean
    public ItemProcessor<RawOrderCancel, Ledger> cancelProcessor() {
        return orderCancel -> {
            List<RawPaymentCancel> paymentCancels = rawPaymentCancelRepository.findByOrderNumber(orderCancel.getOrderNumber());

            if (paymentCancels.isEmpty()) {
                orderCancel.setReconciled(true);
                orderCancel.setReconciliationStatus(ReconciliationStatus.PAYMENT_CANCEL_NOT_FOUND);
                return null;
            }

            RawPaymentCancel paymentCancel = paymentCancels.get(0);

            // Success
            orderCancel.setReconciled(true);
            orderCancel.setReconciliationStatus(ReconciliationStatus.SUCCESS);
            paymentCancel.setReconciled(true);
            paymentCancel.setReconciliationStatus(ReconciliationStatus.SUCCESS);
            rawPaymentCancelRepository.save(paymentCancel);

            return Ledger.builder()
                    .orderNumber(orderCancel.getOrderNumber())
                    .amount(BigDecimal.valueOf(paymentCancel.getAmount()).negate())
                    .ledgerType("CANCEL")
                    .eventAt(orderCancel.getCancelledAt())
                    .build();
        };
    }

    @Bean
    public ItemWriter<Ledger> cancelWriter() {
        return items -> {
            for (Ledger ledger : items) {
                ledgerRepository.save(ledger);
            }
        };
    }
}
