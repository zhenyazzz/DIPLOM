ALTER TABLE finance.import_sessions
    ADD CONSTRAINT ck_import_sessions_row_count
        CHECK (row_count IS NULL OR row_count >= 0);

ALTER TABLE finance.import_sessions
    ADD CONSTRAINT ck_import_sessions_error_count
        CHECK (error_count IS NULL OR error_count >= 0);

ALTER TABLE finance.import_sessions
    ADD CONSTRAINT fk_import_sessions_published_snapshot
        FOREIGN KEY (published_snapshot_id) REFERENCES finance.financial_snapshots (id);

ALTER TABLE finance.cash_flows
    ADD CONSTRAINT ck_cash_flows_period_index
        CHECK (period_index >= 0);

CREATE INDEX idx_import_sessions_status ON finance.import_sessions (status);
CREATE INDEX idx_financial_snapshots_published ON finance.financial_snapshots (published_at DESC);
