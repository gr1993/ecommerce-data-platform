package com.example.settlementservice.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_payments")
@Getter
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

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
