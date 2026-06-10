-- Поле для защиты от повторного начисления DAILY_LOGIN XP
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_daily_login DATE;
