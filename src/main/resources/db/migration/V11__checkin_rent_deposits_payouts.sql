CREATE TABLE checkin_records (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings (id) ON DELETE CASCADE,
    checked_in_at TIMESTAMPTZ NOT NULL,
    early_checkin BOOLEAN NOT NULL DEFAULT FALSE,
    verified_by_owner BOOLEAN NOT NULL DEFAULT FALSE,
    verified_by_customer BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE checkout_records (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings (id) ON DELETE CASCADE,
    checked_out_at TIMESTAMPTZ NOT NULL,
    late_checkout BOOLEAN NOT NULL DEFAULT FALSE,
    late_checkout_fee NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    verified_by_owner BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE booking_failures (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings (id),
    failure_reason VARCHAR(50) NOT NULL,
    description TEXT,
    alternative_property_id UUID REFERENCES properties (id),
    alternative_bed_id UUID REFERENCES beds (id),
    customer_accepted_alternative BOOLEAN,
    resolution_status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE rent_schedules (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    installment_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    amount_due NUMERIC(12, 2) NOT NULL,
    amount_paid NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rent_schedules_booking ON rent_schedules (booking_id);

CREATE TABLE rent_payments (
    id UUID PRIMARY KEY,
    rent_schedule_id UUID NOT NULL REFERENCES rent_schedules (id) ON DELETE CASCADE,
    amount NUMERIC(12, 2) NOT NULL,
    payment_reference VARCHAR(60) NOT NULL,
    paid_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE security_deposits (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings (id) ON DELETE CASCADE,
    deposit_amount NUMERIC(12, 2) NOT NULL,
    deducted_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    refunded_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    deduction_reason VARCHAR(500),
    disputed BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'HELD',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE owner_commissions (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL UNIQUE REFERENCES users (id),
    commission_percentage NUMERIC(5, 2) NOT NULL DEFAULT 10.00,
    updated_by UUID REFERENCES users (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE owner_payouts (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users (id),
    gross_amount NUMERIC(12, 2) NOT NULL,
    commission_amount NUMERIC(12, 2) NOT NULL,
    refund_adjustments NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    net_payable NUMERIC(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    settlement_reference VARCHAR(100),
    failure_reason VARCHAR(500),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);

CREATE INDEX idx_owner_payouts_owner ON owner_payouts (owner_id);
