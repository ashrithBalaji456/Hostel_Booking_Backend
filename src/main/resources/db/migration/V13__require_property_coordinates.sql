UPDATE properties SET latitude = 12.9716 WHERE latitude IS NULL;
UPDATE properties SET longitude = 77.5946 WHERE longitude IS NULL;

ALTER TABLE properties
    ALTER COLUMN latitude SET NOT NULL,
    ALTER COLUMN longitude SET NOT NULL,
    ADD CONSTRAINT chk_properties_latitude CHECK (latitude BETWEEN -90.0 AND 90.0),
    ADD CONSTRAINT chk_properties_longitude CHECK (longitude BETWEEN -180.0 AND 180.0);
