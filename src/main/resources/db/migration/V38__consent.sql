ALTER TABLE users
    ADD COLUMN IF NOT EXISTS consent_given_at   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS consent_version    VARCHAR(10),
    ADD COLUMN IF NOT EXISTS cookie_consent     VARCHAR(10) DEFAULT 'pending';
