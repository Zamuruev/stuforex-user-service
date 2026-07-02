-- Добавляем context_id для хранения закреплённых отдельно по табу/папке
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pinned_chats' AND column_name = 'context_id'
    ) THEN
        ALTER TABLE pinned_chats DROP CONSTRAINT IF EXISTS pinned_chats_pkey;
        ALTER TABLE pinned_chats ADD COLUMN context_id VARCHAR(36) NOT NULL DEFAULT 'all';
        ALTER TABLE pinned_chats ADD PRIMARY KEY (user_id, context_id, room_id);
        DROP INDEX IF EXISTS idx_pinned_chats_user;
        CREATE INDEX idx_pinned_chats_user_ctx ON pinned_chats(user_id, context_id);
    END IF;
END $$;
