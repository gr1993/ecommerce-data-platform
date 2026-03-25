-- 스트림 생성 이전 레코드를 처리하려면 설정 필요
SET 'auto.offset.reset' = 'earliest';

-- 주문 항목 단위로 데이터를 펼치는(Flattening) ETL 처리
-- 하나의 주문(N개 아이템)을 N개의 개별 매출 레코드로 변환하여 새로운 토픽으로 발행
-- 이 결과물은 ClickHouse Kafka Engine이 직접 구독
CREATE STREAM IF NOT EXISTS analytics_order_item 
WITH (KAFKA_TOPIC='analytics-order-item', VALUE_FORMAT='JSON') AS
    SELECT
        orderId AS order_id,
        orderNumber AS order_number,
        userId AS user_id,
        orderStatus AS status,
        orderedAt AS ordered_at,
        EXPLODE(orderItems)->orderItemId AS order_item_id,
        EXPLODE(orderItems)->productId AS product_id,
        EXPLODE(orderItems)->productName AS product_name,
        EXPLODE(orderItems)->categoryId AS category_id,
        EXPLODE(orderItems)->categoryName AS category_name,
        EXPLODE(orderItems)->quantity AS quantity,
        CAST(EXPLODE(orderItems)->unitPrice AS DOUBLE) AS unit_price,
        CAST(EXPLODE(orderItems)->totalPrice AS DOUBLE) AS total_price
    FROM order_created_raw
    EMIT CHANGES;
