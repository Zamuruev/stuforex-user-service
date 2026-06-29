-- anti_plagiat_verified: null означает «на рассмотрении», но колонка была NOT NULL.
-- Снимаем ограничение, чтобы новые пользователи создавались без ошибки.
ALTER TABLE users ALTER COLUMN anti_plagiat_verified DROP NOT NULL;
