-- Add university field to tasks
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS university VARCHAR(255);

-- Performance indexes
CREATE INDEX IF NOT EXISTS idx_tasks_status           ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_category_active  ON tasks(category_id, status);
CREATE INDEX IF NOT EXISTS idx_tasks_customer         ON tasks(customer_id);
CREATE INDEX IF NOT EXISTS idx_tasks_executor         ON tasks(executor_id);
CREATE INDEX IF NOT EXISTS idx_tasks_course_year      ON tasks(course_year) WHERE course_year IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_boosted          ON tasks(boosted_until) WHERE boosted_until IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_proposals_task_id      ON proposals(task_id);
CREATE INDEX IF NOT EXISTS idx_proposals_executor_id  ON proposals(executor_id);
