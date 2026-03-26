package com.example.analyticsservice.dto;

import java.math.BigDecimal;

public record RevenueTrendDto(
    String date,
    BigDecimal revenue
) {}
