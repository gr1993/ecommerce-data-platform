package com.example.settlementservice.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RawPaymentId implements Serializable {
    private String orderNumber;
    private LocalDateTime paidAt;
}
