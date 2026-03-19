package com.example.settlementservice.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_payment_cancels")
@Getter
@NoArgsConstructor
@IdClass(RawPaymentCancelId.class)
public class RawPaymentCancel {

    @Id
    @Column(name = "order_number")
    private String orderNumber;

    @Id
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
