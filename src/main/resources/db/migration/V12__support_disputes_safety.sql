CREATE TABLE notification_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    whatsapp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE support_tickets (
    id UUID PRIMARY KEY,
    ticket_reference VARCHAR(40) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users (id),
    booking_id UUID REFERENCES bookings (id),
    assigned_admin_id UUID REFERENCES users (id),
    category VARCHAR(40) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    resolution TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_support_tickets_user ON support_tickets (user_id);
CREATE INDEX idx_support_tickets_status ON support_tickets (status);

CREATE TABLE support_messages (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES support_tickets (id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users (id),
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE booking_disputes (
    id UUID PRIMARY KEY,
    dispute_reference VARCHAR(40) NOT NULL UNIQUE,
    booking_id UUID NOT NULL REFERENCES bookings (id),
    customer_id UUID NOT NULL REFERENCES users (id),
    property_id UUID NOT NULL REFERENCES properties (id),
    category VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    evidence_references TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    resolution TEXT,
    admin_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_booking_disputes_booking ON booking_disputes (booking_id);
CREATE INDEX idx_booking_disputes_status ON booking_disputes (status);

CREATE TABLE safety_complaints (
    id UUID PRIMARY KEY,
    complaint_reference VARCHAR(40) NOT NULL UNIQUE,
    reporter_id UUID NOT NULL REFERENCES users (id),
    property_id UUID NOT NULL REFERENCES properties (id),
    booking_id UUID REFERENCES bookings (id),
    category VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    evidence_references TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    admin_notes TEXT,
    resolution TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_safety_complaints_property ON safety_complaints (property_id);
CREATE INDEX idx_safety_complaints_status ON safety_complaints (status);
