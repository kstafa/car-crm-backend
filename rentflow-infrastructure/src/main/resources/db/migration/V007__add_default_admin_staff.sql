ALTER TABLE vehicle_categories
    ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(4, 2) NOT NULL DEFAULT 0.20,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE vehicles
    ALTER COLUMN year TYPE INT,
    ADD COLUMN IF NOT EXISTS description TEXT;

CREATE TABLE IF NOT EXISTS vehicle_photos (
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    photo_key  TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS reservation_extras (
    id             UUID PRIMARY KEY,
    reservation_id UUID REFERENCES reservations(id) ON DELETE CASCADE,
    name           VARCHAR(255),
    unit_price     NUMERIC(10, 2),
    quantity       INT NOT NULL DEFAULT 0
);

-- Insert default admin user (password: 'changeme' bcrypt-hashed)
INSERT INTO staff (id, email, password_hash, first_name, last_name, role_id, status)
VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'admin@rentflow.com',
    '$2a$10$LZFfFIzbHvhQAVOSqVGdIOgbuowDq/Cr6DReBPDfme7izL5vUfvuO',
    'Admin', 'User',
    '00000000-0000-0000-0000-000000000001',
    'ACTIVE'
);
