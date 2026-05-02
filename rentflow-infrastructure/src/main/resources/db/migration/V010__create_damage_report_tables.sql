CREATE TABLE damage_reports (
    id                  UUID          PRIMARY KEY,
    vehicle_id          UUID          NOT NULL REFERENCES vehicles(id),
    contract_id         UUID          REFERENCES contracts(id),
    customer_id         UUID          REFERENCES customers(id),
    damage_description  TEXT          NOT NULL,
    severity            VARCHAR(20)   NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    liability           VARCHAR(20)   NOT NULL,
    estimated_cost      NUMERIC(10,2) NOT NULL,
    currency            VARCHAR(3)    NOT NULL DEFAULT 'EUR',
    actual_cost         NUMERIC(10,2),
    reported_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version             BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE damage_report_photos (
    report_id   UUID         NOT NULL REFERENCES damage_reports(id),
    photo_key   VARCHAR(500) NOT NULL,
    PRIMARY KEY (report_id, photo_key)
);

CREATE INDEX idx_damage_vehicle  ON damage_reports (vehicle_id);
CREATE INDEX idx_damage_contract ON damage_reports (contract_id);
CREATE INDEX idx_damage_status   ON damage_reports (status);
