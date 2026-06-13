CREATE TABLE IF NOT EXISTS task_folders (
                                            id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL,
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(20) NOT NULL DEFAULT '#6366f1',
    created_at TIMESTAMP NOT NULL DEFAULT now()
    );
CREATE INDEX IF NOT EXISTS idx_task_folders_user ON task_folders(user_id);

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS folder_id UUID REFERENCES task_folders(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_folder ON tasks(folder_id);

-- Лимит трат студента (в отдельной миграции можно выделить, здесь добавляем к V6)
ALTER TABLE users ADD COLUMN IF NOT EXISTS monthly_budget_limit INTEGER;
