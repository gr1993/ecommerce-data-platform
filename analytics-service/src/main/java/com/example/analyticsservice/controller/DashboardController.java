package com.example.analyticsservice.controller;

import com.example.analyticsservice.dto.DashboardSummaryDto;
import com.example.analyticsservice.repository.EventStatsRepository;
import com.example.analyticsservice.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

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
        LocalDate today      = LocalDate.now();
        LocalDate weekStart  = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.withDayOfMonth(1);

        // --- 매출 지표 ---
        BigDecimal dailyRevenue   = sumRevenue(statsRepository, today, today);
        BigDecimal weeklyRevenue  = sumRevenue(statsRepository, weekStart, today);
        BigDecimal monthlyRevenue = sumRevenue(statsRepository, monthStart, today);

        // --- 총 주문 수 (전체 누적) ---
        long totalOrders = statsRepository.getTotalOrderCount();

        // --- 이벤트 기반 지표 ---
        long newMembers    = eventStatsRepository.getTodaySignupCount();
        long lowStockCount = eventStatsRepository.getLowStockCount();
        long todayVisitors = eventStatsRepository.getTodayVisitors();
        long weekVisitors  = eventStatsRepository.getWeekVisitors();

        // --- 재고 가장 부족한 상품 ---
        var critical = eventStatsRepository.getMostCriticalProduct();
        String criticalProductName  = critical.map(p -> p.productName()).orElse("-");
        int    criticalProductStock = critical.map(p -> p.currentStock()).orElse(-1);

        return new DashboardSummaryDto(
                totalOrders,
                dailyRevenue,
                weeklyRevenue,
                monthlyRevenue,
                newMembers,
                lowStockCount,
                criticalProductName,
                criticalProductStock,
                todayVisitors,
                weekVisitors
        );
    }

    private BigDecimal sumRevenue(StatsRepository repo, LocalDate from, LocalDate to) {
        return repo.getRevenueTrend(from, to, "daily")
                .stream()
                .map(r -> r.revenue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
