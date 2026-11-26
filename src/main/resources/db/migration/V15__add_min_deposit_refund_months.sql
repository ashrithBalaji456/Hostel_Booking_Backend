ALTER TABLE properties
    ADD COLUMN IF NOT EXISTS min_deposit_refund_months INTEGER NOT NULL DEFAULT 1;
