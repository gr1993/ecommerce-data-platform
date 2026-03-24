package com.example.settlementservice.controller;

import com.example.settlementservice.dto.ReconciliationErrorResponse;
import com.example.settlementservice.service.SettlementDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/settlement/dashboard")
@RequiredArgsConstructor
public class SettlementDashboardController {

    private final SettlementDashboardService settlementDashboardService;

    /**
     * 정산 대조 과정에서 발생한 모든 오류 목록을 조회합니다.
     */
    @GetMapping("/errors")
    public List<ReconciliationErrorResponse> getReconciliationErrors() {
        return settlementDashboardService.getAllErrors();
    }
}
