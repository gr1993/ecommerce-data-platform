-- Raw Order Events Table (Partitioned by ordered_at)
CREATE TABLE raw_orders (
    order_id BIGINT NOT NULL,
    order_number VARCHAR(50) NOT NULL,
    user_id BIGINT,
    order_status VARCHAR(20),
    total_payment_amount DECIMAL(19, 2),
    ordered_at TIMESTAMP NOT NULL,
    order_items JSONB,
    delivery JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (order_number, ordered_at)
) PARTITION BY RANGE (ordered_at);

-- Raw Payment Events Table (Partitioned by paid_at)
CREATE TABLE raw_payments (
    order_number VARCHAR(50) NOT NULL,
    payment_key VARCHAR(100),
    payment_method VARCHAR(20),
    payment_amount BIGINT,
    payment_status VARCHAR(20),
    paid_at TIMESTAMP NOT NULL,
    customer_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (order_number, paid_at)
) PARTITION BY RANGE (paid_at);

-- Raw Order Cancel Events Table (Partitioned by cancelled_at)
CREATE TABLE raw_order_cancels (
    order_id BIGINT,
    order_number VARCHAR(50) NOT NULL,
    userId BIGINT,
    cancellation_reason TEXT,
    cancelled_at TIMESTAMP NOT NULL,
    cancelled_items JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (order_number, cancelled_at)
) PARTITION BY RANGE (cancelled_at);

-- Raw Payment Cancel Events Table (Partitioned by cancelled_at)
CREATE TABLE raw_payment_cancels (
    order_number VARCHAR(50) NOT NULL,
    amount BIGINT,
    customer_id VARCHAR(50),
    cancel_reason TEXT,
    cancelled_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (order_number, cancelled_at)
) PARTITION BY RANGE (cancelled_at);

---------------------------------------------------------
-- 2026년 3월 1일 ~ 3월 19일 파티션 수동 생성
---------------------------------------------------------

-- 1. raw_orders Partitions
CREATE TABLE raw_orders_2026_03_01 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-01 00:00:00') TO ('2026-03-02 00:00:00');
CREATE TABLE raw_orders_2026_03_02 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-02 00:00:00') TO ('2026-03-03 00:00:00');
CREATE TABLE raw_orders_2026_03_03 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-03 00:00:00') TO ('2026-03-04 00:00:00');
CREATE TABLE raw_orders_2026_03_04 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-04 00:00:00') TO ('2026-03-05 00:00:00');
CREATE TABLE raw_orders_2026_03_05 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-05 00:00:00') TO ('2026-03-06 00:00:00');
CREATE TABLE raw_orders_2026_03_06 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-06 00:00:00') TO ('2026-03-07 00:00:00');
CREATE TABLE raw_orders_2026_03_07 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-07 00:00:00') TO ('2026-03-08 00:00:00');
CREATE TABLE raw_orders_2026_03_08 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-08 00:00:00') TO ('2026-03-09 00:00:00');
CREATE TABLE raw_orders_2026_03_09 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-09 00:00:00') TO ('2026-03-10 00:00:00');
CREATE TABLE raw_orders_2026_03_10 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-10 00:00:00') TO ('2026-03-11 00:00:00');
CREATE TABLE raw_orders_2026_03_11 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-11 00:00:00') TO ('2026-03-12 00:00:00');
CREATE TABLE raw_orders_2026_03_12 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-12 00:00:00') TO ('2026-03-13 00:00:00');
CREATE TABLE raw_orders_2026_03_13 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-13 00:00:00') TO ('2026-03-14 00:00:00');
CREATE TABLE raw_orders_2026_03_14 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-14 00:00:00') TO ('2026-03-15 00:00:00');
CREATE TABLE raw_orders_2026_03_15 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-15 00:00:00') TO ('2026-03-16 00:00:00');
CREATE TABLE raw_orders_2026_03_16 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-16 00:00:00') TO ('2026-03-17 00:00:00');
CREATE TABLE raw_orders_2026_03_17 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-17 00:00:00') TO ('2026-03-18 00:00:00');
CREATE TABLE raw_orders_2026_03_18 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-18 00:00:00') TO ('2026-03-19 00:00:00');
CREATE TABLE raw_orders_2026_03_19 PARTITION OF raw_orders FOR VALUES FROM ('2026-03-19 00:00:00') TO ('2026-03-20 00:00:00');

-- 2. raw_payments Partitions
CREATE TABLE raw_payments_2026_03_01 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-01 00:00:00') TO ('2026-03-02 00:00:00');
CREATE TABLE raw_payments_2026_03_02 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-02 00:00:00') TO ('2026-03-03 00:00:00');
CREATE TABLE raw_payments_2026_03_03 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-03 00:00:00') TO ('2026-03-04 00:00:00');
CREATE TABLE raw_payments_2026_03_04 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-04 00:00:00') TO ('2026-03-05 00:00:00');
CREATE TABLE raw_payments_2026_03_05 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-05 00:00:00') TO ('2026-03-06 00:00:00');
CREATE TABLE raw_payments_2026_03_06 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-06 00:00:00') TO ('2026-03-07 00:00:00');
CREATE TABLE raw_payments_2026_03_07 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-07 00:00:00') TO ('2026-03-08 00:00:00');
CREATE TABLE raw_payments_2026_03_08 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-08 00:00:00') TO ('2026-03-09 00:00:00');
CREATE TABLE raw_payments_2026_03_09 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-09 00:00:00') TO ('2026-03-10 00:00:00');
CREATE TABLE raw_payments_2026_03_10 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-10 00:00:00') TO ('2026-03-11 00:00:00');
CREATE TABLE raw_payments_2026_03_11 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-11 00:00:00') TO ('2026-03-12 00:00:00');
CREATE TABLE raw_payments_2026_03_12 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-12 00:00:00') TO ('2026-03-13 00:00:00');
CREATE TABLE raw_payments_2026_03_13 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-13 00:00:00') TO ('2026-03-14 00:00:00');
CREATE TABLE raw_payments_2026_03_14 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-14 00:00:00') TO ('2026-03-15 00:00:00');
CREATE TABLE raw_payments_2026_03_15 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-15 00:00:00') TO ('2026-03-16 00:00:00');
CREATE TABLE raw_payments_2026_03_16 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-16 00:00:00') TO ('2026-03-17 00:00:00');
CREATE TABLE raw_payments_2026_03_17 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-17 00:00:00') TO ('2026-03-18 00:00:00');
CREATE TABLE raw_payments_2026_03_18 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-18 00:00:00') TO ('2026-03-19 00:00:00');
CREATE TABLE raw_payments_2026_03_19 PARTITION OF raw_payments FOR VALUES FROM ('2026-03-19 00:00:00') TO ('2026-03-20 00:00:00');

-- 3. raw_order_cancels Partitions
CREATE TABLE raw_order_cancels_2026_03_01 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-01 00:00:00') TO ('2026-03-02 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_02 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-02 00:00:00') TO ('2026-03-03 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_03 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-03 00:00:00') TO ('2026-03-04 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_04 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-04 00:00:00') TO ('2026-03-05 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_05 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-05 00:00:00') TO ('2026-03-06 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_06 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-06 00:00:00') TO ('2026-03-07 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_07 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-07 00:00:00') TO ('2026-03-08 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_08 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-08 00:00:00') TO ('2026-03-09 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_09 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-09 00:00:00') TO ('2026-03-10 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_10 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-10 00:00:00') TO ('2026-03-11 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_11 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-11 00:00:00') TO ('2026-03-12 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_12 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-12 00:00:00') TO ('2026-03-13 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_13 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-13 00:00:00') TO ('2026-03-14 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_14 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-14 00:00:00') TO ('2026-03-15 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_15 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-15 00:00:00') TO ('2026-03-16 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_16 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-16 00:00:00') TO ('2026-03-17 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_17 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-17 00:00:00') TO ('2026-03-18 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_18 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-18 00:00:00') TO ('2026-03-19 00:00:00');
CREATE TABLE raw_order_cancels_2026_03_19 PARTITION OF raw_order_cancels FOR VALUES FROM ('2026-03-19 00:00:00') TO ('2026-03-20 00:00:00');

-- 4. raw_payment_cancels Partitions
CREATE TABLE raw_payment_cancels_2026_03_01 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-01 00:00:00') TO ('2026-03-02 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_02 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-02 00:00:00') TO ('2026-03-03 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_03 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-03 00:00:00') TO ('2026-03-04 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_04 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-04 00:00:00') TO ('2026-03-05 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_05 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-05 00:00:00') TO ('2026-03-06 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_06 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-06 00:00:00') TO ('2026-03-07 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_07 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-07 00:00:00') TO ('2026-03-08 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_08 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-08 00:00:00') TO ('2026-03-09 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_09 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-09 00:00:00') TO ('2026-03-10 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_10 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-10 00:00:00') TO ('2026-03-11 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_11 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-11 00:00:00') TO ('2026-03-12 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_12 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-12 00:00:00') TO ('2026-03-13 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_13 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-13 00:00:00') TO ('2026-03-14 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_14 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-14 00:00:00') TO ('2026-03-15 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_15 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-15 00:00:00') TO ('2026-03-16 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_16 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-16 00:00:00') TO ('2026-03-17 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_17 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-17 00:00:00') TO ('2026-03-18 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_18 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-18 00:00:00') TO ('2026-03-19 00:00:00');
CREATE TABLE raw_payment_cancels_2026_03_19 PARTITION OF raw_payment_cancels FOR VALUES FROM ('2026-03-19 00:00:00') TO ('2026-03-20 00:00:00');

-- Default Partitions (Safety Net)
CREATE TABLE raw_orders_default PARTITION OF raw_orders DEFAULT;
CREATE TABLE raw_payments_default PARTITION OF raw_payments DEFAULT;
CREATE TABLE raw_order_cancels_default PARTITION OF raw_order_cancels DEFAULT;
CREATE TABLE raw_payment_cancels_default PARTITION OF raw_payment_cancels DEFAULT;
