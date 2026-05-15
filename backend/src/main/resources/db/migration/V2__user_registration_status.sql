ALTER TABLE users
    ADD COLUMN registration_status VARCHAR(32) NOT NULL DEFAULT 'APPROVED';

UPDATE users SET registration_status = 'APPROVED';
