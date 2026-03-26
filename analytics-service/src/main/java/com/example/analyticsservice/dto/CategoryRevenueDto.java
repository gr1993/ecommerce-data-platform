package com.example.analyticsservice.dto;

import java.math.BigDecimal;

public record CategoryRevenueDto(
    Long categoryId,
    String categoryName,
    BigDecimal revenue,
    Long quantity,
    Long orderCount
) {}
