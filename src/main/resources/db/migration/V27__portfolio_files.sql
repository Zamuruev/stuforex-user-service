CREATE TABLE IF NOT EXISTS portfolio_files (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_url    TEXT        NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    file_type   VARCHAR(100),
    description TEXT,
    category    VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_portfolio_files_user_id ON portfolio_files(user_id);
