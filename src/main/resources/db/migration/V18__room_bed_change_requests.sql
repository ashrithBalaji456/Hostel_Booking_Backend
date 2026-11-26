CREATE TABLE bed_change_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    current_bed_id UUID NOT NULL REFERENCES beds(id),
    requested_bed_id UUID NOT NULL REFERENCES beds(id),
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    owner_note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_bed_change_booking_status ON bed_change_requests (booking_id, status);
CREATE INDEX idx_bed_change_property_status ON bed_change_requests (property_id, status);
