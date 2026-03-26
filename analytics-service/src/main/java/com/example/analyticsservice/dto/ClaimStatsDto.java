package com.example.analyticsservice.dto;

import java.math.BigDecimal;

public record ClaimStatsDto(
    String type,
    Long count,
    BigDecimal amount
) {}
