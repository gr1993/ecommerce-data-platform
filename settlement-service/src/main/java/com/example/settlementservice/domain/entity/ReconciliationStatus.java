package com.example.settlementservice.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReconciliationStatus {
    PENDING("대조 대기"),
    SUCCESS("대조 성공"),
    
    // 오류 상태들 (Order -> Payment 방향)
    PAYMENT_NOT_FOUND("결제 데이터 누락"),
    AMOUNT_MISMATCH("주문-결제 금액 불일치"),
    PAYMENT_CANCEL_NOT_FOUND("결제 취소 데이터 누락"),

    // 오류 상태들 (Payment -> Order 방향)
    ORDER_NOT_FOUND("주문 데이터 누락"),
    ORDER_CANCEL_NOT_FOUND("주문 취소 데이터 누락");

    private final String description;
}
