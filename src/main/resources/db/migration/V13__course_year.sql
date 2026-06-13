-- Курс студента в профиле
ALTER TABLE users ADD COLUMN IF NOT EXISTS course_year SMALLINT CHECK (course_year BETWEEN 1 AND 6);

-- Курс в задании
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS course_year SMALLINT CHECK (course_year BETWEEN 1 AND 6);
