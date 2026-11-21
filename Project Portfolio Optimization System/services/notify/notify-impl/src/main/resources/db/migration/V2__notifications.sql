CREATE TABLE notify.notifications (
    id                   UUID         PRIMARY KEY,
    user_id              UUID         NOT NULL,
    channel              VARCHAR(20)  NOT NULL,
    title                VARCHAR(200) NOT NULL,
    body                 TEXT         NOT NULL,
    event_type           VARCHAR(80)  NOT NULL,
    related_entity_type  VARCHAR(80),
    related_entity_id    UUID,
    read_at              TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_notifications_channel
        CHECK (channel IN ('IN_APP', 'EMAIL', 'WS'))
);

CREATE TABLE notify.delivery_attempts (
    id                UUID        PRIMARY KEY,
    notification_id   UUID        NOT NULL,
    channel           VARCHAR(20) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    attempt_number    SMALLINT    NOT NULL,
    attempted_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    error_code        VARCHAR(80),
    error_message     TEXT,
    CONSTRAINT fk_delivery_attempts_notification
        FOREIGN KEY (notification_id) REFERENCES notify.notifications (id) ON DELETE CASCADE,
    CONSTRAINT ck_delivery_attempts_channel
        CHECK (channel IN ('IN_APP', 'EMAIL', 'WS')),
    CONSTRAINT ck_delivery_attempts_status
        CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT ck_delivery_attempts_number
        CHECK (attempt_number > 0),
    CONSTRAINT uk_delivery_attempts_notification_channel_number
        UNIQUE (notification_id, channel, attempt_number)
);

CREATE INDEX idx_notifications_user ON notify.notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notify.notifications (user_id, created_at DESC)
    WHERE read_at IS NULL;
CREATE INDEX idx_delivery_attempts_notification ON notify.delivery_attempts (notification_id);
