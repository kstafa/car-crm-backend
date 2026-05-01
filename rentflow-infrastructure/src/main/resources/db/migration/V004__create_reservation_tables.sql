CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE reservations (
    id                   UUID PRIMARY KEY,
    reservation_number   VARCHAR(20) NOT NULL UNIQUE,
    customer_id          UUID NOT NULL REFERENCES customers(id),
    vehicle_id           UUID NOT NULL REFERENCES vehicles(id),
    pickup_datetime      TIMESTAMPTZ NOT NULL,
    return_datetime      TIMESTAMPTZ NOT NULL,
    status               VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    base_amount          NUMERIC(10, 2) NOT NULL,
    currency             VARCHAR(3) NOT NULL DEFAULT 'EUR',
    discount_amount      NUMERIC(10, 2) NOT NULL DEFAULT 0,
    deposit_amount       NUMERIC(10, 2) NOT NULL DEFAULT 0,
    tax_amount           NUMERIC(10, 2) NOT NULL DEFAULT 0,
    notes                TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(255),
    version              BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE reservations
ADD CONSTRAINT no_vehicle_overlap
EXCLUDE USING GIST (
    vehicle_id WITH =,
    tstzrange(pickup_datetime, return_datetime, '[)') WITH &&
)
WHERE (status NOT IN ('CANCELLED', 'DRAFT'));

CREATE INDEX idx_res_vehicle_dates ON reservations
    (vehicle_id, pickup_datetime, return_datetime)
    WHERE status NOT IN ('CANCELLED', 'DRAFT');
CREATE INDEX idx_res_status        ON reservations (status);
CREATE INDEX idx_res_customer      ON reservations (customer_id);
