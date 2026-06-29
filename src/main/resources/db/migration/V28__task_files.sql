CREATE TABLE IF NOT EXISTS task_files (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id     UUID NOT NULL,
    uploader_id UUID NOT NULL,
    file_url    TEXT NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    file_type   VARCHAR(100),
    file_size   BIGINT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_task_files_task_id ON task_files(task_id);
