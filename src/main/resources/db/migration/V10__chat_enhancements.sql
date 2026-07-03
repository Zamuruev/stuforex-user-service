-- Cross-service tables: chat-service
CREATE TABLE IF NOT EXISTS chat_rooms (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id     UUID,
    customer_id UUID,
    executor_id UUID,
    created_at  TIMESTAMP DEFAULT NOW(),
    title       VARCHAR(100),
    task_status VARCHAR(50) DEFAULT 'ACTIVE',
    updated_at  TIMESTAMP,
    admin_id    UUID
);

CREATE TABLE IF NOT EXISTS messages (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id      UUID,
    sender_id    UUID,
    content      TEXT NOT NULL,
    message_type VARCHAR(50) DEFAULT 'TEXT',
    is_read      BOOLEAN DEFAULT FALSE,
    created_at   TIMESTAMP DEFAULT NOW(),
    reply_to_id  UUID,
    is_edited    BOOLEAN DEFAULT FALSE,
    edited_at    TIMESTAMP
);

-- Название чата (кастомное, по умолчанию NULL — покажем название задания)
ALTER TABLE chat_rooms ADD COLUMN IF NOT EXISTS title VARCHAR(100);

-- Поля для файлов в сообщениях
ALTER TABLE messages ADD COLUMN IF NOT EXISTS file_url    TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS file_name   VARCHAR(255);
ALTER TABLE messages ADD COLUMN IF NOT EXISTS file_duration INTEGER;

-- Стикеры (хранятся в БД, видны всем участникам чата)
CREATE TABLE IF NOT EXISTS stickers (
                                        id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id     UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    created_by  UUID NOT NULL,
    content     TEXT NOT NULL DEFAULT '',
    color       VARCHAR(20) NOT NULL DEFAULT '#fef08a',
    pos_x       FLOAT NOT NULL DEFAULT 50,
    pos_y       FLOAT NOT NULL DEFAULT 50,
    is_pinned   BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_stickers_room ON stickers(room_id);
