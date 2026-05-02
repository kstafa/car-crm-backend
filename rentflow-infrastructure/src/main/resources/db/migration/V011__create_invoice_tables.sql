CREATE TABLE invoices (
    id               UUID          PRIMARY KEY,
    invoice_number   VARCHAR(20)   NOT NULL UNIQUE,
    contract_id      UUID          NOT NULL REFERENCES contracts(id),
    customer_id      UUID          NOT NULL REFERENCES customers(id),
    status           VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    issue_date       DATE          NOT NULL DEFAULT CURRENT_DATE,
    due_date         DATE          NOT NULL,
    paid_amount      NUMERIC(10,2) NOT NULL DEFAULT 0,
    currency         VARCHAR(3)    NOT NULL DEFAULT 'EUR',
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version          BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE invoice_line_items (
    id           UUID          PRIMARY KEY,
    invoice_id   UUID          NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description  TEXT          NOT NULL,
    type         VARCHAR(30)   NOT NULL,
    unit_price   NUMERIC(10,2) NOT NULL,
    currency     VARCHAR(3)    NOT NULL DEFAULT 'EUR',
    quantity     INT           NOT NULL DEFAULT 1,
    sort_order   INT           NOT NULL DEFAULT 0
);

CREATE TABLE invoice_payments (
    id                  UUID          PRIMARY KEY,
    invoice_id          UUID          NOT NULL REFERENCES invoices(id),
    amount              NUMERIC(10,2) NOT NULL,
    currency            VARCHAR(3)    NOT NULL DEFAULT 'EUR',
    method              VARCHAR(20)   NOT NULL,
    gateway_reference   VARCHAR(255),
    paid_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoices_contract   ON invoices (contract_id);
CREATE INDEX idx_invoices_customer   ON invoices (customer_id);
CREATE INDEX idx_invoices_status     ON invoices (status);
CREATE INDEX idx_invoices_due        ON invoices (due_date) WHERE status IN ('SENT','PARTIALLY_PAID');
