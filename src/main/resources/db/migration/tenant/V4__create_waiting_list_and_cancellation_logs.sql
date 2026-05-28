CREATE TABLE IF NOT EXISTS waiting_list (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id       UUID NOT NULL,
    professional_id  UUID NOT NULL,
    priority         INTEGER NOT NULL DEFAULT 0,
    requested_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notified_at      TIMESTAMPTZ,
    offered_at       TIMESTAMPTZ,
    offered_until    TIMESTAMPTZ,
    slot_start_time  TIMESTAMPTZ,
    slot_end_time    TIMESTAMPTZ,
    status           VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cancellation_logs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id    UUID NOT NULL,
    cancelled_by      UUID,
    cancelled_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reason            VARCHAR(500),
    cancelled_by_role VARCHAR(30),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE rooms ADD COLUMN IF NOT EXISTS capacity INTEGER NOT NULL DEFAULT 1;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS type VARCHAR(30) NOT NULL DEFAULT 'PHYSICAL';

CREATE INDEX IF NOT EXISTS idx_waiting_list_professional ON waiting_list(professional_id, status);
CREATE INDEX IF NOT EXISTS idx_waiting_list_patient ON waiting_list(patient_id);
CREATE INDEX IF NOT EXISTS idx_cancellation_logs_appointment ON cancellation_logs(appointment_id);
