package com.example.analyticsservice.dto;

/**
 * 재고 부족 상품 DTO.
 * GET /api/v1/events/inventory/low-stock 응답에 사용
 */
public record LowStockDto(
        long productId,
        String productName,
        int currentStock
) {}
