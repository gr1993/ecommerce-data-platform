-- 주문 항목 사실 테이블 (Wide Table)
CREATE TABLE IF NOT EXISTS default.order_item_fact (
    order_id Int64,
    order_item_id Int64,
    order_number String,
    user_id Int64,
    
    -- 상품 및 카테고리 정보
    product_id Int64,
    product_name String,
    category_id Int64,
    category_name String,
    
    -- 수량 및 금액
    quantity Int32,
    unit_price Decimal(18, 2),
    total_price Decimal(18, 2),
    
    -- 시간 정보
    ordered_at DateTime,
    updated_at DateTime DEFAULT now(),
    
    -- 상태 (CONFIRMED, CANCELLED)
    status String
) ENGINE = ReplacingMergeTree(updated_at)
PARTITION BY toYYYYMM(ordered_at)
ORDER BY (ordered_at, category_id, product_id, order_item_id);


-- Kafka Engine 테이블 생성 (입구)
-- ksqlDB는 기본적으로 컬럼명을 대문자로 처리하므로 매핑을 위해 대문자로 정의
CREATE TABLE IF NOT EXISTS default.order_item_kafka (
    ORDER_ID Int64,
    ORDER_NUMBER String,
    USER_ID Int64,
    STATUS String,
    ORDERED_AT String,
    ORDER_ITEM_ID Int64,
    PRODUCT_ID Int64,
    PRODUCT_NAME String,
    CATEGORY_ID Int64,
    CATEGORY_NAME String,
    QUANTITY Int32,
    UNIT_PRICE Float64,
    TOTAL_PRICE Float64
) ENGINE = Kafka
SETTINGS 
    kafka_broker_list = 'kafka1:9091,kafka2:9092,kafka3:9094',
    kafka_topic_list = 'analytics-order-item',
    kafka_group_name = 'clickhouse-analytics-group',
    kafka_format = 'JSONEachRow',
    kafka_skip_broken_messages = 0;


-- 파이프라인용 Materialized View 생성 (전달자)
-- Kafka의 대문자 컬럼을 Fact 테이블의 소문자 컬럼으로 매핑
CREATE MATERIALIZED VIEW IF NOT EXISTS default.order_item_pipeline
TO default.order_item_fact
AS SELECT
    ORDER_ID as order_id,
    ORDER_ITEM_ID as order_item_id,
    ORDER_NUMBER as order_number,
    USER_ID as user_id,
    PRODUCT_ID as product_id,
    PRODUCT_NAME as product_name,
    CATEGORY_ID as category_id,
    CATEGORY_NAME as category_name,
    QUANTITY as quantity,
    UNIT_PRICE as unit_price,
    TOTAL_PRICE as total_price,
    parseDateTimeBestEffort(ORDERED_AT) AS ordered_at,
    now() AS updated_at,
    STATUS as status
FROM default.order_item_kafka;


-- 대시보드용 일별/카테고리별 매출 집계 (Materialized View)
CREATE MATERIALIZED VIEW IF NOT EXISTS default.daily_category_revenue_mv
ENGINE = SummingMergeTree()
ORDER BY (sale_date, category_id, category_name)
AS SELECT
    toDate(ordered_at) as sale_date,
    category_id,
    category_name,
    sum(total_price) as daily_revenue,
    sum(quantity) as daily_quantity,
    count(DISTINCT order_id) as order_count
FROM default.order_item_fact
GROUP BY sale_date, category_id, category_name;


-- 대시보드용 일별/상품별 매출 집계 (Materialized View)
CREATE MATERIALIZED VIEW IF NOT EXISTS default.daily_product_revenue_mv
ENGINE = SummingMergeTree()
ORDER BY (sale_date, product_id, product_name)
AS SELECT
    toDate(ordered_at) as sale_date,
    product_id,
    product_name,
    sum(total_price) as daily_revenue,
    sum(quantity) as daily_quantity
FROM default.order_item_fact
WHERE status = 'CONFIRMED'
GROUP BY sale_date, product_id, product_name;


-- 대시보드용 일별 클레임(취소/반품) 집계 (Materialized View)
CREATE MATERIALIZED VIEW IF NOT EXISTS default.daily_claim_stats_mv
ENGINE = SummingMergeTree()
ORDER BY (sale_date)
AS SELECT
    toDate(ordered_at) as sale_date,
    count() as daily_claim_count,
    sum(total_price) as daily_claim_amount
FROM default.order_item_fact
WHERE status = 'CANCELLED'
GROUP BY sale_date;
