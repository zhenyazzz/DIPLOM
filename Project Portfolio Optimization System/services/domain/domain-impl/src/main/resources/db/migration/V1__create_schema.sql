CREATE SCHEMA IF NOT EXISTS domain;

CREATE TABLE domain.departments (
    id          UUID         PRIMARY KEY,
    code        VARCHAR(40)  NOT NULL,
    name        VARCHAR(200) NOT NULL,
    parent_id   UUID,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_departments_code UNIQUE (code),
    CONSTRAINT fk_departments_parent
        FOREIGN KEY (parent_id) REFERENCES domain.departments (id) ON DELETE RESTRICT,
    CONSTRAINT ck_departments_parent_not_self
        CHECK (parent_id IS NULL OR parent_id <> id)
);

CREATE TABLE domain.budget_periods (
    id          UUID         PRIMARY KEY,
    code        VARCHAR(40)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    starts_on   DATE         NOT NULL,
    ends_on     DATE         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_budget_periods_code UNIQUE (code),
    CONSTRAINT ck_budget_periods_status
        CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED')),
    CONSTRAINT ck_budget_periods_dates
        CHECK (ends_on >= starts_on)
);

CREATE TABLE domain.strategic_goals (
    id          UUID         PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    priority    INTEGER      NOT NULL DEFAULT 1,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_strategic_goals_code UNIQUE (code),
    CONSTRAINT ck_strategic_goals_priority
        CHECK (priority > 0)
);

CREATE INDEX idx_departments_parent ON domain.departments (parent_id);
CREATE INDEX idx_budget_periods_status ON domain.budget_periods (status);
CREATE INDEX idx_strategic_goals_active ON domain.strategic_goals (is_active);

CREATE TABLE domain.projects (
    id                UUID          PRIMARY KEY,
    code              VARCHAR(50)   NOT NULL,
    title             VARCHAR(250)  NOT NULL,
    description       TEXT,
    business_case     TEXT,
    status            VARCHAR(40)   NOT NULL DEFAULT 'DRAFT',
    department_id     UUID          NOT NULL,
    owner_id          UUID          NOT NULL,
    manager_id        UUID,
    planned_start     DATE,
    planned_finish    DATE,
    requested_budget  NUMERIC(19,4) NOT NULL,
    expected_benefit  NUMERIC(19,4),
    risk_category     VARCHAR(20),
    submitted_at      TIMESTAMPTZ,
    archived_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_projects_code UNIQUE (code),
    CONSTRAINT ck_projects_status CHECK (status IN (
        'DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'EVALUATED',
        'APPROVED_FOR_PORTFOLIO', 'REJECTED', 'RETURNED_FOR_REVISION', 'ARCHIVED'
    )),
    CONSTRAINT fk_projects_department
        FOREIGN KEY (department_id) REFERENCES domain.departments (id) ON DELETE RESTRICT,
    CONSTRAINT ck_projects_requested_budget
        CHECK (requested_budget >= 0),
    CONSTRAINT ck_projects_risk_category
        CHECK (risk_category IS NULL OR risk_category IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_projects_dates
        CHECK (planned_finish IS NULL OR planned_start IS NULL OR planned_finish >= planned_start)
);

CREATE TABLE domain.project_goals (
    project_id          UUID     NOT NULL,
    strategic_goal_id   UUID     NOT NULL,
    contribution_level  SMALLINT NOT NULL DEFAULT 1,
    CONSTRAINT pk_project_goals PRIMARY KEY (project_id, strategic_goal_id),
    CONSTRAINT fk_project_goals_project
        FOREIGN KEY (project_id) REFERENCES domain.projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_goals_strategic_goal
        FOREIGN KEY (strategic_goal_id) REFERENCES domain.strategic_goals (id) ON DELETE RESTRICT,
    CONSTRAINT ck_project_goals_contribution
        CHECK (contribution_level BETWEEN 1 AND 5)
);

CREATE TABLE domain.project_links (
    id               UUID        PRIMARY KEY,
    from_project_id  UUID        NOT NULL,
    to_project_id    UUID        NOT NULL,
    link_type        VARCHAR(30) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_project_links_from
        FOREIGN KEY (from_project_id) REFERENCES domain.projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_links_to
        FOREIGN KEY (to_project_id) REFERENCES domain.projects (id) ON DELETE RESTRICT,
    CONSTRAINT ck_project_links_type
        CHECK (link_type IN ('DEPENDENCY', 'MUTUAL_EXCLUSION')),
    CONSTRAINT ck_project_links_not_self
        CHECK (from_project_id <> to_project_id),
    CONSTRAINT uk_project_links_from_to_type
        UNIQUE (from_project_id, to_project_id, link_type)
);

CREATE TABLE domain.project_attachments (
    id           UUID         PRIMARY KEY,
    project_id   UUID         NOT NULL,
    file_id      UUID         NOT NULL,
    sha256       VARCHAR(64)  NOT NULL,
    caption      VARCHAR(200),
    uploaded_by  UUID         NOT NULL,
    uploaded_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_project_attachments_project
        FOREIGN KEY (project_id) REFERENCES domain.projects (id) ON DELETE CASCADE,
    CONSTRAINT uk_project_attachments_project_file
        UNIQUE (project_id, file_id)
);

CREATE INDEX idx_projects_status ON domain.projects (status);
CREATE INDEX idx_projects_department ON domain.projects (department_id);
CREATE INDEX idx_projects_owner ON domain.projects (owner_id);
CREATE INDEX idx_projects_dates ON domain.projects (planned_start, planned_finish);
CREATE INDEX idx_project_links_to ON domain.project_links (to_project_id);
CREATE INDEX idx_project_attachments_file ON domain.project_attachments (file_id);
