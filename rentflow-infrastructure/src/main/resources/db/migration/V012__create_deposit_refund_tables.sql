CREATE TABLE deposits (
    id              UUID          PRIMARY KEY,
    contract_id     UUID          NOT NULL REFERENCES contracts(id),
    customer_id     UUID          NOT NULL REFERENCES customers(id),
    invoice_id      UUID          NOT NULL REFERENCES invoices(id),
    amount          NUMERIC(10,2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'EUR',
    status          VARCHAR(20)   NOT NULL DEFAULT 'HELD',
    release_reason  TEXT,
    forfeit_reason  TEXT,
    held_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    settled_at      TIMESTAMPTZ,
    version         BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE refunds (
    id              UUID          PRIMARY KEY,
    invoice_id      UUID          NOT NULL REFERENCES invoices(id),
    customer_id     UUID          NOT NULL REFERENCES customers(id),
    amount          NUMERIC(10,2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'EUR',
    reason          VARCHAR(30)   NOT NULL,
    method          VARCHAR(20)   NOT NULL,
    notes           TEXT,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    approved_by     UUID,
    processed_by    UUID,
    requested_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ,
    version         BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_deposits_contract  ON deposits (contract_id);
CREATE INDEX idx_deposits_customer  ON deposits (customer_id);
CREATE INDEX idx_deposits_status    ON deposits (status);
CREATE INDEX idx_refunds_invoice    ON refunds (invoice_id);
CREATE INDEX idx_refunds_status     ON refunds (status);
