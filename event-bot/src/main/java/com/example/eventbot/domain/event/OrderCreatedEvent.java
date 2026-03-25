package com.example.eventbot.domain.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class OrderCreatedEvent {
	private Long orderId;
	private String orderNumber;
	private Long userId;
	private String orderStatus;
	private BigDecimal totalProductAmount;
	private BigDecimal totalDiscountAmount;
	private BigDecimal totalPaymentAmount;
	private List<OrderItemSnapshot> orderItems;
	private DeliverySnapshot delivery;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	@Schema(description = "주문 일시", example = "2026-01-23T16:58:34", type = "string")
	private LocalDateTime orderedAt;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OrderItemSnapshot {
		private Long orderItemId;
		private Long productId;
		private Long skuId;
		private String productName;
		private String productCode;
		private Long categoryId;
		private String categoryName;
		private Integer quantity;
		private BigDecimal unitPrice;
		private BigDecimal totalPrice;
	}

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DeliverySnapshot {
		private String receiverName;
		private String receiverPhone;
		private String zipcode;
		private String address;
		private String addressDetail;
		private String deliveryMemo;
	}
}
