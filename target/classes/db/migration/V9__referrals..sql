ALTER TABLE users ADD COLUMN IF NOT EXISTS ref_code VARCHAR(16) UNIQUE;

CREATE TABLE IF NOT EXISTS referrals (
                                         id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inviter_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invitee_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_active   BOOLEAN NOT NULL DEFAULT false,
    reward_days INT,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (invitee_id)
    );

CREATE INDEX IF NOT EXISTS idx_referrals_inviter ON referrals(inviter_id);
UPDATE users u
SET ref_code = t.code
    FROM (
    SELECT id,
           LOWER(LEFT(MD5(id::text), 8)) AS code
    FROM users
    WHERE ref_code IS NULL
) t
WHERE u.id = t.id
  AND NOT EXISTS (
    SELECT 1 FROM users u2 WHERE u2.ref_code = LOWER(LEFT(MD5(u.id::text), 8))
    );
