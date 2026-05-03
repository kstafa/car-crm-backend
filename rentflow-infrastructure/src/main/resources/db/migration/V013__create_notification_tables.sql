CREATE TABLE notification_templates (
    id               UUID         PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    trigger          VARCHAR(40)  NOT NULL,
    channel          VARCHAR(10)  NOT NULL,
    subject_template TEXT,
    body_template    TEXT         NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version          BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE automation_rules (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    trigger       VARCHAR(40)  NOT NULL,
    template_id   UUID         NOT NULL REFERENCES notification_templates(id),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    delay_minutes INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version       BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE staff_notifications (
    id           UUID         PRIMARY KEY,
    recipient_id UUID         NOT NULL,
    title        VARCHAR(255) NOT NULL,
    message      TEXT,
    entity_type  VARCHAR(50),
    entity_id    VARCHAR(36),
    read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_staff_notif_recipient ON staff_notifications (recipient_id, created_at DESC);
CREATE INDEX idx_staff_notif_unread    ON staff_notifications (recipient_id) WHERE read = FALSE;
