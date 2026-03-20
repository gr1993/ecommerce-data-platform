package com.example.settlementservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmedEvent {
    private String orderNumber;
    private String paymentKey;
    private String paymentMethod;
    private Long paymentAmount;
    private String paymentStatus;
    private LocalDateTime paidAt;
    private String customerId;
}
