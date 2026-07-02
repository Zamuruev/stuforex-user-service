CREATE TABLE IF NOT EXISTS pinned_chats (
    user_id  UUID         NOT NULL,
    room_id  VARCHAR(36)  NOT NULL,
    position INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, room_id)
);
CREATE INDEX IF NOT EXISTS idx_pinned_chats_user ON pinned_chats(user_id);
