package com.example.analyticsservice.controller;

import com.example.analyticsservice.dto.LowStockDto;
import com.example.analyticsservice.dto.SignupStatsDto;
import com.example.analyticsservice.dto.VisitorStatsDto;
import com.example.analyticsservice.repository.EventStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 이벤트 기반 통계 API.
 *
 * <pre>
 * GET /api/v1/events/signups          — 기간별 일별 신규 가입자 추이
 * GET /api/v1/events/visitors         — 기간별 일별 방문자 추이 (PV + UV)
 * GET /api/v1/events/inventory/low-stock — 재고 부족 상품 목록
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventStatsController {

    private final EventStatsRepository eventStatsRepository;

    /**
     * 일별 신규 가입자 추이.
     * 예: GET /api/v1/events/signups?startDate=2026-03-01&endDate=2026-03-27
     */
    @GetMapping("/signups")
    public List<SignupStatsDto> getSignupStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return eventStatsRepository.getDailySignupStats(startDate, endDate);
    }

    /**
     * 일별 방문자 추이 (총 페이지뷰 + 순 방문자 수).
     * 예: GET /api/v1/events/visitors?startDate=2026-03-01&endDate=2026-03-27
     */
    @GetMapping("/visitors")
    public List<VisitorStatsDto> getVisitorStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return eventStatsRepository.getDailyVisitorStats(startDate, endDate);
    }

    /**
     * 재고 10개 미만 상품 목록 (재고 오름차순).
     * 예: GET /api/v1/events/inventory/low-stock
     */
    @GetMapping("/inventory/low-stock")
    public List<LowStockDto> getLowStockProducts() {
        return eventStatsRepository.getLowStockProducts();
    }
}
