-- 1. Create Monthly General Ledger table
CREATE TABLE monthly_general_ledger (
    id BIGSERIAL PRIMARY KEY,
    start_date DATE NOT NULL,               -- 월의 시작일 (예: 2026-03-01)
    end_date DATE NOT NULL,                 -- 월의 종료일 (예: 2026-03-31)
    ledger_type VARCHAR(20) NOT NULL,       -- 'SALES', 'CANCEL'
    total_amount DECIMAL(19, 2) NOT NULL,   -- 월간 해당 타입의 합산 금액
    total_count INTEGER NOT NULL,            -- 월간 해당 타입의 총 건수
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- 월의 시작일과 타입으로 유니크 제약
    CONSTRAINT uq_monthly_gl_start_type UNIQUE (start_date, ledger_type)
);

-- 시작일 기준 인덱스
CREATE INDEX idx_monthly_gl_start ON monthly_general_ledger(start_date);
