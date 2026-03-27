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
