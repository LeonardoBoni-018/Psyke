CREATE TABLE IF NOT EXISTS prontuarios (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id         UUID NOT NULL,
    professional_id    UUID NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    allergies          TEXT,
    chronic_conditions TEXT,
    medications        TEXT,
    notes              TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS evolucoes_clinicas (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prontuario_id    UUID NOT NULL REFERENCES prontuarios(id) ON DELETE CASCADE,
    appointment_id   UUID NOT NULL,
    professional_id  UUID NOT NULL,
    subjective       TEXT,
    objective        TEXT,
    assessment       TEXT,
    plan             TEXT,
    techniques       VARCHAR(500),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS anamneses (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prontuario_id    UUID NOT NULL REFERENCES prontuarios(id) ON DELETE CASCADE,
    professional_id  UUID NOT NULL,
    chief_complaint  TEXT,
    history_illness  TEXT,
    personal_history TEXT,
    family_history   TEXT,
    social_history   TEXT,
    medications      TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_prontuarios_patient ON prontuarios(patient_id);
CREATE INDEX IF NOT EXISTS idx_prontuarios_professional ON prontuarios(professional_id);
CREATE INDEX IF NOT EXISTS idx_evolucoes_prontuario ON evolucoes_clinicas(prontuario_id);
CREATE INDEX IF NOT EXISTS idx_evolucoes_appointment ON evolucoes_clinicas(appointment_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_evolucoes_unique_appointment ON evolucoes_clinicas(appointment_id);
CREATE INDEX IF NOT EXISTS idx_anamneses_prontuario ON anamneses(prontuario_id);
