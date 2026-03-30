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


-- ============================================================
-- Event Analytics Schema (Kafka Direct Ingestion)
-- ksqlDB를 거치지 않고 Kafka -> ClickHouse 직접 인입
-- 구성: Kafka Engine 테이블 (입구) -> Fact 테이블 (Raw) -> Materialized View (집계)
-- ============================================================


-- ===========================================================
-- 신규 회원 가입 (user_registered)
-- ===========================================================

-- Raw 데이터 저장용 Fact 테이블
CREATE TABLE IF NOT EXISTS default.user_registered_raw (
    user_id      Int64,
    email        String,
    signup_source String,
    registered_at DateTime,
    created_at   DateTime DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(registered_at)
ORDER BY (registered_at, user_id);

-- Kafka Engine 테이블 (입구) — event-bot DTO의 camelCase 필드명과 매핑
CREATE TABLE IF NOT EXISTS default.user_registered_kafka (
    userId       Int64,
    email        String,
    signupSource String,
    registeredAt String
) ENGINE = Kafka
SETTINGS
    kafka_broker_list        = 'kafka1:9091,kafka2:9092,kafka3:9094',
    kafka_topic_list         = 'user_registered',
    kafka_group_name         = 'ch-user-reg-group',
    kafka_format             = 'JSONEachRow',
    kafka_skip_broken_messages = 1;

-- 파이프라인 (Kafka -> Raw Fact)
CREATE MATERIALIZED VIEW IF NOT EXISTS default.user_registered_pipeline
TO default.user_registered_raw
AS SELECT
    userId                              AS user_id,
    email,
    signupSource                        AS signup_source,
    parseDateTimeBestEffort(registeredAt) AS registered_at,
    now()                               AS created_at
FROM default.user_registered_kafka;

-- 집계 MV: 일별 신규 가입자 수 (SummingMergeTree)
CREATE MATERIALIZED VIEW IF NOT EXISTS default.daily_signup_stats_mv
ENGINE = SummingMergeTree()
ORDER BY (log_date)
AS SELECT
    toDate(registered_at) AS log_date,
    count()               AS signup_count
FROM default.user_registered_raw
GROUP BY log_date;


-- ===========================================================
-- 재고 변동 알림 (inventory_changed)
-- ===========================================================

-- Raw 데이터 저장용 Fact 테이블 (전체 변동 이력 보존)
CREATE TABLE IF NOT EXISTS default.inventory_changed_raw (
    product_id    Int64,
    product_name  String,
    change_amount Int32,
    current_stock Int32,
    changed_at    DateTime,
    created_at    DateTime DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(changed_at)
ORDER BY (changed_at, product_id);

-- Kafka Engine 테이블 (입구)
CREATE TABLE IF NOT EXISTS default.inventory_changed_kafka (
    productId    Int64,
    productName  String,
    changeAmount Int32,
    currentStock Int32,
    changedAt    String
) ENGINE = Kafka
SETTINGS
    kafka_broker_list        = 'kafka1:9091,kafka2:9092,kafka3:9094',
    kafka_topic_list         = 'inventory_changed',
    kafka_group_name         = 'ch-inv-change-group',
    kafka_format             = 'JSONEachRow',
    kafka_skip_broken_messages = 1;

-- 파이프라인 (Kafka -> Raw Fact)
CREATE MATERIALIZED VIEW IF NOT EXISTS default.inventory_changed_pipeline
TO default.inventory_changed_raw
AS SELECT
    productId                            AS product_id,
    productName                          AS product_name,
    changeAmount                         AS change_amount,
    currentStock                         AS current_stock,
    parseDateTimeBestEffort(changedAt)   AS changed_at,
    now()                                AS created_at
FROM default.inventory_changed_kafka;

-- 집계 MV: 상품별 최신 재고 상태 (ReplacingMergeTree — changed_at 기준으로 중복 제거)
CREATE MATERIALIZED VIEW IF NOT EXISTS default.current_inventory_mv
ENGINE = ReplacingMergeTree(changed_at)
ORDER BY (product_id)
AS SELECT
    product_id,
    product_name,
    current_stock,
    changed_at
FROM default.inventory_changed_raw;


-- ===========================================================
-- 방문자 수 (page_viewed)
-- ===========================================================

-- Raw 데이터 저장용 Fact 테이블
CREATE TABLE IF NOT EXISTS default.page_viewed_raw (
    user_id    Int64,
    page_url   String,
    user_agent String,
    viewed_at  DateTime,
    created_at DateTime DEFAULT now()
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(viewed_at)
ORDER BY (viewed_at, user_id);

-- Kafka Engine 테이블 (입구)
CREATE TABLE IF NOT EXISTS default.page_viewed_kafka (
    userId    Int64,
    pageUrl   String,
    userAgent String,
    viewedAt  String
) ENGINE = Kafka
SETTINGS
    kafka_broker_list        = 'kafka1:9091,kafka2:9092,kafka3:9094',
    kafka_topic_list         = 'page_viewed',
    kafka_group_name         = 'ch-page-view-group',
    kafka_format             = 'JSONEachRow',
    kafka_skip_broken_messages = 1;

-- 파이프라인 (Kafka -> Raw Fact)
CREATE MATERIALIZED VIEW IF NOT EXISTS default.page_viewed_pipeline
TO default.page_viewed_raw
AS SELECT
    userId                             AS user_id,
    pageUrl                            AS page_url,
    userAgent                          AS user_agent,
    parseDateTimeBestEffort(viewedAt)  AS viewed_at,
    now()                              AS created_at
FROM default.page_viewed_kafka;

-- 집계 MV: 일별 총 페이지 뷰 & 순 방문자 수
CREATE MATERIALIZED VIEW IF NOT EXISTS default.daily_visitor_stats_mv
ENGINE = SummingMergeTree()
ORDER BY (log_date)
AS SELECT
    toDate(viewed_at)   AS log_date,
    count()             AS total_page_views,
    uniq(user_id)       AS unique_visitor_count
FROM default.page_viewed_raw
GROUP BY log_date;