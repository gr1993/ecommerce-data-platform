package com.example.settlementservice.dto;

import com.example.settlementservice.domain.entity.ReconciliationStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ReconciliationErrorResponse {
    private String orderNumber;
    private String category; // "SALES" 또는 "CANCEL"
    private ReconciliationStatus status;
    private BigDecimal amount;
    private LocalDateTime eventAt;
    private String errorMessage;
}
