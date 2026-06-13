CREATE TABLE IF NOT EXISTS notifications (
                                             id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    type        VARCHAR(60) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    body        TEXT,
    is_read     BOOLEAN NOT NULL DEFAULT false,
    link        VARCHAR(500),
    meta        JSONB,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_unread  ON notifications(user_id, is_read) WHERE is_read = false;

-- Автоудаление уведомлений старше 30 дней (запускать через pg_cron или scheduler)
-- DELETE FROM notifications WHERE created_at < now() - INTERVAL '30 days';
