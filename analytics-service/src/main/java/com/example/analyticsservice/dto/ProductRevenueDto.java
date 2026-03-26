package com.example.analyticsservice.dto;

import java.math.BigDecimal;

public record ProductRevenueDto(
    Long productId,
    String productName,
    BigDecimal revenue,
    Long quantity
) {}
