-- V9: configurações de notificação por usuário

CREATE TABLE IF NOT EXISTS notification_configs (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID NOT NULL,
    email_enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled           BOOLEAN NOT NULL DEFAULT FALSE,
    push_enabled          BOOLEAN NOT NULL DEFAULT FALSE,
    remind_before_minutes INTEGER NOT NULL DEFAULT 1440,
    notify_cancellations  BOOLEAN NOT NULL DEFAULT TRUE,
    notify_confirmations  BOOLEAN NOT NULL DEFAULT TRUE,
    notify_changes        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_notif_configs_user ON notification_configs(user_id);
