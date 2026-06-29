CREATE TABLE IF NOT EXISTS support_messages (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    UUID         REFERENCES users(id) ON DELETE CASCADE,
    role       VARCHAR(20)  NOT NULL,
    content    TEXT         NOT NULL,
    escalated  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_support_messages_user ON support_messages(user_id, created_at);
