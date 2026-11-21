CREATE TABLE domain.scenarios (
    id                     UUID          PRIMARY KEY,
    code                   VARCHAR(60)   NOT NULL,
    name                   VARCHAR(200)  NOT NULL,
    budget_period_id       UUID          NOT NULL,
    scoring_model_id       UUID          NOT NULL,
    financial_snapshot_id  UUID,
    total_budget           NUMERIC(19,4) NOT NULL,
    status                 VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    created_by             UUID          NOT NULL,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version                BIGINT        NOT NULL DEFAULT 0,
    archived_at            TIMESTAMPTZ,
    CONSTRAINT uk_scenarios_code UNIQUE (code),
    CONSTRAINT fk_scenarios_budget_period
        FOREIGN KEY (budget_period_id) REFERENCES domain.budget_periods (id) ON DELETE RESTRICT,
    CONSTRAINT fk_scenarios_scoring_model
        FOREIGN KEY (scoring_model_id) REFERENCES domain.scoring_models (id) ON DELETE RESTRICT,
    CONSTRAINT ck_scenarios_total_budget
        CHECK (total_budget >= 0),
    CONSTRAINT ck_scenarios_status CHECK (status IN (
        'DRAFT', 'READY_FOR_OPTIMIZATION', 'OPTIMIZING', 'OPTIMIZED',
        'IN_REVIEW', 'APPROVED', 'REJECTED', 'ARCHIVED'
    ))
);

CREATE TABLE domain.scenario_items (
    scenario_id     UUID        NOT NULL,
    project_id      UUID        NOT NULL,
    selection_flag  VARCHAR(20) NOT NULL DEFAULT 'CANDIDATE',
    added_by        UUID        NOT NULL,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_scenario_items PRIMARY KEY (scenario_id, project_id),
    CONSTRAINT fk_scenario_items_scenario
        FOREIGN KEY (scenario_id) REFERENCES domain.scenarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_scenario_items_project
        FOREIGN KEY (project_id) REFERENCES domain.projects (id) ON DELETE RESTRICT,
    CONSTRAINT ck_scenario_items_selection_flag
        CHECK (selection_flag IN ('CANDIDATE', 'MANDATORY', 'FORBIDDEN'))
);

CREATE TABLE domain.optimization_runs (
    id                 UUID          PRIMARY KEY,
    scenario_id        UUID          NOT NULL,
    scenario_version   BIGINT        NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'QUEUED',
    algorithm_code     VARCHAR(60)   NOT NULL DEFAULT 'BINARY_DP',
    algorithm_version  VARCHAR(40)   NOT NULL,
    parameters         JSONB         NOT NULL DEFAULT '{}'::jsonb,
    input_snapshot     JSONB         NOT NULL,
    input_sha256       CHAR(64)      NOT NULL,
    objective_value    NUMERIC(19,8),
    budget_used        NUMERIC(19,4),
    requested_by       UUID          NOT NULL,
    queued_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    started_at         TIMESTAMPTZ,
    finished_at        TIMESTAMPTZ,
    execution_seconds  NUMERIC(12,4),
    error_code         VARCHAR(80),
    error_message      TEXT,
    CONSTRAINT fk_optimization_runs_scenario
        FOREIGN KEY (scenario_id) REFERENCES domain.scenarios (id) ON DELETE RESTRICT,
    CONSTRAINT ck_optimization_runs_scenario_version
        CHECK (scenario_version > 0),
    CONSTRAINT ck_optimization_runs_status CHECK (status IN (
        'QUEUED', 'RUNNING', 'OPTIMAL', 'FEASIBLE', 'INFEASIBLE',
        'TIME_LIMIT', 'FAILED', 'CANCELLED', 'STALE'
    )),
    CONSTRAINT ck_optimization_runs_budget_used
        CHECK (budget_used IS NULL OR budget_used >= 0),
    CONSTRAINT ck_optimization_runs_execution_seconds
        CHECK (execution_seconds IS NULL OR execution_seconds >= 0)
);

CREATE TABLE domain.run_items (
    run_id            UUID           NOT NULL,
    project_id        UUID           NOT NULL,
    selected          BOOLEAN        NOT NULL,
    score             NUMERIC(14,10) NOT NULL,
    cost              NUMERIC(19,4)  NOT NULL,
    selection_reason  TEXT,
    CONSTRAINT pk_run_items PRIMARY KEY (run_id, project_id),
    CONSTRAINT fk_run_items_run
        FOREIGN KEY (run_id) REFERENCES domain.optimization_runs (id) ON DELETE CASCADE,
    CONSTRAINT fk_run_items_project
        FOREIGN KEY (project_id) REFERENCES domain.projects (id) ON DELETE RESTRICT,
    CONSTRAINT ck_run_items_cost
        CHECK (cost >= 0)
);

CREATE TABLE domain.approvals (
    id           UUID        PRIMARY KEY,
    scenario_id  UUID        NOT NULL,
    run_id       UUID        NOT NULL,
    decision     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    comment      TEXT,
    decided_by   UUID,
    assigned_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at   TIMESTAMPTZ,
    CONSTRAINT fk_approvals_scenario
        FOREIGN KEY (scenario_id) REFERENCES domain.scenarios (id) ON DELETE RESTRICT,
    CONSTRAINT fk_approvals_run
        FOREIGN KEY (run_id) REFERENCES domain.optimization_runs (id) ON DELETE RESTRICT,
    CONSTRAINT ck_approvals_decision
        CHECK (decision IN ('PENDING', 'APPROVED', 'REJECTED', 'RETURNED')),
    CONSTRAINT uk_approvals_scenario_run UNIQUE (scenario_id, run_id)
);

CREATE TABLE domain.reports (
    id            UUID         PRIMARY KEY,
    run_id        UUID         NOT NULL,
    scenario_id   UUID         NOT NULL,
    report_type   VARCHAR(60)  NOT NULL DEFAULT 'PORTFOLIO_DECISION',
    file_id       UUID         NOT NULL,
    sha256        VARCHAR(64)  NOT NULL,
    generated_by  UUID         NOT NULL,
    generated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_reports_run
        FOREIGN KEY (run_id) REFERENCES domain.optimization_runs (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reports_scenario
        FOREIGN KEY (scenario_id) REFERENCES domain.scenarios (id) ON DELETE RESTRICT
);

CREATE TABLE domain.audit_log (
    id           UUID         PRIMARY KEY,
    occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actor_id     UUID,
    action       VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(80)  NOT NULL,
    entity_id    UUID,
    request_id   VARCHAR(100),
    outcome      VARCHAR(20)  NOT NULL,
    payload      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ck_audit_log_outcome
        CHECK (outcome IN ('SUCCESS', 'DENIED', 'FAILED'))
);

CREATE INDEX idx_scenarios_period_status ON domain.scenarios (budget_period_id, status);
CREATE INDEX idx_scenarios_snapshot ON domain.scenarios (financial_snapshot_id);
CREATE INDEX idx_scenario_items_project ON domain.scenario_items (project_id);
CREATE INDEX idx_runs_scenario ON domain.optimization_runs (scenario_id, queued_at DESC);
CREATE INDEX idx_runs_status ON domain.optimization_runs (status);
CREATE INDEX idx_run_items_project ON domain.run_items (project_id);
CREATE INDEX idx_approvals_pending ON domain.approvals (decision, assigned_at);
CREATE INDEX idx_reports_run ON domain.reports (run_id);
CREATE INDEX idx_audit_entity ON domain.audit_log (entity_type, entity_id, occurred_at DESC);
CREATE INDEX idx_audit_actor ON domain.audit_log (actor_id, occurred_at DESC);
CREATE INDEX idx_audit_action ON domain.audit_log (action, occurred_at DESC);
