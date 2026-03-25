-- 주문 항목 사실 테이블 (Wide Table)
CREATE TABLE IF NOT EXISTS default.order_item_fact (
    order_id Int64,
    order_item_id Int64,
    order_number String,
    user_id Int64,
    
    -- 상품 및 카테고리 정보 (사실 테이블에 차원 정보를 포함하여 초고속 조회를 가능하게 함)
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
