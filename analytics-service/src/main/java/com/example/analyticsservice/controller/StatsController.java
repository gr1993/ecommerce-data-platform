package com.example.analyticsservice.controller;

import com.example.analyticsservice.dto.CategoryRevenueDto;
import com.example.analyticsservice.dto.ClaimStatsDto;
import com.example.analyticsservice.dto.ProductRevenueDto;
import com.example.analyticsservice.dto.RevenueTrendDto;
import com.example.analyticsservice.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsRepository statsRepository;

    @GetMapping("/categories")
    public List<CategoryRevenueDto> getCategoryStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return statsRepository.getCategoryRevenue(startDate, endDate);
    }

    @GetMapping("/products")
    public List<ProductRevenueDto> getProductStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {
        return statsRepository.getProductRevenue(startDate, endDate, limit);
    }

    @GetMapping("/revenue-trend")
    public List<RevenueTrendDto> getRevenueTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String period) {
        return statsRepository.getRevenueTrend(startDate, endDate, period);
    }

    @GetMapping("/claims")
    public List<ClaimStatsDto> getClaimStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return statsRepository.getClaimStats(startDate, endDate);
    }
}
