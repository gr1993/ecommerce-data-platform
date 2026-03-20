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
public class PaymentCancelledEvent {
    private String orderNumber;
    private Long amount;
    private String customerId;
    private String cancelReason;
    private LocalDateTime cancelledAt;
}
