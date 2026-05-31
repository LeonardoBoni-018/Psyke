-- V8: tabela de planos terapêuticos

CREATE TABLE IF NOT EXISTS treatment_plans (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id          UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    prontuario_id      UUID NOT NULL REFERENCES prontuarios(id) ON DELETE CASCADE,
    title              VARCHAR(200) NOT NULL,
    description        TEXT,
    goals              TEXT,
    techniques         TEXT,
    frequency          VARCHAR(100),
    estimated_sessions INTEGER,
    status             VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    start_date         DATE,
    end_date           DATE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_treatment_plans_clinic ON treatment_plans(clinic_id);
CREATE INDEX IF NOT EXISTS idx_treatment_plans_prontuario ON treatment_plans(prontuario_id);
