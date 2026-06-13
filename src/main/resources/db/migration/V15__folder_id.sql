-- Добавить folder_id в tasks
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS folder_id UUID REFERENCES task_folders(id) ON DELETE SET NULL;

-- Добавить course_year в tasks
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS course_year SMALLINT CHECK (course_year BETWEEN 1 AND 6);
