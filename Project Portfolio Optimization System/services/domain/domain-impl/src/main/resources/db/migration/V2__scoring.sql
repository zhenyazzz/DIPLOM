CREATE TABLE domain.criteria (
    id                    UUID         PRIMARY KEY,
    code                  VARCHAR(60)  NOT NULL,
    name                  VARCHAR(160) NOT NULL,
    description           TEXT,
    value_type            VARCHAR(20)  NOT NULL,
    unit                  VARCHAR(40),
    direction             VARCHAR(3)   NOT NULL,
    source                VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',
    normalization_method  VARCHAR(20)  NOT NULL DEFAULT 'MIN_MAX',
    is_mandatory          BOOLEAN      NOT NULL DEFAULT TRUE,
    min_allowed           NUMERIC(19,6),
    max_allowed           NUMERIC(19,6),
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_criteria_code UNIQUE (code),
    CONSTRAINT ck_criteria_value_type
        CHECK (value_type IN ('NUMBER', 'PERCENT', 'CURRENCY', 'INTEGER', 'RATING')),
    CONSTRAINT ck_criteria_direction
        CHECK (direction IN ('MAX', 'MIN')),
    CONSTRAINT ck_criteria_source
        CHECK (source IN ('MANUAL', 'COMPUTED')),
    CONSTRAINT ck_criteria_normalization
        CHECK (normalization_method IN ('MIN_MAX', 'RATIO', 'MANUAL_SCALE')),
    CONSTRAINT ck_criteria_bounds
        CHECK (min_allowed IS NULL OR max_allowed IS NULL OR max_allowed >= min_allowed)
);

CREATE TABLE domain.scoring_models (
    id              UUID         PRIMARY KEY,
    code            VARCHAR(60)  NOT NULL,
    name            VARCHAR(180) NOT NULL,
    description     TEXT,
    version_no      INTEGER      NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    method          VARCHAR(20)  NOT NULL DEFAULT 'SAW',
    missing_policy  VARCHAR(30)  NOT NULL DEFAULT 'REJECT_PROJECT',
    created_by      UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    activated_at    TIMESTAMPTZ,
    CONSTRAINT uk_scoring_models_code_version UNIQUE (code, version_no),
    CONSTRAINT ck_scoring_models_version_no
        CHECK (version_no > 0),
    CONSTRAINT ck_scoring_models_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED')),
    CONSTRAINT ck_scoring_models_method
        CHECK (method IN ('SAW')),
    CONSTRAINT ck_scoring_models_missing_policy
        CHECK (missing_policy IN ('REJECT_PROJECT', 'NEUTRAL_VALUE'))
);

CREATE TABLE domain.model_weights (
    model_id      UUID          NOT NULL,
    criterion_id  UUID          NOT NULL,
    weight        NUMERIC(12,8) NOT NULL,
    sort_order    INTEGER       NOT NULL DEFAULT 0,
    fixed_min     NUMERIC(19,6),
    fixed_max     NUMERIC(19,6),
    CONSTRAINT pk_model_weights PRIMARY KEY (model_id, criterion_id),
    CONSTRAINT fk_model_weights_model
        FOREIGN KEY (model_id) REFERENCES domain.scoring_models (id) ON DELETE CASCADE,
    CONSTRAINT fk_model_weights_criterion
        FOREIGN KEY (criterion_id) REFERENCES domain.criteria (id) ON DELETE RESTRICT,
    CONSTRAINT ck_model_weights_weight
        CHECK (weight >= 0),
    CONSTRAINT ck_model_weights_fixed_bounds
        CHECK (fixed_min IS NULL OR fixed_max IS NULL OR fixed_max > fixed_min)
);

CREATE TABLE domain.assessments (
    id            UUID          PRIMARY KEY,
    project_id    UUID          NOT NULL,
    model_id      UUID          NOT NULL,
    criterion_id  UUID          NOT NULL,
    expert_id     UUID          NOT NULL,
    raw_value     NUMERIC(19,6) NOT NULL,
    comment       TEXT,
    status        VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    submitted_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_assessments_project
        FOREIGN KEY (project_id) REFERENCES domain.projects (id) ON DELETE RESTRICT,
    CONSTRAINT fk_assessments_model
        FOREIGN KEY (model_id) REFERENCES domain.scoring_models (id) ON DELETE RESTRICT,
    CONSTRAINT fk_assessments_criterion
        FOREIGN KEY (criterion_id) REFERENCES domain.criteria (id) ON DELETE RESTRICT,
    CONSTRAINT ck_assessments_status
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT uk_assessments_project_model_criterion_expert
        UNIQUE (project_id, model_id, criterion_id, expert_id)
);

CREATE TABLE domain.project_scores (
    id                       UUID           PRIMARY KEY,
    project_id               UUID           NOT NULL,
    model_id                 UUID           NOT NULL,
    total_score              NUMERIC(14,10) NOT NULL,
    criterion_contributions  JSONB          NOT NULL DEFAULT '{}'::jsonb,
    input_fingerprint        CHAR(64)       NOT NULL,
    calculated_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    is_current               BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_project_scores_project
        FOREIGN KEY (project_id) REFERENCES domain.projects (id) ON DELETE RESTRICT,
    CONSTRAINT fk_project_scores_model
        FOREIGN KEY (model_id) REFERENCES domain.scoring_models (id) ON DELETE RESTRICT,
    CONSTRAINT ck_project_scores_total
        CHECK (total_score >= 0),
    CONSTRAINT uk_project_scores_fingerprint
        UNIQUE (project_id, model_id, input_fingerprint)
);

CREATE INDEX idx_assessments_project ON domain.assessments (project_id, status);
CREATE INDEX idx_assessments_model ON domain.assessments (model_id, criterion_id);
CREATE INDEX idx_scores_project_current ON domain.project_scores (project_id, is_current);
CREATE INDEX idx_scores_model ON domain.project_scores (model_id);
CREATE INDEX idx_scoring_models_status ON domain.scoring_models (status);
