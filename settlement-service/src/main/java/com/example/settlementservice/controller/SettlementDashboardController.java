package com.example.settlementservice.controller;

import com.example.settlementservice.dto.DailySettlementResponse;
import com.example.settlementservice.dto.ReconciliationErrorResponse;
import com.example.settlementservice.service.SettlementDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/settlement/dashboard")
@RequiredArgsConstructor
public class SettlementDashboardController {

    private final SettlementDashboardService settlementDashboardService;

    /**
     * 특정 기간의 일별 정산 총계 데이터를 조회합니다.
     */
    @GetMapping("/daily")
    public List<DailySettlementResponse> getDailySettlements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return settlementDashboardService.getDailySettlements(start, end);
    }

    /**
     * 정산 대조 과정에서 발생한 모든 오류 목록을 조회합니다.
     */
    @GetMapping("/errors")
    public List<ReconciliationErrorResponse> getReconciliationErrors() {
        return settlementDashboardService.getAllErrors();
    }
}
