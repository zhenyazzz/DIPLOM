-- V1: files (metadata; bytes live in object storage)

CREATE TABLE files (
    id            UUID         PRIMARY KEY,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    version       BIGINT,
    bucket        VARCHAR(32)  NOT NULL,
    object_key    VARCHAR(64)  NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type  VARCHAR(255) NOT NULL,
    size_bytes    BIGINT       NOT NULL CHECK (size_bytes >= 0),
    sha256        VARCHAR(64)  NOT NULL,
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT uk_files_object_key UNIQUE (object_key)
);
