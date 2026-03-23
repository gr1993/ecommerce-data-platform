-- 1. Create Weekly General Ledger table
CREATE TABLE weekly_general_ledger (
    id BIGSERIAL PRIMARY KEY,
    start_date DATE NOT NULL,               -- 주의 시작일 (예: 월요일)
    end_date DATE NOT NULL,                 -- 주의 종료일 (예: 일요일)
    ledger_type VARCHAR(20) NOT NULL,       -- 'SALES', 'CANCEL'
    total_amount DECIMAL(19, 2) NOT NULL,   -- 주간 해당 타입의 합산 금액
    total_count INTEGER NOT NULL,            -- 주간 해당 타입의 총 건수
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- 주의 시작일과 타입으로 유니크 제약
    CONSTRAINT uq_weekly_gl_start_type UNIQUE (start_date, ledger_type)
);

-- 시작일 기준 인덱스
CREATE INDEX idx_weekly_gl_start ON weekly_general_ledger(start_date);
