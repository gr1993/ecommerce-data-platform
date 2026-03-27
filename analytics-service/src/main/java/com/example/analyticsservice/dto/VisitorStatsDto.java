package com.example.analyticsservice.dto;

/**
 * 일별 방문자 통계 DTO.
 * GET /api/v1/events/visitors 응답에 사용
 */
public record VisitorStatsDto(
        String date,
        long totalPageViews,
        long uniqueVisitorCount
) {}
