CREATE TABLE contracts (
    id                      UUID PRIMARY KEY,
    contract_number         VARCHAR(20)  NOT NULL UNIQUE,
    reservation_id          UUID         NOT NULL REFERENCES reservations(id),
    customer_id             UUID         NOT NULL REFERENCES customers(id),
    vehicle_id              UUID         NOT NULL REFERENCES vehicles(id),
    scheduled_pickup        TIMESTAMPTZ  NOT NULL,
    scheduled_return        TIMESTAMPTZ  NOT NULL,
    actual_pickup_datetime  TIMESTAMPTZ,
    actual_return_datetime  TIMESTAMPTZ,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    signature_key           VARCHAR(500),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255),
    version                 BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE contract_inspections (
    id              UUID        PRIMARY KEY,
    contract_id     UUID        NOT NULL REFERENCES contracts(id),
    type            VARCHAR(4)  NOT NULL,
    front_ok        BOOLEAN     NOT NULL DEFAULT TRUE,
    rear_ok         BOOLEAN     NOT NULL DEFAULT TRUE,
    left_side_ok    BOOLEAN     NOT NULL DEFAULT TRUE,
    right_side_ok   BOOLEAN     NOT NULL DEFAULT TRUE,
    interior_ok     BOOLEAN     NOT NULL DEFAULT TRUE,
    trunk_ok        BOOLEAN     NOT NULL DEFAULT TRUE,
    tires_ok        BOOLEAN     NOT NULL DEFAULT TRUE,
    lights_ok       BOOLEAN     NOT NULL DEFAULT TRUE,
    notes           TEXT,
    fuel_level      VARCHAR(20) NOT NULL,
    mileage         INT         NOT NULL,
    performed_at    TIMESTAMPTZ NOT NULL,
    performed_by    UUID        NOT NULL
);

CREATE TABLE inspection_photos (
    inspection_id   UUID         NOT NULL REFERENCES contract_inspections(id),
    photo_key       VARCHAR(500) NOT NULL,
    PRIMARY KEY (inspection_id, photo_key)
);

CREATE INDEX idx_contracts_reservation ON contracts (reservation_id);
CREATE INDEX idx_contracts_vehicle     ON contracts (vehicle_id);
CREATE INDEX idx_contracts_customer    ON contracts (customer_id);
CREATE INDEX idx_contracts_status      ON contracts (status);
