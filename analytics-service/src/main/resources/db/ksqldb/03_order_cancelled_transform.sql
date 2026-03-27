-- 주문 취소 원천 스트림 (order.cancelled 토픽 구독)
CREATE STREAM IF NOT EXISTS order_cancelled_raw (
    orderId BIGINT,
    orderNumber STRING,
    userId BIGINT,
    orderStatus STRING,
    cancelledAt STRING,
    cancelledItems ARRAY<STRUCT<
        orderItemId BIGINT,
        productId BIGINT,
        productName STRING,
        categoryId BIGINT,
        categoryName STRING,
        quantity INT,
        unitPrice DOUBLE,
        totalPrice DOUBLE
    >>
) WITH (
    KAFKA_TOPIC='order-cancelled',
    VALUE_FORMAT='JSON'
);


-- 취소된 주문 항목을 마이너스(-) 매출로 변환하여 발행
-- 이 데이터가 ClickHouse로 들어가면 SummingMergeTree에서 기존 매출을 상쇄함
-- ksqldb에서 INSERT INTO ... SELECT 구문은 지속적인 쿼리로 계속 실시간 파이프라인으로 동작
INSERT INTO analytics_order_item
    SELECT
        orderId AS order_id,
        orderNumber AS order_number,
        userId AS user_id,
        'CANCELLED' AS status, -- 상태를 CANCELLED로 고정
        cancelledAt AS ordered_at,
        EXPLODE(cancelledItems)->orderItemId AS order_item_id,
        EXPLODE(cancelledItems)->productId AS product_id,
        EXPLODE(cancelledItems)->productName AS product_name,
        EXPLODE(cancelledItems)->categoryId AS category_id,
        EXPLODE(cancelledItems)->categoryName AS category_name,
        (EXPLODE(cancelledItems)->quantity * -1) AS quantity, -- 수량 마이너스
        EXPLODE(cancelledItems)->unitPrice AS unit_price,
        (CAST(EXPLODE(cancelledItems)->totalPrice AS DOUBLE) * -1.0) AS total_price -- 금액 마이너스
    FROM order_cancelled_raw
    EMIT CHANGES;
