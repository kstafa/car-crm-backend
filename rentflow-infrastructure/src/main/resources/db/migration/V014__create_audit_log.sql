CREATE TABLE audit_log (
    id           UUID         NOT NULL,
    action_type  VARCHAR(60)  NOT NULL,
    entity_type  VARCHAR(50),
    entity_id    VARCHAR(36),
    actor        VARCHAR(100) NOT NULL,
    occurred_at  TIMESTAMPTZ  NOT NULL,
    before_value TEXT,
    after_value  TEXT,
    ip_address   VARCHAR(45),
    PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);

CREATE TABLE audit_log_2026_q2 PARTITION OF audit_log
    FOR VALUES FROM ('2026-04-01') TO ('2026-07-01');
CREATE TABLE audit_log_2026_q3 PARTITION OF audit_log
    FOR VALUES FROM ('2026-07-01') TO ('2026-10-01');
CREATE TABLE audit_log_2026_q4 PARTITION OF audit_log
    FOR VALUES FROM ('2026-10-01') TO ('2027-01-01');
CREATE TABLE audit_log_2027_q1 PARTITION OF audit_log
    FOR VALUES FROM ('2027-01-01') TO ('2027-04-01');

CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_actor  ON audit_log (actor, occurred_at DESC);
CREATE INDEX idx_audit_action ON audit_log (action_type, occurred_at DESC);
