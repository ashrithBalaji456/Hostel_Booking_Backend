CREATE TABLE properties (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users (id),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    property_type VARCHAR(30) NOT NULL,
    address VARCHAR(300) NOT NULL,
    locality VARCHAR(150),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    gender_policy VARCHAR(30) NOT NULL,
    check_in_time TIME NOT NULL DEFAULT '12:00:00',
    check_out_time TIME NOT NULL DEFAULT '11:00:00',
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    listing_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    instant_booking_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    min_short_stay_hours INTEGER NOT NULL DEFAULT 3,
    max_short_stay_days INTEGER NOT NULL DEFAULT 14,
    min_long_stay_days INTEGER NOT NULL DEFAULT 7,
    max_long_stay_months INTEGER NOT NULL DEFAULT 12,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_properties_owner ON properties (owner_id);
CREATE INDEX idx_properties_city ON properties (LOWER(city));
CREATE INDEX idx_properties_pincode ON properties (pincode);
CREATE INDEX idx_properties_status ON properties (verification_status, listing_status);
CREATE INDEX idx_properties_type_gender ON properties (property_type, gender_policy);

CREATE TABLE property_images (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    image_type VARCHAR(30) NOT NULL DEFAULT 'GALLERY',
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_property_images_property ON property_images (property_id);

CREATE TABLE amenities (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE property_amenities (
    property_id UUID NOT NULL REFERENCES properties (id) ON DELETE CASCADE,
    amenity_id UUID NOT NULL REFERENCES amenities (id) ON DELETE CASCADE,
    PRIMARY KEY (property_id, amenity_id)
);

CREATE TABLE property_pricing (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL UNIQUE REFERENCES properties (id) ON DELETE CASCADE,
    hourly_rate NUMERIC(12, 2),
    nightly_rate NUMERIC(12, 2) NOT NULL,
    weekly_rate NUMERIC(12, 2),
    monthly_rate NUMERIC(12, 2),
    security_deposit NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    extra_charges NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    discount_percentage NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO amenities (id, code, name, description) VALUES
    ('11111111-1111-1111-1111-111111111101', 'WIFI', 'High-Speed Wi-Fi', 'Complimentary wireless internet'),
    ('11111111-1111-1111-1111-111111111102', 'AC', 'Air Conditioning', 'Air conditioned rooms'),
    ('11111111-1111-1111-1111-111111111103', 'WASHING_MACHINE', 'Washing Machine', 'Self-service or shared laundry'),
    ('11111111-1111-1111-1111-111111111104', 'FOOD', 'Meals Included', 'Breakfast, lunch, or dinner facility'),
    ('11111111-1111-1111-1111-111111111105', 'PARKING', 'Parking', 'Two-wheeler and four-wheeler parking'),
    ('11111111-1111-1111-1111-111111111106', 'LOCKER', 'Personal Locker', 'Secure storage locker for each bed'),
    ('11111111-1111-1111-1111-111111111107', 'HOT_WATER', 'Hot Water', '24x7 geyser / hot water supply'),
    ('11111111-1111-1111-1111-111111111108', 'STUDY_ROOM', 'Study Room', 'Quiet study and coworking space'),
    ('11111111-1111-1111-1111-111111111109', 'CCTV', 'CCTV Security', '24x7 surveillance cameras in common areas'),
    ('11111111-1111-1111-1111-111111111110', 'POWER_BACKUP', 'Power Backup', 'Inverter or DG power backup');
