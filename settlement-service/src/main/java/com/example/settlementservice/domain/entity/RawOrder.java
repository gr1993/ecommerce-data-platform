package com.example.settlementservice.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "raw_orders")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@IdClass(RawOrderId.class)
public class RawOrder {

    @Id
    @Column(name = "order_number")
    private String orderNumber;

    @Id
    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "order_status")
    private String orderStatus;

    @Column(name = "total_payment_amount")
    private BigDecimal totalPaymentAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "order_items", columnDefinition = "jsonb")
    private Map<String, Object> orderItems;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delivery", columnDefinition = "jsonb")
    private Map<String, Object> delivery;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
