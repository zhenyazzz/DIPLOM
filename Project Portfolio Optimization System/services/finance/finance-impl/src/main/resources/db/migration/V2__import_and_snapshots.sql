-- Finance owns import sessions, errors, financial snapshots and cash flows (ARCH-03).

CREATE TABLE finance.import_sessions (
    id                     UUID PRIMARY KEY,
    file_id                UUID         NOT NULL,
    sha256                 VARCHAR(64)  NOT NULL,
    status                 VARCHAR(40)  NOT NULL,
    uploaded_by            UUID         NOT NULL,
    budget_period_id       UUID,
    row_count              INTEGER,
    error_count            INTEGER,
    published_snapshot_id  UUID,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_import_sessions_status CHECK (status IN (
        'UPLOADED', 'PARSING', 'VALIDATING', 'READY_FOR_REVIEW',
        'PUBLISHED', 'COMPLETED_WITH_ERRORS', 'REJECTED', 'FAILED'
    ))
);

CREATE TABLE finance.import_errors (
    id           UUID PRIMARY KEY,
    session_id   UUID         NOT NULL REFERENCES finance.import_sessions (id) ON DELETE CASCADE,
    row_no       INTEGER,
    column_name  VARCHAR(80),
    code         VARCHAR(80)  NOT NULL,
    message      TEXT         NOT NULL
);

CREATE INDEX idx_import_errors_session ON finance.import_errors (session_id);

CREATE TABLE finance.financial_snapshots (
    id                 UUID PRIMARY KEY,
    source_session_id  UUID         NOT NULL REFERENCES finance.import_sessions (id),
    file_id            UUID         NOT NULL,
    sha256             VARCHAR(64)  NOT NULL,
    published_by       UUID         NOT NULL,
    published_at       TIMESTAMPTZ  NOT NULL,
    discount_rate      NUMERIC(12,6),
    label              VARCHAR(200)
);

CREATE TABLE finance.cash_flows (
    id            UUID PRIMARY KEY,
    snapshot_id   UUID            NOT NULL REFERENCES finance.financial_snapshots (id) ON DELETE CASCADE,
    project_id    UUID            NOT NULL,
    period_index  INTEGER         NOT NULL,
    amount        NUMERIC(19,4)   NOT NULL,
    CONSTRAINT uk_cash_flows_snapshot_project_period UNIQUE (snapshot_id, project_id, period_index)
);

CREATE INDEX idx_cash_flows_snapshot ON finance.cash_flows (snapshot_id);
CREATE INDEX idx_cash_flows_project ON finance.cash_flows (project_id);
