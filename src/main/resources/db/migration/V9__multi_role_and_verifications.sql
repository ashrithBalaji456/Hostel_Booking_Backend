CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, role)
);

INSERT INTO user_roles (user_id, role)
SELECT id, role FROM users;

CREATE TABLE owner_verifications (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    owner_name VARCHAR(120) NOT NULL,
    contact_details VARCHAR(200) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_reference VARCHAR(500) NOT NULL,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    verification_notes TEXT,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES users (id)
);

CREATE INDEX idx_owner_verifications_owner ON owner_verifications (owner_id);
CREATE INDEX idx_owner_verifications_status ON owner_verifications (verification_status);

CREATE TABLE property_verifications (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    address_verified BOOLEAN NOT NULL DEFAULT FALSE,
    document_reference VARCHAR(500) NOT NULL,
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500),
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES users (id)
);

CREATE INDEX idx_property_verifications_prop ON property_verifications (property_id);

ALTER TABLE properties
    ADD COLUMN IF NOT EXISTS emergency_booking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS late_night_check_in_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS latest_check_in_time TIME;
