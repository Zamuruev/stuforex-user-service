-- Миграция: поля для soft-delete и активности аккаунта
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_users_is_deleted ON users(is_deleted);
