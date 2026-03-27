package com.example.analyticsservice.dto;

/**
 * 일별 신규 가입자 통계 DTO.
 * GET /api/v1/events/signups 응답에 사용
 */
public record SignupStatsDto(
        String date,
        long signupCount
) {}
