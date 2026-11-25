CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    booking_reference VARCHAR(40) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users (id),
    property_id UUID NOT NULL REFERENCES properties (id),
    bed_id UUID NOT NULL REFERENCES beds (id),
    check_in TIMESTAMPTZ NOT NULL,
    check_out TIMESTAMPTZ NOT NULL,
    stay_type VARCHAR(30) NOT NULL,
    booking_status VARCHAR(30) NOT NULL,
    approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    guest_name VARCHAR(120) NOT NULL,
    guest_phone VARCHAR(20) NOT NULL,
    base_amount NUMERIC(12, 2) NOT NULL,
    deposit_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    extra_charges NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(12, 2) NOT NULL,
    amount_paid NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    refund_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    cancellation_reason VARCHAR(500),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_booking_dates CHECK (check_out > check_in),
    CONSTRAINT excl_no_overlapping_active_bed_bookings EXCLUDE USING GIST (
        bed_id WITH =,
        tstzrange(check_in, check_out, '[)') WITH &&
    ) WHERE (booking_status IN ('PENDING', 'CONFIRMED'))
);

CREATE INDEX idx_bookings_user ON bookings (user_id);
CREATE INDEX idx_bookings_property ON bookings (property_id);
CREATE INDEX idx_bookings_bed ON bookings (bed_id);
CREATE INDEX idx_bookings_status_expires ON bookings (booking_status, expires_at);
CREATE INDEX idx_bookings_dates ON bookings (check_in, check_out);
