ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS driving_license_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS driving_license_expiry DATE,
    ADD COLUMN IF NOT EXISTS passport_number VARCHAR(30),
    ADD COLUMN IF NOT EXISTS passport_expiry DATE;

CREATE INDEX idx_customers_license_expiry ON customers (driving_license_expiry)
    WHERE driving_license_expiry IS NOT NULL;
CREATE INDEX idx_customers_passport_expiry ON customers (passport_expiry)
    WHERE passport_expiry IS NOT NULL;
