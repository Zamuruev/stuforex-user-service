CREATE TABLE IF NOT EXISTS chat_room_hidden_users (
    room_id UUID NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    PRIMARY KEY (room_id, user_id)
);
