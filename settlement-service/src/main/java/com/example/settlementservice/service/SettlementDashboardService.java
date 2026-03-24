package com.example.settlementservice.service;

import com.example.settlementservice.domain.entity.ReconciliationStatus;
import com.example.settlementservice.dto.ReconciliationErrorResponse;
import com.example.settlementservice.repository.RawOrderCancelRepository;
import com.example.settlementservice.repository.RawOrderRepository;
import com.example.settlementservice.repository.RawPaymentCancelRepository;
import com.example.settlementservice.repository.RawPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementDashboardService {

    private final RawOrderRepository rawOrderRepository;
    private final RawOrderCancelRepository rawOrderCancelRepository;
    private final RawPaymentRepository rawPaymentRepository;
    private final RawPaymentCancelRepository rawPaymentCancelRepository;

    private static final List<ReconciliationStatus> ERROR_STATUSES = List.of(
            ReconciliationStatus.AMOUNT_MISMATCH,
            ReconciliationStatus.PAYMENT_NOT_FOUND,
            ReconciliationStatus.PAYMENT_CANCEL_NOT_FOUND
    );

    /**
     * 모든 정산 오류 내역 조회 (주문 번호 기준 취합)
     */
    @Transactional(readOnly = true)
    public List<ReconciliationErrorResponse> getAllErrors() {
        Map<String, ReconciliationErrorResponse> errorMap = new HashMap<>();

        // 1. 주문 테이블 오류 조회
        rawOrderRepository.findByReconciliationStatusIn(ERROR_STATUSES).forEach(o -> 
            errorMap.put(o.getOrderNumber(), ReconciliationErrorResponse.builder()
                    .orderNumber(o.getOrderNumber())
                    .category("SALES")
                    .status(o.getReconciliationStatus())
                    .amount(o.getTotalPaymentAmount())
                    .eventAt(o.getOrderedAt())
                    .errorMessage(o.getReconciliationStatus().getDescription())
                    .build())
        );

        // 2. 취소 테이블 오류 조회
        rawOrderCancelRepository.findByReconciliationStatusIn(ERROR_STATUSES).forEach(oc -> 
            errorMap.put(oc.getOrderNumber(), ReconciliationErrorResponse.builder()
                    .orderNumber(oc.getOrderNumber())
                    .category("CANCEL")
                    .status(oc.getReconciliationStatus())
                    .amount(null) // 취소 금액은 결제 쪽 확인 필요
                    .eventAt(oc.getCancelledAt())
                    .errorMessage(oc.getReconciliationStatus().getDescription())
                    .build())
        );

        // 3. 결제 테이블 오류 조회 (주문에 없는 결제만 추가)
        rawPaymentRepository.findByReconciliationStatusIn(ERROR_STATUSES).forEach(p -> {
            if (!errorMap.containsKey(p.getOrderNumber())) {
                errorMap.put(p.getOrderNumber(), ReconciliationErrorResponse.builder()
                        .orderNumber(p.getOrderNumber())
                        .category("PAYMENT_ONLY")
                        .status(p.getReconciliationStatus())
                        .eventAt(p.getPaidAt())
                        .errorMessage("결제 데이터는 있으나 매칭되는 주문이 없음")
                        .build());
            }
        });

        // 4. 결제 취소 테이블 오류 조회 (주문 취소에 없는 결제 취소만 추가)
        rawPaymentCancelRepository.findByReconciliationStatusIn(ERROR_STATUSES).forEach(pc -> {
            if (!errorMap.containsKey(pc.getOrderNumber())) {
                errorMap.put(pc.getOrderNumber(), ReconciliationErrorResponse.builder()
                        .orderNumber(pc.getOrderNumber())
                        .category("CANCEL_ONLY")
                        .status(pc.getReconciliationStatus())
                        .eventAt(pc.getCancelledAt())
                        .errorMessage("결제 취소 데이터는 있으나 매칭되는 주문 취소가 없음")
                        .build());
            }
        });

        return new ArrayList<>(errorMap.values()).stream()
                .sorted(Comparator.comparing(ReconciliationErrorResponse::getEventAt).reversed())
                .collect(Collectors.toList());
    }
}
