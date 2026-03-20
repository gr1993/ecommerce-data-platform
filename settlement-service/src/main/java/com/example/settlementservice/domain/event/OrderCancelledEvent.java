package com.example.settlementservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {
    private Long orderId;
    private String orderNumber;
    private String cancellationReason;
    private Long userId;
    private LocalDateTime cancelledAt;
    private List<Map<String, Object>> cancelledItems;
}
