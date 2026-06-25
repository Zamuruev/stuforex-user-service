CREATE TABLE IF NOT EXISTS donations (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount          DECIMAL(10,2) NOT NULL,
    method          VARCHAR(50)   NOT NULL DEFAULT 'MOCK',
    status          VARCHAR(50)   NOT NULL DEFAULT 'PAID',
    message         TEXT,
    payment_provider VARCHAR(50)  NOT NULL DEFAULT 'MOCK',
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
