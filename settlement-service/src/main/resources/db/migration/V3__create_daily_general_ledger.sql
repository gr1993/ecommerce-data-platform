-- 1. Create Daily General Ledger table for daily aggregated financial data
CREATE TABLE daily_general_ledger (
    id BIGSERIAL PRIMARY KEY,
    settlement_date DATE NOT NULL,          -- 정산 대상 일자 (예: 2026-03-23)
    ledger_type VARCHAR(20) NOT NULL,       -- 'SALES' (매출), 'CANCEL' (취소)
    total_amount DECIMAL(19, 2) NOT NULL,   -- 당일 해당 타입의 합산 금액
    total_count INTEGER NOT NULL,            -- 당일 해당 타입의 총 건수
    description VARCHAR(255),               -- 필요 시 비고 (예: "정기 배치", "수동 재처리")
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- [핵심] 동일 날짜/타입의 중복 방지 및 Upsert를 위한 유니크 제약
    CONSTRAINT uq_daily_gl_date_type UNIQUE (settlement_date, ledger_type)
);

-- 날짜 기반 조회가 많으므로 인덱스 추가
CREATE INDEX idx_daily_gl_date ON daily_general_ledger(settlement_date);
