-- 1. Add unique constraint to ledgers table to prevent duplicate entries for the same order and type
ALTER TABLE ledgers ADD CONSTRAINT uq_ledger_order_type UNIQUE (order_number, ledger_type);
