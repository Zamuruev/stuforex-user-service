CREATE TABLE IF NOT EXISTS task_favorites (
    id         UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID      NOT NULL,
    task_id    UUID      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, task_id)
);

CREATE INDEX IF NOT EXISTS idx_favorites_user ON task_favorites(user_id);
CREATE INDEX IF NOT EXISTS idx_favorites_task ON task_favorites(task_id);
