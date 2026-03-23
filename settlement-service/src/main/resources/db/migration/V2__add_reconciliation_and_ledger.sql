-- 1. Add reconciliation columns to raw tables
ALTER TABLE raw_order_cancels RENAME COLUMN "userid" TO user_id;

ALTER TABLE raw_orders ADD COLUMN is_reconciled BOOLEAN DEFAULT FALSE;
ALTER TABLE raw_orders ADD COLUMN reconciliation_status VARCHAR(20) DEFAULT 'PENDING';

ALTER TABLE raw_payments ADD COLUMN is_reconciled BOOLEAN DEFAULT FALSE;
ALTER TABLE raw_payments ADD COLUMN reconciliation_status VARCHAR(20) DEFAULT 'PENDING';

ALTER TABLE raw_order_cancels ADD COLUMN is_reconciled BOOLEAN DEFAULT FALSE;
ALTER TABLE raw_order_cancels ADD COLUMN reconciliation_status VARCHAR(20) DEFAULT 'PENDING';

ALTER TABLE raw_payment_cancels ADD COLUMN is_reconciled BOOLEAN DEFAULT FALSE;
ALTER TABLE raw_payment_cancels ADD COLUMN reconciliation_status VARCHAR(20) DEFAULT 'PENDING';

-- 2. Create partial indexes for efficient reconciliation processing
-- Note: In PostgreSQL, indexes on partitioned tables are inherited by child partitions.
CREATE INDEX idx_raw_orders_unreconciled ON raw_orders (order_number) WHERE is_reconciled = FALSE;
CREATE INDEX idx_raw_payments_unreconciled ON raw_payments (order_number) WHERE is_reconciled = FALSE;
CREATE INDEX idx_raw_order_cancels_unreconciled ON raw_order_cancels (order_number) WHERE is_reconciled = FALSE;
CREATE INDEX idx_raw_payment_cancels_unreconciled ON raw_payment_cancels (order_number) WHERE is_reconciled = FALSE;

-- 3. Create Ledger table for reconciled transactions
CREATE TABLE ledgers (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    ledger_type VARCHAR(20) NOT NULL, -- 'SALES', 'CANCEL'
    event_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for analytics/reporting on ledgers
CREATE INDEX idx_ledgers_order_number ON ledgers (order_number);
CREATE INDEX idx_ledgers_event_at ON ledgers (event_at);
