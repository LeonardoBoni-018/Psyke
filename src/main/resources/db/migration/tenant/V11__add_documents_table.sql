-- V11: documentos e anexos

CREATE TABLE IF NOT EXISTS documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    patient_id      UUID,
    appointment_id  UUID,
    professional_id UUID,
    original_name   VARCHAR(500) NOT NULL,
    storage_path    VARCHAR(1000) NOT NULL,
    file_type       VARCHAR(100),
    file_size       BIGINT NOT NULL DEFAULT 0,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_documents_clinic ON documents(clinic_id);
CREATE INDEX IF NOT EXISTS idx_documents_patient ON documents(patient_id);
CREATE INDEX IF NOT EXISTS idx_documents_appointment ON documents(appointment_id);
