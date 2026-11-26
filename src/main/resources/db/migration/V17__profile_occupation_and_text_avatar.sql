ALTER TABLE users
    ALTER COLUMN profile_image TYPE TEXT;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS occupation_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS organization_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS course_or_role VARCHAR(150);
