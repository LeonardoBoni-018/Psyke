-- V13: respostas a questionários

CREATE TABLE IF NOT EXISTS questionnaire_assignments (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id        UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    questionnaire_id UUID NOT NULL REFERENCES questionnaires(id) ON DELETE CASCADE,
    patient_id       UUID NOT NULL,
    professional_id  UUID,
    status           VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    answers          JSONB DEFAULT NULL,
    score            DECIMAL(10,2),
    assigned_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_qassignments_clinic ON questionnaire_assignments(clinic_id);
CREATE INDEX IF NOT EXISTS idx_qassignments_patient ON questionnaire_assignments(patient_id);
CREATE INDEX IF NOT EXISTS idx_qassignments_qid ON questionnaire_assignments(questionnaire_id);
