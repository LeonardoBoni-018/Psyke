CREATE TABLE IF NOT EXISTS appointments (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id        UUID NOT NULL,
    patient_id       UUID NOT NULL,
    professional_id  UUID NOT NULL,
    room_id          UUID,
    start_time       TIMESTAMPTZ NOT NULL,
    end_time         TIMESTAMPTZ NOT NULL,
    status           VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    prontuario       TEXT,
    notes            VARCHAR(500),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS recurrence_rules (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id   UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    freq             VARCHAR(20) NOT NULL DEFAULT 'WEEKLY',
    interval_val     INTEGER NOT NULL DEFAULT 1,
    count            INTEGER,
    until_date       DATE,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS confirmation_tokens (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token            UUID NOT NULL UNIQUE,
    appointment_id   UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    type             VARCHAR(20) NOT NULL,
    expires_at       TIMESTAMPTZ NOT NULL,
    used_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_appointments_professional_time ON appointments(professional_id, start_time);
CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_room_time ON appointments(room_id, start_time);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status);
CREATE INDEX IF NOT EXISTS idx_confirmation_tokens_token ON confirmation_tokens(token);
