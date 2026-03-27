package com.example.analyticsservice.dto;

import java.math.BigDecimal;

/**
 * GET /api/v1/dashboard/summary 단일 응답 DTO.
 * 대시보드 카드 8개 지표를 한 번의 API 호출로 반환한다.
 */
public record DashboardSummaryDto(
        long totalOrders,
        BigDecimal dailyRevenue,
        BigDecimal weeklyRevenue,
        BigDecimal monthlyRevenue,
        long newMembers,       // 오늘 신규 가입자 수
        long lowStockCount,    // 재고 10개 미만 상품 수
        long todayVisitors,    // 오늘 순 방문자 수 (UV)
        long weekVisitors      // 이번 주 순 방문자 수 (UV)
) {}
