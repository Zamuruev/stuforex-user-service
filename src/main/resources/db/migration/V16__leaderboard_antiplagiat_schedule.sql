-- Антиплагиат
ALTER TABLE users ADD COLUMN IF NOT EXISTS anti_plagiat_verified     BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS anti_plagiat_verified_at  TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS anti_plagiat_requested_at TIMESTAMP;

-- Расписание исполнителя
CREATE TABLE IF NOT EXISTS executor_schedule (
                                                 id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,
    day_of_week  SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7), -- 1=Пн 7=Вс
    time_from    TIME NOT NULL,
    time_to      TIME NOT NULL,
    UNIQUE (user_id, day_of_week)
    );
CREATE INDEX IF NOT EXISTS idx_executor_schedule_user ON executor_schedule(user_id);

-- Максимальное количество одновременных заданий
ALTER TABLE users ADD COLUMN IF NOT EXISTS max_concurrent_tasks SMALLINT DEFAULT 3;

