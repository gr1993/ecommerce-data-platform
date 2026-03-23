package com.example.settlementservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_payments")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@IdClass(RawPaymentId.class)
public class RawPayment {

    @Id
    @Column(name = "order_number")
    private String orderNumber;

    @Id
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_key")
    private String paymentKey;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_amount")
    private Long paymentAmount;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "is_reconciled")
    private boolean isReconciled;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status")
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.PENDING;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
