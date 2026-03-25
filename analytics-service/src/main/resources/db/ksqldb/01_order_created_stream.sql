-- 원본 주문 생성 이벤트 스트림 정의
-- Kafka 토픽 'order-created'의 JSON 데이터를 ksqlDB 스트림으로 매핑
CREATE STREAM IF NOT EXISTS order_created_raw (
    orderId BIGINT,
    orderNumber STRING,
    userId BIGINT,
    orderStatus STRING,
    orderItems ARRAY<STRUCT<
        orderItemId BIGINT,
        productId BIGINT,
        productName STRING,
        categoryId BIGINT,
        categoryName STRING,
        quantity INTEGER,
        unitPrice DECIMAL(18, 2),
        totalPrice DECIMAL(18, 2)
    >>,
    orderedAt STRING
) WITH (
    KAFKA_TOPIC='order-created',
    VALUE_FORMAT='JSON'
);
