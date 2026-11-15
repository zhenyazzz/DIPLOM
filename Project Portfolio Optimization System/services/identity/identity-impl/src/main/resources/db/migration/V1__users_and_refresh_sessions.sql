-- V1: identity schema + users + refresh_sessions

CREATE SCHEMA IF NOT EXISTS identity;

CREATE TABLE identity.users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(320) NOT NULL,
    display_name  VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    blocked       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE identity.refresh_sessions (
    id             UUID        PRIMARY KEY,
    user_id        UUID        NOT NULL,
    token_hash     CHAR(64)    NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    revoked_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL,
    replaced_by_id UUID,
    CONSTRAINT uk_refresh_sessions_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_sessions_user
        FOREIGN KEY (user_id) REFERENCES identity.users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_sessions_replaced_by
        FOREIGN KEY (replaced_by_id) REFERENCES identity.refresh_sessions (id)
);

CREATE INDEX idx_refresh_sessions_user_id ON identity.refresh_sessions (user_id);
CREATE INDEX idx_refresh_sessions_expires_at ON identity.refresh_sessions (expires_at);
