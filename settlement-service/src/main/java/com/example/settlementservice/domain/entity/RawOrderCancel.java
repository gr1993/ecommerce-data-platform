package com.example.settlementservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "raw_order_cancels")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@IdClass(RawOrderCancelId.class)
public class RawOrderCancel {

    @Id
    @Column(name = "order_number")
    private String orderNumber;

    @Id
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cancelled_items", columnDefinition = "jsonb")
    private Map<String, Object> cancelledItems;

    @Column(name = "is_reconciled")
    private boolean isReconciled;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status")
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.PENDING;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
