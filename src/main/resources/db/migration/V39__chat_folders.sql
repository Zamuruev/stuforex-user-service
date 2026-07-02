CREATE TABLE IF NOT EXISTS chat_folders (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID         NOT NULL,
    name       VARCHAR(100) NOT NULL,
    emoji      VARCHAR(10),
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS chat_folder_rooms (
    folder_id UUID        NOT NULL REFERENCES chat_folders(id) ON DELETE CASCADE,
    room_id   VARCHAR(36) NOT NULL,
    PRIMARY KEY (folder_id, room_id)
);
