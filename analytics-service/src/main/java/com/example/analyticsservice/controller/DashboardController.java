package com.example.analyticsservice.controller;

import com.example.analyticsservice.dto.DashboardSummaryDto;
import com.example.analyticsservice.repository.EventStatsRepository;
import com.example.analyticsservice.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * AdminDashboard 전용 통합 요약 API.
 *
 * <pre>
 * GET /api/v1/dashboard/summary
 * </pre>
 *
 * 프론트엔드가 한 번의 호출로 대시보드 카드 8개 지표를 모두 가져올 수 있도록
 * 주문(StatsRepository)과 이벤트(EventStatsRepository) 데이터를 조합한다.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StatsRepository statsRepository;
    private final EventStatsRepository eventStatsRepository;

    @GetMapping("/summary")
    public DashboardSummaryDto getSummary() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);   // 최근 7일
        LocalDate monthStart = today.withDayOfMonth(1);

        // --- 주문 지표 (order_item_fact 기반) ---
        BigDecimal dailyRevenue = statsRepository
                .getRevenueTrend(today, today, "daily")
                .stream()
                .map(r -> r.revenue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weeklyRevenue = statsRepository
                .getRevenueTrend(weekStart, today, "daily")
                .stream()
                .map(r -> r.revenue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyRevenue = statsRepository
                .getRevenueTrend(monthStart, today, "daily")
                .stream()
                .map(r -> r.revenue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = statsRepository
                .getCategoryRevenue(monthStart, today)
                .stream()
                .mapToLong(r -> r.orderCount())
                .sum();

        // --- 이벤트 기반 지표 ---
        long newMembers    = eventStatsRepository.getTodaySignupCount();
        long lowStockCount = eventStatsRepository.getLowStockCount();
        long todayVisitors = eventStatsRepository.getTodayVisitors();
        long weekVisitors  = eventStatsRepository.getWeekVisitors();

        return new DashboardSummaryDto(
                totalOrders,
                dailyRevenue,
                weeklyRevenue,
                monthlyRevenue,
                newMembers,
                lowStockCount,
                todayVisitors,
                weekVisitors
        );
    }
}
