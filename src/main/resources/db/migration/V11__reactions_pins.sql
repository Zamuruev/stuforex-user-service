-- Реакции на сообщения
CREATE TABLE IF NOT EXISTS message_reactions (
                                                 id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id  UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    emoji       VARCHAR(10) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (message_id, user_id, emoji)
    );
CREATE INDEX IF NOT EXISTS idx_reactions_message ON message_reactions(message_id);

-- Закреплённые сообщения
ALTER TABLE messages ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS pinned_at TIMESTAMP;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS pinned_by UUID;
