package com.example.settlementservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String orderStatus;
    private BigDecimal totalPaymentAmount;
    private LocalDateTime orderedAt;
    private List<Map<String, Object>> orderItems;
    private Map<String, Object> delivery;
}
