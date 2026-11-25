CREATE TABLE payments (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings (id),
    payment_reference VARCHAR(60) NOT NULL UNIQUE,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    provider VARCHAR(30) NOT NULL,
    provider_order_id VARCHAR(120),
    provider_transaction_id VARCHAR(120),
    amount NUMERIC(12, 2) NOT NULL,
    refund_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    payment_status VARCHAR(30) NOT NULL,
    payment_method VARCHAR(30),
    failure_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_booking ON payments (booking_id);
CREATE INDEX idx_payments_status ON payments (payment_status);
CREATE INDEX idx_payments_provider_txn ON payments (provider_transaction_id);
