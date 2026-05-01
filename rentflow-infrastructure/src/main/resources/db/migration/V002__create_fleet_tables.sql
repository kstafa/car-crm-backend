CREATE TABLE vehicles (
    id                  UUID PRIMARY KEY,
    license_plate       VARCHAR(20) NOT NULL UNIQUE,
    brand               VARCHAR(100) NOT NULL,
    model               VARCHAR(100) NOT NULL,
    year                SMALLINT NOT NULL,
    vin                 VARCHAR(17) UNIQUE,
    color               VARCHAR(50),
    fuel_type           VARCHAR(20),
    transmission        VARCHAR(20),
    seats               SMALLINT,
    current_mileage     INT NOT NULL DEFAULT 0,
    status              VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    category_id         UUID REFERENCES vehicle_categories(id),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_vehicles_status   ON vehicles (status) WHERE active = TRUE;
CREATE INDEX idx_vehicles_category ON vehicles (category_id) WHERE active = TRUE;
