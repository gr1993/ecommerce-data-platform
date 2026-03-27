package com.example.eventbot.domain.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {

	private Long orderId;
	private String orderNumber;
	private String cancellationReason;  // USER_REQUEST, ADMIN_CANCEL, SYSTEM_TIMEOUT 등
	private Long userId;
	private List<CancelledOrderItem> cancelledItems;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime cancelledAt;

	/**
	 * 취소된 주문 항목 (재고 복구 대상)
	 */
	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CancelledOrderItem {
		private Long orderItemId;
		private Long productId;
		private Long skuId;
		private String productName;
		private String productCode;
		private Long categoryId;
		private String categoryName;
		private Integer quantity;  // 복구할 재고 수량
		private BigDecimal unitPrice;
		private BigDecimal totalPrice;
	}
}
