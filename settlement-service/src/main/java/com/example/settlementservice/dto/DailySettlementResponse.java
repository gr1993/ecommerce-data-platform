package com.example.settlementservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class DailySettlementResponse {
    private LocalDate settlementDate;
    private BigDecimal totalOrderAmount;
    private int orderCount;
    private BigDecimal refundAmount;
    private int refundCount;
    private BigDecimal netRevenue;
}
