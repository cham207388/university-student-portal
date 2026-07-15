ALTER TABLE student_profiles
    ADD COLUMN address_line_2 VARCHAR(500),
    ADD COLUMN city VARCHAR(120) NOT NULL DEFAULT 'unknown',
    ADD COLUMN state VARCHAR(120) NOT NULL DEFAULT 'unknown',
    ADD COLUMN postal_code VARCHAR(40) NOT NULL DEFAULT 'unknown',
    ADD COLUMN country VARCHAR(120) NOT NULL DEFAULT 'unknown';
