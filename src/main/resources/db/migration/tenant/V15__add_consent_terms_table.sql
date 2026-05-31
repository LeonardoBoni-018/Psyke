-- V15: termos de consentimento informado

CREATE TABLE IF NOT EXISTS consent_terms (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id             UUID NOT NULL REFERENCES clinics(id) ON DELETE CASCADE,
    patient_id            UUID NOT NULL,
    professional_id       UUID NOT NULL,
    title                 VARCHAR(300) NOT NULL,
    content               TEXT NOT NULL,
    signed_by_patient     BOOLEAN NOT NULL DEFAULT FALSE,
    signed_by_professional BOOLEAN NOT NULL DEFAULT FALSE,
    signed_at             TIMESTAMPTZ,
    expires_at            TIMESTAMPTZ,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_consent_terms_clinic ON consent_terms(clinic_id);
CREATE INDEX IF NOT EXISTS idx_consent_terms_patient ON consent_terms(patient_id);
