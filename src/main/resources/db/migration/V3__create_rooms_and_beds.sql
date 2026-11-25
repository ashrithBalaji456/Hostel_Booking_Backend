CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    room_number VARCHAR(50) NOT NULL,
    room_type VARCHAR(30) NOT NULL,
    floor INTEGER NOT NULL DEFAULT 0,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    amenities VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    hourly_rate NUMERIC(12, 2),
    nightly_rate NUMERIC(12, 2),
    weekly_rate NUMERIC(12, 2),
    monthly_rate NUMERIC(12, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_property_room_number UNIQUE (property_id, room_number)
);

CREATE INDEX idx_rooms_property ON rooms (property_id);

CREATE TABLE beds (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    bed_number VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_room_bed_number UNIQUE (room_id, bed_number)
);

CREATE INDEX idx_beds_room ON beds (room_id);
CREATE INDEX idx_beds_status ON beds (status);
